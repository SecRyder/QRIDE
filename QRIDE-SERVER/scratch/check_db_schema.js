const sql = require("mssql");
require("dotenv").config();

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

async function checkSchema() {
    try {
        let pool = await sql.connect(dbConfig);
        console.log("Connected to MSSQL Database");
        
        const result = await pool.request().query(`
            SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_NAME = 'vehicle'
        `);
        console.log("Columns of table 'vehicle':");
        console.log(result.recordset);
        
        await pool.close();
    } catch (err) {
        console.error("Error checking schema:", err);
    }
}

checkSchema();
