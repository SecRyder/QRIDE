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
    return pool.request().query("SELECT name, object_definition(object_id) as definition FROM sys.triggers");
}).then(result => {
    console.log(JSON.stringify(result.recordset, null, 2));
    process.exit(0);
}).catch(err => {
    console.error(err);
    process.exit(1);
});
