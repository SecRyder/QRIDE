const fs = require('fs');
const path = require('path');

const dumpPath = path.join(__dirname, '..', 'Dump20260520.sql');

function detectEncoding(filePath) {
    const buffer = fs.readFileSync(filePath);
    if (buffer[0] === 0xFF && buffer[1] === 0xFE) {
        return 'utf16le';
    }
    return 'utf8';
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
            if (!tableData[tableName]) {
                tableData[tableName] = [];
            }
            tableData[tableName].push(...rows);
        }
    }
    return tableData;
}

function parseValuesString(str) {
    const rows = [];
    let i = 0;
    while (i < str.length) {
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

const mySqlData = parseMySQLDump(dumpPath);

// Print details of missing rows
console.log('--- MISSING USERS IN MYSQL ---');
const users = mySqlData['users'] || [];
for (let row of users) {
    const id = row[0];
    if (['13', '14', '15', '16', '17', '19', '20'].includes(id)) {
        // truncate avatar base64
        const rowCopy = [...row];
        if (rowCopy[10] && rowCopy[10].length > 50) {
            rowCopy[10] = rowCopy[10].substring(0, 30) + '... (truncated)';
        }
        console.log(`User ID ${id}:`, rowCopy);
    }
}

console.log('\n--- MISSING PAYMENTS IN MYSQL ---');
const payments = mySqlData['payments'] || [];
for (let row of payments) {
    const id = row[0];
    if (['17', '18', '19', '20', '21', '22', '23', '24', '25', '26', '27', '28'].includes(id)) {
        console.log(`Payment ID ${id}:`, row);
    }
}

console.log('\n--- MISSING VEHICLES IN MYSQL ---');
const vehicles = mySqlData['vehicle'] || [];
for (let row of vehicles) {
    const id = row[0];
    if (parseInt(id) >= 16) {
        console.log(`Vehicle ID ${id}:`, row);
    }
}

console.log('\n--- MISSING VOUCHERS IN MYSQL ---');
const vouchers = mySqlData['vouchers'] || [];
for (let row of vouchers) {
    console.log(`Voucher:`, row);
}

console.log('\n--- WALLETS IN MYSQL ---');
const wallets = mySqlData['wallet'] || [];
for (let row of wallets) {
    console.log(`Wallet:`, row);
}
