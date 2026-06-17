const fs = require('fs');

function parseSqlRow(rowStr) {
    // strip outer parentheses
    let str = rowStr.trim();
    if (str.startsWith('(')) str = str.substring(1);
    if (str.endsWith(')') || str.endsWith('),') || str.endsWith(');')) {
        str = str.replace(/\),?|;?$/, '');
    }
    
    const values = [];
    let current = '';
    let inQuotes = false;
    let quoteChar = '';
    let escaped = false;
    
    for (let i = 0; i < str.length; i++) {
        const char = str[i];
        if (escaped) {
            current += char;
            escaped = false;
            continue;
        }
        if (char === '\\') {
            current += char;
            escaped = true;
            continue;
        }
        if ((char === "'" || char === '"') && !inQuotes) {
            inQuotes = true;
            quoteChar = char;
            current += char;
            continue;
        }
        if (char === quoteChar && inQuotes) {
            inQuotes = false;
            current += char;
            continue;
        }
        if (char === ',' && !inQuotes) {
            values.push(current.trim());
            current = '';
            continue;
        }
        current += char;
    }
    values.push(current.trim());
    return values;
}

const content = fs.readFileSync('Dump20260617.sql', 'utf8');
const tables = content.split('DROP TABLE IF EXISTS');

tables.slice(1).forEach(t => {
    const nameMatch = t.match(/`(\w+)`/);
    if (!nameMatch) return;
    const tableName = nameMatch[1];
    
    // Find column count in CREATE TABLE
    const createMatch = t.match(/CREATE TABLE `\w+` \(([\s\S]*?)\) ENGINE/);
    if (!createMatch) return;
    const createBody = createMatch[1];
    const columns = createBody.split('\n')
        .map(line => line.trim())
        .filter(line => line && !line.startsWith('PRIMARY KEY') && !line.startsWith('KEY') && !line.startsWith('CONSTRAINT') && !line.startsWith('UNIQUE KEY'));
    const colCount = columns.length;
    
    // Find rows in INSERT
    const insertMatch = t.match(/INSERT INTO `\w+` VALUES\s*([\s\S]*?);/);
    if (!insertMatch) return;
    const rowsContent = insertMatch[1];
    const rows = rowsContent.split(/\),\s*\(/).map(r => {
        let text = r.trim();
        if (!text.startsWith('(')) text = '(' + text;
        if (!text.endsWith(')')) text = text + ')';
        return text;
    });
    
    rows.forEach((row, idx) => {
        const cols = parseSqlRow(row);
        if (cols.length !== colCount) {
            console.log(`Table \`${tableName}\`: Row ${idx + 1} has ${cols.length} values, but table expects ${colCount} columns!`);
            console.log(`Raw row: ${row}`);
            console.log(`Parsed values (${cols.length}):`, cols);
        }
    });
});
console.log('All tables checked.');
