const fs = require('fs');
const path = require('path');
const sql = require('mssql');
require('dotenv').config();

const dumpPath = path.join(__dirname, '..', 'Dump20260520.sql');
const qridePath = path.join(__dirname, '..', 'qride.sql');
const targetPath = path.join(__dirname, '..', 'qride_utf8.sql');

const dbConfig = {
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    server: process.env.DB_SERVER,
    database: process.env.DB_NAME,
    options: { encrypt: false, trustServerCertificate: true }
};

function detectEncoding(filePath) {
    const buffer = fs.readFileSync(filePath);
    if (buffer[0] === 0xFF && buffer[1] === 0xFE) return 'utf16le';
    return 'utf8';
}

function parseValuesString(str) {
    const rows = [];
    let i = 0;
    while (i < str.length) {
        while (i < str.length && str[i] !== '(') i++;
        if (i >= str.length) break;
        i++; 
        const row = [];
        let currentVal = '';
        let inString = false;
        let stringChar = null;
        let escaped = false;
        while (i < str.length) {
            const char = str[i];
            if (escaped) { currentVal += char; escaped = false; i++; continue; }
            if (char === '\\') { escaped = true; currentVal += char; i++; continue; }
            if (inString) {
                if (char === stringChar) inString = false;
                else currentVal += char;
                i++;
                continue;
            }
            if (char === "'" || char === '"') { inString = true; stringChar = char; i++; continue; }
            if (char === ',') { row.push(currentVal.trim()); currentVal = ''; i++; continue; }
            if (char === ')') { row.push(currentVal.trim()); rows.push(row); i++; break; }
            currentVal += char;
            i++;
        }
    }
    return rows;
}

function parseMySQLSchema(filePath) {
    const encoding = detectEncoding(filePath);
    const content = fs.readFileSync(filePath, encoding);
    const tableSchemas = {};
    const tableRegex = /CREATE TABLE `([^`]+)` \(([\s\S]*?)\) ENGINE/g;
    let match;
    while ((match = tableRegex.exec(content)) !== null) {
        const tableName = match[1];
        const columnsStr = match[2];
        const columns = [];
        const colLines = columnsStr.split('\n');
        for (let line of colLines) {
            line = line.trim();
            if (line.startsWith('`')) {
                const colName = line.match(/^`([^`]+)`/)[1];
                columns.push(colName);
            }
        }
        tableSchemas[tableName] = columns;
    }
    return tableSchemas;
}

function parseMySQLDump(filePath) {
    const encoding = detectEncoding(filePath);
    const content = fs.readFileSync(filePath, encoding);
    const lines = content.split(/\r?\n/);
    const tableData = {};
    const insertRegex = /INSERT INTO `([^`]+)` VALUES\s+(.*);/i;
    for (let line of lines) {
        const match = line.match(insertRegex);
        if (match) {
            const tableName = match[1];
            const valuesStr = match[2];
            const rows = parseValuesString(valuesStr);
            if (!tableData[tableName]) tableData[tableName] = [];
            tableData[tableName].push(...rows);
        }
    }
    return tableData;
}

function toMssqlLiteral(val) {
    if (val === 'NULL' || val === null || val === undefined) return 'NULL';
    if (val.match(/^b'([01])'/)) return val.replace(/^b'([01])'/, '$1'); // boolean
    // Date/Time hack: MySQL dates look like 2026-05-06 12:11:49, we convert to string literals properly
    if (val.match(/^\d{4}-\d{2}-\d{2}/)) {
        return `CAST(N'${val.replace(' ', 'T')}' AS DateTime)`;
    }
    // String types - escape single quotes
    return `N'${val.replace(/'/g, "''")}'`;
}

async function generateCleanDump() {
    const mysqlSchema = parseMySQLSchema(dumpPath);
    const mysqlData = parseMySQLDump(dumpPath);

    console.log('Reading qride.sql to strip existing INSERTs...');
    const qrideEncoding = detectEncoding(qridePath);
    let qrideContent = fs.readFileSync(qridePath, qrideEncoding);
    
    // Strip all lines that start with INSERT [dbo].[tableName]
    // And also SET IDENTITY_INSERT ... ON / OFF
    const lines = qrideContent.split(/\r?\n/);
    const cleanLines = [];
    
    // Add DROP DATABASE to ensure clean recreation
    cleanLines.push('USE [master]');
    cleanLines.push('GO');
    cleanLines.push(`IF EXISTS (SELECT name FROM sys.databases WHERE name = N'QRIDE')`);
    cleanLines.push('BEGIN');
    cleanLines.push(`    ALTER DATABASE [QRIDE] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;`);
    cleanLines.push(`    DROP DATABASE [QRIDE];`);
    cleanLines.push('END');
    cleanLines.push('GO');
    
    for (let line of lines) {
        if (line.trim().startsWith('INSERT [dbo].')) continue;
        if (line.trim().startsWith('SET IDENTITY_INSERT')) continue;
        cleanLines.push(line);
    }
    
    // Construct MSSQL INSERTS
    let newInserts = '\n\n-- DATA SYNCHRONIZED FROM MYSQL DUMP --\n';
    
    for (let tableName in mysqlData) {
        const rows = mysqlData[tableName];
        if (!rows || rows.length === 0) continue;
        
        console.log(`Processing table: ${tableName} (${rows.length} rows)`);
        
        let cols = mysqlSchema[tableName];
        if (tableName === 'vehicle') {
            cols = cols.filter(c => c !== 'type'); // omit 'type'
        }
        
        newInserts += `\nSET IDENTITY_INSERT [dbo].[${tableName}] ON;\nGO\n`;
        
        for (let row of rows) {
            let rowVals = [...row];
            if (tableName === 'vehicle') {
                const typeIdx = mysqlSchema['vehicle'].indexOf('type');
                if (typeIdx > -1) rowVals.splice(typeIdx, 1);
            }
            if (tableName === 'users') {
                // CCCD hack
                const id = rowVals[0];
                let cccdIdx = cols.indexOf('cccd');
                if (cccdIdx > -1) {
                    if (['15', '16', '17', '19', '20'].includes(id) && rowVals[cccdIdx] === '066204007703') {
                        rowVals[cccdIdx] = `0662040077${id}`;
                    }
                }
            }
            
            const msVals = rowVals.map(toMssqlLiteral);
            const colsStr = cols.map(c => `[${c}]`).join(', ');
            const insertStmt = `INSERT [dbo].[${tableName}] (${colsStr}) VALUES (${msVals.join(', ')});`;
            newInserts += insertStmt + '\n';
        }
        newInserts += `SET IDENTITY_INSERT [dbo].[${tableName}] OFF;\nGO\n`;
    }
    
    // Write new content to qride_utf8.sql
    console.log('Writing qride_utf8.sql...');
    const finalContent = cleanLines.join('\n') + newInserts;
    fs.writeFileSync(targetPath, finalContent, 'utf8');
    console.log('qride_utf8.sql generated successfully!');
    
    // Drop and recreate DB tables
    console.log('\\nRecreating database QRIDE from qride_utf8.sql...');
    // We can use sqlcmd to execute the file
    const { exec } = require('child_process');
    const sqlCmd = `sqlcmd -C -S ${process.env.DB_SERVER} -U ${process.env.DB_USER} -P ${process.env.DB_PASSWORD} -i "${targetPath}"`;
    
    exec(sqlCmd, (error, stdout, stderr) => {
        if (error) {
            console.error('Error executing sqlcmd:', error);
            console.error('stderr:', stderr);
            return;
        }
        console.log('Database recreation complete!');
    });
}

generateCleanDump().catch(console.error);
