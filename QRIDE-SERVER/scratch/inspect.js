const fs = require('fs');

const content = fs.readFileSync('Dump20260617.sql', 'utf8');
const start = content.indexOf('INSERT INTO `payments` VALUES');
if (start === -1) {
    console.log('Not found');
    process.exit(1);
}
const end = content.indexOf(';', start);
const block = content.substring(start, end);

const rows = block.substring('INSERT INTO `payments` VALUES '.length).split(/\),\s*\(/);
rows.forEach((row, idx) => {
    let cleanRow = row;
    if (!cleanRow.startsWith('(')) cleanRow = '(' + cleanRow;
    if (!cleanRow.endsWith(')')) cleanRow = cleanRow + ')';
    
    // Parse columns by matching commas not inside single quotes
    const cols = cleanRow.match(/('[^']*'|[^,]+)/g);
    if (cols.length !== 14) {
        console.log(`Row ${idx + 1} (ID: ${cleanRow.split(',')[0].replace('(', '').trim()}): ${cols.length} columns! Raw: ${row}`);
    }
});
console.log('Check finished.');
