const fs = require('fs');
const path = require('path');

const dumpPath = path.join(__dirname, '..', 'Dump20260520.sql');
const qridePath = path.join(__dirname, '..', 'qride_utf8.sql');

function detectEncoding(filePath) {
    const buffer = fs.readFileSync(filePath);
    if (buffer[0] === 0xFF && buffer[1] === 0xFE) {
        return 'utf16le';
    }
    if (buffer[0] === 0xFE && buffer[1] === 0xFF) {
        return 'utf16be'; // unusual on Windows
    }
    return 'utf8';
}

function parseMySQLDump(filePath) {
    const encoding = detectEncoding(filePath);
    console.log(`Reading MySQL Dump: ${filePath} (${encoding})`);
    const content = fs.readFileSync(filePath, encoding);
    const lines = content.split(/\r?\n/);
    
    const tableData = {}; // tableName -> Array of rows (objects or arrays)
    
    // We want to capture inserts like: INSERT INTO `tableName` VALUES (row1), (row2), ...
    // Note: mysqldump puts each table's insert on a single line usually.
    const insertRegex = /INSERT INTO `([^`]+)` VALUES\s+(.*);/i;
    
    for (let line of lines) {
        const match = line.match(insertRegex);
        if (match) {
            const tableName = match[1];
            const valuesStr = match[2];
            
            // Parse valuesStr: (val1, val2, ...), (val3, val4, ...)
            const rows = parseValuesString(valuesStr);
            if (!tableData[tableName]) {
                tableData[tableName] = [];
            }
            tableData[tableName].push(...rows);
        }
    }
    return tableData;
}

// A simple but robust value string parser to split rows and fields
function parseValuesString(str) {
    const rows = [];
    let i = 0;
    while (i < str.length) {
        // Find start of row
        while (i < str.length && str[i] !== '(') i++;
        if (i >= str.length) break;
        i++; // skip '('
        
        const row = [];
        let currentVal = '';
        let inString = false;
        let stringChar = null;
        let escaped = false;
        
        while (i < str.length) {
            const char = str[i];
            if (escaped) {
                currentVal += char;
                escaped = false;
                i++;
                continue;
            }
            
            if (char === '\\') {
                escaped = true;
                currentVal += char;
                i++;
                continue;
            }
            
            if (inString) {
                if (char === stringChar) {
                    inString = false;
                } else {
                    currentVal += char;
                }
                i++;
                continue;
            }
            
            if (char === "'" || char === '"') {
                inString = true;
                stringChar = char;
                i++;
                continue;
            }
            
            if (char === ',') {
                row.push(currentVal.trim());
                currentVal = '';
                i++;
                continue;
            }
            
            if (char === ')') {
                row.push(currentVal.trim());
                rows.push(row);
                i++;
                break;
            }
            
            currentVal += char;
            i++;
        }
    }
    return rows;
}

function parseMSSQLDump(filePath) {
    const encoding = detectEncoding(filePath);
    console.log(`Reading MSSQL Dump: ${filePath} (${encoding})`);
    const content = fs.readFileSync(filePath, encoding);
    const lines = content.split(/\r?\n/);
    
    const tableData = {}; // tableName -> Array of rows (by primary key or all values)
    
    // Format: INSERT [dbo].[tableName] ([col1], ...) VALUES (val1, ...)
    // Or sometimes just values. Let's support both.
    const insertRegex = /INSERT\s+\[dbo\]\.\[([^\]]+)\]\s*(?:\(([^)]+)\))?\s*VALUES\s*\((.*)\)/i;
    
    for (let line of lines) {
        const match = line.match(insertRegex);
        if (match) {
            const tableName = match[1];
            const colsStr = match[2];
            const valsStr = match[3];
            
            const cols = colsStr ? colsStr.split(',').map(c => c.replace(/[\[\]\s]/g, '')) : [];
            const vals = parseMSSQLValuesRow(valsStr);
            
            if (!tableData[tableName]) {
                tableData[tableName] = [];
            }
            tableData[tableName].push({ cols, vals });
        }
    }
    return tableData;
}

function parseMSSQLValuesRow(str) {
    const row = [];
    let currentVal = '';
    let inString = false;
    let escaped = false;
    let i = 0;
    
    while (i < str.length) {
        const char = str[i];
        
        // Handle N'string'
        if (char === 'N' && str[i+1] === "'") {
            inString = true;
            i += 2;
            continue;
        }
        
        if (inString) {
            // MSSQL escapes single quote inside single quote by doubling it: ''
            if (char === "'") {
                if (str[i+1] === "'") {
                    currentVal += "'";
                    i += 2;
                } else {
                    inString = false;
                    i++;
                }
            } else {
                currentVal += char;
                i++;
            }
            continue;
        }
        
        if (char === "'") {
            inString = true;
            i++;
            continue;
        }
        
        // Handle functions like CAST(N'...' AS DateTime)
        if (char === 'C' && str.substring(i, i+5).toUpperCase() === 'CAST(') {
            // Find matching parenthesis
            let parenCount = 1;
            let j = i + 5;
            let funcVal = 'CAST(';
            while (j < str.length && parenCount > 0) {
                funcVal += str[j];
                if (str[j] === '(') parenCount++;
                else if (str[j] === ')') parenCount--;
                j++;
            }
            currentVal = funcVal;
            i = j;
            continue;
        }
        
        if (char === ',') {
            row.push(currentVal.trim());
            currentVal = '';
            i++;
            continue;
        }
        
        currentVal += char;
        i++;
    }
    row.push(currentVal.trim());
    return row;
}

function analyze() {
    const mySqlData = parseMySQLDump(dumpPath);
    const msSqlData = parseMSSQLDump(qridePath);
    
    console.log('\n--- ANALYSIS RESULTS ---');
    
    const allTables = new Set([...Object.keys(mySqlData), ...Object.keys(msSqlData)]);
    
    for (let table of allTables) {
        const myCount = mySqlData[table] ? mySqlData[table].length : 0;
        const msRows = msSqlData[table] || [];
        const msCount = msRows.length;
        
        console.log(`Table: ${table}`);
        console.log(`  MySQL Dump count: ${myCount}`);
        console.log(`  MSSQL Dump count: ${msCount}`);
        
        if (myCount > 0 && msCount > 0) {
            // Compare first column (usually ID) to find what is missing in MSSQL
            const myIds = mySqlData[table].map(row => row[0]);
            const msIds = msRows.map(row => {
                // If there's columns, find 'id'
                const idIdx = row.cols.indexOf('id');
                return idIdx !== -1 ? row.vals[idIdx] : row.vals[0];
            });
            
            const missingInMsSql = myIds.filter(id => !msIds.includes(id));
            const missingInMySql = msIds.filter(id => !myIds.includes(id));
            
            if (missingInMsSql.length > 0) {
                console.log(`  IDs missing in MSSQL: ${missingInMsSql.join(', ')}`);
            }
            if (missingInMySql.length > 0) {
                console.log(`  IDs present in MSSQL but missing in MySQL Dump: ${missingInMySql.join(', ')}`);
            }
        }
        console.log('');
    }
}

analyze();
