const fs = require('fs');

const dumpFile = 'Dump20260617.sql';
const sqlServerFile = 'qride_utf8.sql';

function parseSqlRow(rowStr) {
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

function parseSqlServerInserts() {
    const content = fs.readFileSync(sqlServerFile, 'utf8');
    const lines = content.split('\n');
    const inserts = {};

    lines.forEach((line, lineNum) => {
        const trimmed = line.trim();
        if (!trimmed.startsWith('INSERT')) return;

        const insertMatch = trimmed.match(/INSERT\s+\[dbo\]\.\[(\w+)\]\s*\((.*?)\)\s*VALUES\s*\((.*?)\);?$/i);
        if (!insertMatch) {
            console.log(`Failed to parse line ${lineNum + 1}: ${trimmed}`);
            return;
        }

        const tableName = insertMatch[1];
        const colsStr = insertMatch[2];
        const valsStr = insertMatch[3];

        const cols = colsStr.split(',').map(c => c.trim().replace(/[\[\]]/g, ''));

        let processedVals = valsStr
            .replace(/CAST\(N'(.*?)'\s+AS\s+DateTime\)/gi, "'$1'")
            .replace(/N'(.*?)'/g, "'$1'");

        if (!inserts[tableName]) {
            inserts[tableName] = [];
        }
        inserts[tableName].push({ cols, valsStr: processedVals });
    });

    return inserts;
}

function parseMySQLDump() {
    const content = fs.readFileSync(dumpFile, 'utf8');
    const tables = content.split(/(-- Table structure for table `\w+`)/g);
    return { header: tables[0], segments: tables.slice(1) };
}

function mergeData() {
    const mssqlData = parseSqlServerInserts();
    const mysqlDump = parseMySQLDump();
    
    const outputSegments = [];
    
    for (let i = 0; i < mysqlDump.segments.length; i += 2) {
        const structHeader = mysqlDump.segments[i]; 
        const structBody = mysqlDump.segments[i+1]; 
        
        if (!structBody) {
            outputSegments.push(structHeader);
            continue;
        }
        
        const tableNameMatch = structHeader.match(/`(\w+)`/);
        if (!tableNameMatch) {
            outputSegments.push(structHeader, structBody);
            continue;
        }
        
        const tableName = tableNameMatch[1];
        
        // Find column count in CREATE TABLE
        const createMatch = structBody.match(/CREATE TABLE `\w+` \(([\s\S]*?)\) ENGINE/);
        let colCount = 0;
        if (createMatch) {
            const createBody = createMatch[1];
            const columns = createBody.split('\n')
                .map(line => line.trim())
                .filter(line => line && !line.startsWith('PRIMARY KEY') && !line.startsWith('KEY') && !line.startsWith('CONSTRAINT') && !line.startsWith('UNIQUE KEY'));
            colCount = columns.length;
        }
        
        const insertMatch = structBody.match(/INSERT INTO `\w+` VALUES\s*([\s\S]*?);/);
        let existingRows = [];
        if (insertMatch) {
            const rowsContent = insertMatch[1];
            existingRows = rowsContent.split(/\), ?\(/).map((r, idx, arr) => {
                let text = r.trim();
                if (idx > 0 && !text.startsWith('(')) text = '(' + text;
                if (idx < arr.length - 1 && !text.endsWith(')')) text = text + ')';
                return text;
            });
        }

        const mssqlRows = mssqlData[tableName] || [];
        
        if (mssqlRows.length > 0) {
            console.log(`Merging ${mssqlRows.length} rows from SQL Server into MySQL table \`${tableName}\` (currently has ${existingRows.length} rows, expects ${colCount} columns)...`);
            
            const existingKeys = new Set();
            existingRows.forEach(row => {
                const match = row.match(/^\s*\(([^,]+)/);
                if (match) {
                    existingKeys.add(match[1].trim());
                }
            });
            
            mssqlRows.forEach(row => {
                let valStr = row.valsStr.trim();
                if (!valStr.startsWith('(')) valStr = '(' + valStr;
                if (!valStr.endsWith(')')) valStr = valStr + ')';
                
                // Parse the row to check columns count
                let parsedCols = parseSqlRow(valStr);
                
                // If column count is less than expected, pad it
                if (colCount > 0 && parsedCols.length < colCount) {
                    const diff = colCount - parsedCols.length;
                    console.log(`  Padding row in \`${tableName}\` with ${diff} values...`);
                    for (let j = 0; j < diff; j++) {
                        // Pad with '0' or 'NULL' based on column names or default
                        parsedCols.push('0');
                    }
                    valStr = '(' + parsedCols.join(', ') + ')';
                }
                
                // Extract primary key
                const match = valStr.match(/^\s*\(([^,]+)/);
                if (match) {
                    const pk = match[1].trim().replace(/^'|'$/g, '');
                    if (!existingKeys.has(pk) && !existingKeys.has(`'${pk}'`)) {
                        existingRows.push(valStr);
                        existingKeys.add(pk);
                    }
                } else {
                    existingRows.push(valStr);
                }
            });
        }
        
        let newStructBody = structBody;
        if (existingRows.length > 0) {
            const newInsertStmt = `LOCK TABLES \`${tableName}\` WRITE;\n/*!40000 ALTER TABLE \`${tableName}\` DISABLE KEYS */;\nINSERT INTO \`${tableName}\` VALUES ${existingRows.join(',\n')};\n/*!40000 ALTER TABLE \`${tableName}\` ENABLE KEYS */;\nUNLOCK TABLES;`;
            
            const blockRegex = /LOCK TABLES `\w+` WRITE;[\s\S]*?UNLOCK TABLES;/;
            if (structBody.match(blockRegex)) {
                newStructBody = structBody.replace(blockRegex, newInsertStmt);
            } else {
                newStructBody = structBody + '\n\n' + newInsertStmt;
            }
        }
        
        outputSegments.push(structHeader, newStructBody);
    }
    
    const finalContent = mysqlDump.header + outputSegments.join('');
    fs.writeFileSync(dumpFile, finalContent, 'utf8');
    console.log('Successfully merged and padded data from SQL Server dump to MySQL dump file!');
}

mergeData();
