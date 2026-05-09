require("dotenv").config();
const sql = require("mssql");
const dbConfig = {
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    server: process.env.DB_SERVER,
    database: process.env.DB_NAME,
    options: { encrypt: false, trustServerCertificate: true }
};

sql.connect(dbConfig).then(pool => {
    return pool.request().query("UPDATE vehicle SET current_status='available'; DELETE FROM rental;");
}).then(() => process.exit(0)).catch(err => process.exit(1));
