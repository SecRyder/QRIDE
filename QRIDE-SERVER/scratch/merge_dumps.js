const fs = require('fs');
const path = require('path');
const sql = require('mssql');
require('dotenv').config();

const dumpPath = path.join(__dirname, '..', 'Dump20260520.sql');
const qridePath = path.join(__dirname, '..', 'qride_utf8.sql');

const dbConfig = {
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    server: process.env.DB_SERVER,
    database: process.env.DB_NAME,
    options: {
        encrypt: false,
        trustServerCertificate: true
    }
};

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

// Convert MySQL value to MSSQL literal
function toMssqlLiteral(val, type) {
    if (val === 'NULL' || val === null || val === undefined) {
        return 'NULL';
    }
    
    // String types
    if (type === 'string') {
        // escape single quotes
        const escaped = val.replace(/'/g, "''");
        return `N'${escaped}'`;
    }
    
    // Boolean/bit type
    if (type === 'bit') {
        return val === '1' || val === 'true' || val === 1 ? '1' : '0';
    }
    
    // DateTime / Date type
    if (type === 'datetime') {
        // Format: '2026-05-06 12:11:49'
        return `CAST(N'${val.replace(' ', 'T')}' AS DateTime)`;
    }
    if (type === 'date') {
        return `CAST(N'${val}' AS Date)`;
    }
    
    return val;
}

async function merge() {
    const mySqlData = parseMySQLDump(dumpPath);
    
    // 1. Prepare new vouchers
    // Columns: id, type, icon_name, title_key, description_key, discount_text, expiry_text, default_action_key, default_btn_type, has_progress, max_progress, created_at, title_display, status, price
    const newVoucherRows = (mySqlData['vouchers'] || []).filter(r => r[0] === '1');
    const voucherSQLs = newVoucherRows.map(r => {
        const id = r[0];
        const type = toMssqlLiteral(r[1], 'string');
        const icon_name = toMssqlLiteral(r[2], 'string');
        const title_key = toMssqlLiteral(r[3], 'string');
        const description_key = toMssqlLiteral(r[4], 'string');
        const discount_text = toMssqlLiteral(r[5], 'string');
        const expiry_text = toMssqlLiteral(r[6], 'string');
        const default_action_key = toMssqlLiteral(r[7], 'string');
        const default_btn_type = toMssqlLiteral(r[8], 'string');
        const has_progress = toMssqlLiteral(r[9], 'bit');
        const max_progress = toMssqlLiteral(r[10], 'number');
        const created_at = toMssqlLiteral(r[11], 'datetime');
        const title_display = toMssqlLiteral(r[12], 'string');
        const status = toMssqlLiteral(r[13], 'string');
        const price = toMssqlLiteral(r[14], 'number');
        
        return `INSERT [dbo].[vouchers] ([id], [type], [icon_name], [title_key], [description_key], [discount_text], [expiry_text], [default_action_key], [default_btn_type], [has_progress], [max_progress], [created_at], [title_display], [status], [price]) VALUES (${id}, ${type}, ${icon_name}, ${title_key}, ${description_key}, ${discount_text}, ${expiry_text}, ${default_action_key}, ${default_btn_type}, ${has_progress}, ${max_progress}, ${created_at}, ${title_display}, ${status}, ${price})`;
    });

    // 2. Prepare new users
    // Columns: id, phone, password_hash, name, cccd, address, gender, birthday, created_at, referral_code, avatar
    const missingUserIds = ['13', '14', '15', '16', '17', '19', '20'];
    const newUsersRows = (mySqlData['users'] || []).filter(r => missingUserIds.includes(r[0]));
    console.log('Found user rows for IDs:', newUsersRows.map(r => r[0]));
    const userSQLs = newUsersRows.map(r => {
        const id = r[0];
        const phone = toMssqlLiteral(r[1], 'string');
        const password_hash = toMssqlLiteral(r[2], 'string');
        const name = toMssqlLiteral(r[3], 'string');
        let cccd = toMssqlLiteral(r[4], 'string');
        if (['15', '16', '17', '19', '20'].includes(id) && cccd === "N'066204007703'") {
            cccd = `N'0662040077${id}'`;
        }
        const address = toMssqlLiteral(r[5], 'string');
        const gender = toMssqlLiteral(r[6], 'string');
        const birthday = toMssqlLiteral(r[7], 'date');
        const created_at = toMssqlLiteral(r[8], 'datetime');
        const referral_code = toMssqlLiteral(r[9], 'string');
        const avatar = toMssqlLiteral(r[10], 'string');
        
        return `INSERT [dbo].[users] ([id], [phone], [password_hash], [name], [cccd], [address], [gender], [birthday], [created_at], [referral_code], [avatar]) VALUES (${id}, ${phone}, ${password_hash}, ${name}, ${cccd}, ${address}, ${gender}, ${birthday}, ${created_at}, ${referral_code}, ${avatar})`;
    });

    // 3. Prepare new wallets (IDs 10 to 16)
    // Columns: id, user_id, balance, currency, status, created_at, updated_at
    const walletSQLs = [];
    const newUsers = [13, 14, 15, 16, 17, 19, 20];
    const mysqlWallets = mySqlData['wallet'] || [];
    
    newUsers.forEach((userId, index) => {
        const id = 10 + index;
        const myWallet = mysqlWallets.find(w => parseInt(w[1]) === userId);
        const balance = myWallet ? myWallet[2] : '0';
        const currency = myWallet ? toMssqlLiteral(myWallet[3], 'string') : "N'VND'";
        const status = myWallet ? toMssqlLiteral(myWallet[4], 'string') : "N'active'";
        const created_at = myWallet ? toMssqlLiteral(myWallet[5], 'datetime') : 'GETDATE()';
        const updated_at = myWallet ? toMssqlLiteral(myWallet[6], 'datetime') : 'GETDATE()';
        
        walletSQLs.push(`INSERT [dbo].[wallet] ([id], [user_id], [balance], [currency], [status], [created_at], [updated_at]) VALUES (${id}, ${userId}, ${balance}, ${currency}, ${status}, ${created_at}, ${updated_at})`);
    });

    // 4. Prepare new vehicles (IDs 16 to 38, omit column `type`)
    // Columns: id, plate, pin, station_id, current_status, updated_at
    const newVehicleRows = (mySqlData['vehicle'] || []).filter(r => parseInt(r[0]) >= 16);
    const vehicleSQLs = newVehicleRows.map(r => {
        const id = r[0];
        const plate = toMssqlLiteral(r[1], 'string');
        const pin = toMssqlLiteral(r[2], 'number');
        const station_id = toMssqlLiteral(r[3], 'number');
        const current_status = toMssqlLiteral(r[4], 'string');
        const updated_at = toMssqlLiteral(r[5], 'datetime');
        
        return `INSERT [dbo].[vehicle] ([id], [plate], [pin], [station_id], [current_status], [updated_at]) VALUES (${id}, ${plate}, ${pin}, ${station_id}, ${current_status}, ${updated_at})`;
    });

    // 5. Prepare new payments (IDs 17 to 28)
    // Columns: id, user_id, rental_id, amount, currency, method, status, transaction_code, external_ref, description, created_at, updated_at, payment_type, target_id
    const missingPaymentIds = ['17', '18', '19', '20', '21', '22', '23', '24', '25', '26', '27', '28'];
    const newPaymentsRows = (mySqlData['payments'] || []).filter(r => missingPaymentIds.includes(r[0]));
    const paymentSQLs = newPaymentsRows.map(r => {
        const id = r[0];
        const user_id = toMssqlLiteral(r[1], 'number');
        const rental_id = toMssqlLiteral(r[2], 'number');
        const amount = toMssqlLiteral(r[3], 'number');
        const currency = toMssqlLiteral(r[4], 'string');
        const method = toMssqlLiteral(r[5], 'string');
        const status = toMssqlLiteral(r[6], 'string');
        const transaction_code = toMssqlLiteral(r[7], 'string');
        const external_ref = toMssqlLiteral(r[8], 'string');
        const description = toMssqlLiteral(r[9], 'string');
        const created_at = toMssqlLiteral(r[10], 'datetime');
        const updated_at = toMssqlLiteral(r[11], 'datetime');
        const payment_type = toMssqlLiteral(r[12], 'string');
        const target_id = toMssqlLiteral(r[13], 'number');
        
        return `INSERT [dbo].[payments] ([id], [user_id], [rental_id], [amount], [currency], [method], [status], [transaction_code], [external_ref], [description], [created_at], [updated_at], [payment_type], [target_id]) VALUES (${id}, ${user_id}, ${rental_id}, ${amount}, ${currency}, ${method}, ${status}, ${transaction_code}, ${external_ref}, ${description}, ${created_at}, ${updated_at}, ${payment_type}, ${target_id})`;
    });

    // Now update qride_utf8.sql content
    console.log('Reading original qride_utf8.sql...');
    let qrideContent = fs.readFileSync(qridePath, 'utf8');
    
    // Function to insert rows before IDENTITY_INSERT OFF
    function insertBeforeOff(content, tableName, sqls) {
        if (sqls.length === 0) return content;
        
        const offPattern = new RegExp(`SET IDENTITY_INSERT \\[dbo\\]\\.\\[${tableName}\\] OFF`, 'i');
        const match = content.match(offPattern);
        if (!match) {
            console.error(`Could not find SET IDENTITY_INSERT [dbo].[${tableName}] OFF in qride_utf8.sql`);
            return content;
        }
        
        const insertBlock = '\n' + sqls.join('\n') + '\n';
        const index = match.index;
        return content.substring(0, index) + insertBlock + content.substring(index);
    }
    
    console.log('Merging data into SQL file memory...');
    qrideContent = insertBeforeOff(qrideContent, 'vouchers', voucherSQLs);
    qrideContent = insertBeforeOff(qrideContent, 'users', userSQLs);
    qrideContent = insertBeforeOff(qrideContent, 'wallet', walletSQLs);
    qrideContent = insertBeforeOff(qrideContent, 'vehicle', vehicleSQLs);
    qrideContent = insertBeforeOff(qrideContent, 'payments', paymentSQLs);
    
    // console.log('Writing updated qride_utf8.sql...');
    // fs.writeFileSync(qridePath, qrideContent, 'utf8');
    // console.log('SQL file updated successfully!');

    // Now execute on the active MSSQL database
    console.log('\nConnecting to active database to run INSERTs...');
    try {
        let pool = await sql.connect(dbConfig);
        console.log('Connected to MSSQL');

        const executeGroup = async (tableName, sqls) => {
            if (sqls.length === 0) return;
            console.log(`Inserting new rows into '${tableName}' table...`);
            
            for (let insertSql of sqls) {
                const batchSql = `
                    SET IDENTITY_INSERT [dbo].[${tableName}] ON;
                    ${insertSql};
                    SET IDENTITY_INSERT [dbo].[${tableName}] OFF;
                `;
                try {
                    await pool.request().query(batchSql);
                } catch (err) {
                    // If row already exists, log it but don't fail the whole block
                    if (err.message.includes('Violation of PRIMARY KEY constraint') || err.message.includes('Cannot insert duplicate key')) {
                        console.log(`  Row already exists, skipping: ${insertSql.substring(0, 100)}... Reason: ${err.message}`);
                    } else {
                        console.error(`  Error running SQL: ${insertSql}`, err.message);
                    }
                }
            }
        };

        await executeGroup('vouchers', voucherSQLs);
        await executeGroup('users', userSQLs);
        await executeGroup('wallet', walletSQLs);
        await executeGroup('vehicle', vehicleSQLs);
        await executeGroup('payments', paymentSQLs);
        
        console.log('Database updated successfully!');
        await pool.close();
    } catch (dbErr) {
        console.error('Error updating database:', dbErr);
    }
}

merge();
