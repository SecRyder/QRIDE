require("dotenv").config();
const sql = require("mssql");
const dbConfig = {
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    server: process.env.DB_SERVER,
    database: process.env.DB_NAME,
    options: { encrypt: false, trustServerCertificate: true }
};

sql.connect(dbConfig).then(async pool => {
    console.log("Updating all vehicle types to bike first...");
    await pool.request().query("UPDATE [dbo].[vehicle] SET [type] = 'bike'");

    console.log("Updating motor vehicle types...");
    const motorIds = [15, 18, 19, 22, 26, 27, 28, 32, 33, 36, 37, 38];
    await pool.request().query(`UPDATE [dbo].[vehicle] SET [type] = 'motor' WHERE [id] IN (${motorIds.join(',')})`);
    console.log("Updated all vehicle types successfully.");

    process.exit(0);
}).catch(err => {
    console.error("Error updating database:", err);
    process.exit(1);
});
