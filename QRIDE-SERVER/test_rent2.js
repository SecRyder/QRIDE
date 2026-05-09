require("dotenv").config();
const sql = require("mssql");
const dbConfig = {
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    server: process.env.DB_SERVER,
    database: process.env.DB_NAME,
    options: { encrypt: false, trustServerCertificate: true }
};

async function test() {
    const pool = await sql.connect(dbConfig);
    const transaction = new sql.Transaction(pool);
    await transaction.begin();
    try {
        let queryText = "SELECT * FROM vehicle WITH (UPDLOCK, ROWLOCK) WHERE id=@p0";
        const request = new sql.Request(transaction);
        request.input("p0", 1);
        
        const result = await request.query(queryText);
        console.log("Success");
        await transaction.commit();
    } catch(err) {
        console.error("Error:", err);
        await transaction.rollback();
    }
    process.exit(0);
}
test();
