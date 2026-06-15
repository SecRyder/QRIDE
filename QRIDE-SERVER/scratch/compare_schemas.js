const fs = require('fs');
const path = require('path');

const dumpPath = path.join(__dirname, '..', 'Dump20260520.sql');
const qridePath = path.join(__dirname, '..', 'qride_utf8.sql');

function parseMySQLSchema(filePath) {
    const content = fs.readFileSync(filePath, 'utf8');
    const tableSchemas = {};
    
    // Simple regex to parse table definitions
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

function parseMSSQLSchema(filePath) {
    const content = fs.readFileSync(filePath, 'utf8');
    const tableSchemas = {};
    
    const tableRegex = /CREATE TABLE \[dbo\]\.\[([^\]]+)\]\(\s*([\s\S]*?)\n\s*PRIMARY KEY/g;
    let match;
    while ((match = tableRegex.exec(content)) !== null) {
        const tableName = match[1];
        const columnsStr = match[2];
        const columns = [];
        const colLines = columnsStr.split('\n');
        for (let line of colLines) {
            line = line.trim();
            if (line.startsWith('[')) {
                const colMatch = line.match(/^\[([^\]]+)\]/);
                if (colMatch) {
                    columns.push(colMatch[1]);
                }
            }
        }
        tableSchemas[tableName] = columns;
    }
    return tableSchemas;
}

const mysqlSchema = parseMySQLSchema(dumpPath);
const mssqlSchema = parseMSSQLSchema(qridePath);

console.log('--- SCHEMA COMPARISON ---');
for (let table in mysqlSchema) {
    if (mssqlSchema[table]) {
        const myCols = mysqlSchema[table];
        const msCols = mssqlSchema[table];
        const missingInMs = myCols.filter(c => !msCols.includes(c));
        const missingInMy = msCols.filter(c => !myCols.includes(c));
        
        if (missingInMs.length > 0 || missingInMy.length > 0) {
            console.log(`Table ${table}:`);
            if (missingInMs.length > 0) console.log(`  Columns in MySQL but NOT in MSSQL: ${missingInMs.join(', ')}`);
            if (missingInMy.length > 0) console.log(`  Columns in MSSQL but NOT in MySQL: ${missingInMy.join(', ')}`);
        }
    } else {
        console.log(`Table ${table} exists in MySQL dump but NOT in MSSQL dump.`);
    }
}
