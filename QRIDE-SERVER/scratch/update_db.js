require('dotenv').config();
const sql = require("mssql");

const dbConfig = {
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    server: process.env.DB_SERVER,
    database: process.env.DB_NAME,
    options: { encrypt: false, trustServerCertificate: true }
};

async function alterDb() {
    try {
        await sql.connect(dbConfig);
        console.log("Connected");
        
        // Add payment_type
        try {
            await sql.query(`ALTER TABLE payments ADD payment_type VARCHAR(50) DEFAULT 'topup'`);
            console.log("Added payment_type");
        } catch (e) {
            console.log("payment_type might exist: " + e.message);
        }

        // Add target_id
        try {
            await sql.query(`ALTER TABLE payments ADD target_id INT NULL`);
            console.log("Added target_id");
        } catch (e) {
            console.log("target_id might exist: " + e.message);
        }
        
        process.exit(0);
    } catch (err) {
        console.error(err);
        process.exit(1);
    }
}
alterDb();
