require("dotenv").config();
const sql = require("mssql");
const dbConfig = {
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    server: process.env.DB_SERVER,
    database: process.env.DB_NAME,
    options: { encrypt: false, trustServerCertificate: true }
};

const db = {
    getConnection: async () => {
        const pool = await sql.connect(dbConfig);
        const transaction = new sql.Transaction(pool);
        await transaction.begin();
        return {
            commit: () => transaction.commit(),
            rollback: () => transaction.rollback(),
            query: async (queryText, params) => {
                const request = new sql.Request(transaction);
                if (params) {
                    params.forEach((val, i) => {
                        request.input(`p${i}`, val);
                    });
                    let i = 0;
                    queryText = queryText.replace(/\?/g, () => `@p${i++}`);
                }
                queryText = queryText.replace(/NOW\(\)/gi, "GETDATE()");
                queryText = queryText.replace(/NOW\(\)/gi, "GETDATE()");
                if (queryText.toUpperCase().includes("LIMIT 1")) {
                    queryText = queryText.replace(/LIMIT 1/gi, "");
                    if (queryText.trim().toUpperCase().startsWith("SELECT") && !queryText.toUpperCase().includes("TOP")) {
                        queryText = queryText.replace(/SELECT/i, "SELECT TOP 1");
                    }
                }
                const result = await request.query(queryText);
                let rows = result.recordset;
                if (!rows && queryText.trim().toUpperCase().startsWith("INSERT")) {
                    const idRes = await new sql.Request(transaction).query("SELECT SCOPE_IDENTITY() AS id");
                    rows = { insertId: idRes.recordset[0].id };
                }
                return [rows, result];
            },
            release: () => { }
        };
    }
};

async function testRent() {
    const userId = 1;
    const vehicleId = 1;
    const conn = await db.getConnection();
    try {
        console.log("Q1");
        await conn.query("SELECT id FROM rental WHERE user_id=? AND status='renting'", [userId]);
        console.log("Q2");
        await conn.query("SELECT * FROM vehicle WITH (UPDLOCK, ROWLOCK) WHERE id=?", [vehicleId]);
        console.log("Q3");
        await conn.query("SELECT * FROM wallet WITH (UPDLOCK, ROWLOCK) WHERE user_id=?", [userId]);
        console.log("Q4");
        await conn.query("SELECT value FROM system_config WHERE [key]='min_wallet_to_rent'");
        console.log("Q5");
        await conn.query(`INSERT INTO rental(vehicle_id, user_id, start_time, status) VALUES (?, ?, NOW(), 'renting')`, [vehicleId, userId]);
        console.log("Q6");
        await conn.query("UPDATE vehicle SET current_status='renting' WHERE id=?", [vehicleId]);
        console.log("Committing");
        await conn.commit();
        console.log("Success");
    } catch(e) {
        console.error("FAIL", e);
        await conn.rollback();
    }
    process.exit(0);
}
testRent();
