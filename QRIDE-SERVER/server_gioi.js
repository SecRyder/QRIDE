require("dotenv").config();
const multer = require("multer");
const path = require("path");
const express = require("express");
const sql = require("mssql");
const cors = require("cors");
const fs = require("fs");
const bcrypt = require("bcrypt");
const momoService = require("./services/momo.service");
const jwt = require("jsonwebtoken");
const SECRET_KEY = process.env.SECRET;
if (!SECRET_KEY) {
    throw new Error("SECRET_KEY is required");
}

const app = express();
app.use(cors());
app.use(express.json({ limit: '10mb' }));
app.use(express.static("public"));
app.use(express.urlencoded({ limit: '10mb', extended: true }));

app.use((req, res, next) => {
    console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
    next();
});

const storage = multer.diskStorage({
    destination: function (req, file, cb) {
        cb(null, "public/uploads");
    },
    filename: function (req, file, cb) {
        cb(null, Date.now() + "-" + file.originalname);
    }
});
const upload = multer({ storage: storage });

// ================= ROUTER =================
const apiRouter = express.Router();
app.use("/api", apiRouter);

// ================= DB SQL SERVER =================
const dbConfig = {
    server: process.env.DB_SERVER || "localhost",
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    port: parseInt(process.env.DB_PORT) || 1433,
    options: {
        encrypt: process.env.DB_ENCRYPT === "true" || false,
        trustServerCertificate: true,
        enableArithAbort: true
    },
    pool: {
        max: 10,
        min: 0,
        idleTimeoutMillis: 30000
    }
};

let pool;

async function getPool() {
    if (!pool) {
        pool = await sql.connect(dbConfig);
        console.log("Connected to SQL Server: " + (process.env.DB_SERVER || "localhost"));
    }
    return pool;
}

getPool().then(async () => {
    try {
        await db.query(`
            IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='vouchers' AND COLUMN_NAME='discount_value')
            ALTER TABLE vouchers ADD discount_value INT NULL DEFAULT 0;
            
            IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='vouchers' AND COLUMN_NAME='discount_type')
            ALTER TABLE vouchers ADD discount_type VARCHAR(10) NULL DEFAULT 'PERCENT';
            
            IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='vouchers' AND COLUMN_NAME='duration_days')
            ALTER TABLE vouchers ADD duration_days INT NULL DEFAULT 30;

            IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='user_vouchers' AND COLUMN_NAME='expiry_date')
            ALTER TABLE user_vouchers ADD expiry_date DATETIME NULL;
        `);
        console.log("Database migration for vouchers/user_vouchers completed.");
    } catch (err) {
        console.error("Database migration failed: ", err.message);
    }
}).catch(err => {
    console.error("Database Connection Failed! ", err.message);
});

// ================= DB HELPER =================
// Wrapper helper: thay thế ? bằng @p1, @p2,... rồi bind params
const db = {
    query: async (queryText, params) => {
        const p = await getPool();
        const request = p.request();

        // Chuyển ? thành @p1, @p2,...
        let i = 0;
        const converted = queryText.replace(/\?/g, () => `@p${++i}`);

        if (params) {
            params.forEach((val, idx) => {
                request.input(`p${idx + 1}`, val === undefined ? null : val);
            });
        }

        const result = await request.query(converted);
        // Trả về [rows] để tương thích với cú pháp cũ [rows] = await db.query(...)
        return [result.recordset || []];
    },

    getConnection: async () => {
        const p = await getPool();
        const transaction = new sql.Transaction(p);
        await transaction.begin();

        return {
            beginTransaction: async () => { },
            commit: async () => { await transaction.commit(); },
            rollback: async () => { await transaction.rollback(); },
            query: async (queryText, params) => {
                const request = new sql.Request(transaction);

                let i = 0;
                const converted = queryText.replace(/\?/g, () => `@p${++i}`);

                if (params) {
                    params.forEach((val, idx) => {
                        request.input(`p${idx + 1}`, val === undefined ? null : val);
                    });
                }

                const result = await request.query(converted);
                // insertId tương thích: lấy từ SCOPE_IDENTITY() nếu cần
                const recordset = result.recordset || [];
                return [recordset, { insertId: recordset[0]?.id || null }];
            },
            release: () => { /* SQL Server transaction tự quản lý, không cần release */ }
        };
    }
};

// ================= LƯU Ý: CẦN SỬA CÁC QUERY INSERT ĐỂ LẤY insertId =================
// SQL Server không có lastID như mysql2. Cần thêm OUTPUT INSERTED.id vào INSERT.
// Ví dụ: INSERT INTO rental(...) OUTPUT INSERTED.id VALUES (...)
// Sau đó: const rows = result.recordset; insertId = rows[0].id

const momoRoute = require("./routes/momo.route");
apiRouter.use("/momo", momoRoute(db));

const mapRouter = require("./routes/map");
apiRouter.use("/", mapRouter);

// ================= HELPER =================
function getDistance(lat1, lon1, lat2, lon2) {
    const R = 6371000;
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = Math.sin(dLat / 2) ** 2 +
        Math.cos(lat1 * Math.PI / 180) *
        Math.cos(lat2 * Math.PI / 180) *
        Math.sin(dLon / 2) ** 2;
    return R * (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
}

app.get("/", (req, res) => {
    res.send("API Qride OK");
});

// ================= LOGIN =================
apiRouter.post("/login", async (req, res) => {
    try {
        const { phone, password } = req.body;
        if (!phone || !password)
            return res.status(400).json({ message: "Thiếu dữ liệu" });

        const [rows] = await db.query(
            "SELECT * FROM users WHERE phone=?",
            [phone]
        );

        if (rows.length === 0)
            return res.status(401).json({ message: "Sai tài khoản hoặc mật khẩu" });

        const user = rows[0];
        const isMatch = await bcrypt.compare(password, user.password_hash);
        if (!isMatch)
            return res.status(401).json({ message: "Sai tài khoản hoặc mật khẩu" });

        const token = jwt.sign(
            { userId: user.id, phone: user.phone },
            SECRET_KEY,
            { expiresIn: "7d" }
        );

        res.json({
            message: "Login success",
            token: token,
            user: { id: user.id, phone: user.phone, name: user.name }
        });
    } catch (err) {
        res.status(500).json({ message: "SERVER_ERROR" });
    }
});

function authMiddleware(req, res, next) {
    const authHeader = req.headers["authorization"];
    if (!authHeader || !authHeader.startsWith("Bearer "))
        return res.status(401).json({ message: "INVALID_TOKEN_FORMAT" });

    const token = authHeader.split(" ")[1];
    try {
        const decoded = jwt.verify(token, SECRET_KEY);
        req.user = decoded;

        req.user.userId = parseInt(decoded.userId);

        console.log("[AUTH] userId:", req.user.userId, "type:", typeof req.user.userId);
        next();
    } catch (err) {
        return res.status(401).json({ message: "INVALID_TOKEN" });
    }
}

// ================= USER =================
apiRouter.get("/user", authMiddleware, async (req, res) => {
    const userId = req.user.userId;
    try {
        // Kiểm tra cột referral_code (Migration) — SQL Server syntax
        try {
            await db.query(`
                IF NOT EXISTS (
                    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_NAME='users' AND COLUMN_NAME='referral_code'
                )
                ALTER TABLE users ADD referral_code VARCHAR(20) NULL
            `);
        } catch (e) { }

        const [rows] = await db.query(
            "SELECT id, phone, name, referral_code, cccd, address, gender, birthday, avatar FROM users WHERE id=?",
            [userId]
        );

        if (rows.length === 0)
            return res.status(404).json({ message: "USER_NOT_FOUND" });

        let user = rows[0];
        if (!user.referral_code) {
            const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            let newCode = "";
            for (let i = 0; i < 8; i++) {
                newCode += chars.charAt(Math.floor(Math.random() * chars.length));
            }
            await db.query("UPDATE users SET referral_code = ? WHERE id = ?", [newCode, userId]);
            user.referral_code = newCode;
        }

        res.json(user);
    } catch (err) {
        console.error("GET_USER_ERROR:", err);
        res.status(500).json({ message: "SERVER_ERROR", detail: err.message });
    }
});

// ================== UPDATE USER =================
apiRouter.post("/user/update", authMiddleware, async (req, res) => {
    const userId = req.user.userId;
    const { name, cccd, address, gender, birthday, avatar } = req.body;
    try {
        if (avatar) {
            await db.query(
                `UPDATE users SET name=?, cccd=?, address=?, gender=?, birthday=?, avatar=? WHERE id=?`,
                [name, cccd, address, gender, birthday, avatar, userId]
            );
        } else {
            await db.query(
                `UPDATE users SET name=?, cccd=?, address=?, gender=?, birthday=? WHERE id=?`,
                [name, cccd, address, gender, birthday, userId]
            );
        }
        res.json({ message: "SUCCESS" });
    } catch (err) {
        console.error("UPDATE_USER_ERROR:", err);
        res.status(500).json({ message: "SERVER_ERROR" });
    }
});

// ================= CHECK PHONE =================
apiRouter.post("/check-phone", async (req, res) => {
    const [rows] = await db.query(
        "SELECT id FROM users WHERE phone=?",
        [req.body.phone]
    );
    res.json({ exists: rows.length > 0 });
});

apiRouter.get("/check-phone/:phone", async (req, res) => {
    const [rows] = await db.query(
        "SELECT id FROM users WHERE phone=?",
        [req.params.phone]
    );
    res.json({ exists: rows.length > 0 });
});

// ================= REGISTER =================
apiRouter.post("/register", async (req, res) => {
    try {
        const { phone, password, name, cccd, address, gender, birthday } = req.body;
        if (!phone || !password || !name)
            return res.status(400).json({ message: "INVALID_INPUT" });

        const hash = await bcrypt.hash(password, 10);

        const [exist] = await db.query(
            "SELECT id FROM users WHERE phone=?",
            [phone]
        );
        if (exist.length > 0)
            return res.status(400).json({ message: "PHONE_EXISTS" });

        const finalBirthday = (birthday === "" || birthday === "null") ? null : birthday;

        await db.query(
            `INSERT INTO users(phone, password_hash, name, cccd, address, gender, birthday)
             VALUES (?, ?, ?, ?, ?, ?, ?)`,
            [phone, hash, name, cccd, address, gender, finalBirthday]
        );

        console.log("Register success for: " + phone);
        res.json({ message: "SUCCESS" });
    } catch (err) {
        console.error("Register Error: ", err);
        res.status(500).json({ message: "SERVER_ERROR", detail: err.message });
    }
});

apiRouter.post("/change-phone", authMiddleware, async (req, res) => {
    const { newPhone } = req.body;
    const userId = req.user.userId;
    await db.query("UPDATE users SET phone=? WHERE id=?", [newPhone, userId]);
    res.json({ message: "SUCCESS" });
});

apiRouter.post("/reset-password", async (req, res) => {
    const { phone, newPassword } = req.body;
    if (!phone || !newPassword)
        return res.status(400).json({ message: "INVALID_INPUT" });

    const hash = await bcrypt.hash(newPassword, 10);

    // SQL Server: dùng @@ROWCOUNT thay vì raw.rowsAffected
    const p = await getPool();
    const request = p.request();
    request.input("hash", hash);
    request.input("phone", phone);
    const result = await request.query(
        "UPDATE users SET password_hash=@hash WHERE phone=@phone; SELECT @@ROWCOUNT AS affected"
    );

    if (result.recordset[0].affected === 0)
        return res.status(404).json({ message: "USER_NOT_FOUND" });

    res.json({ message: "SUCCESS" });
});

// ================= CHANGE PASSWORD =================
apiRouter.post("/user/change-password", authMiddleware, async (req, res) => {
    const userId = req.user.userId;
    const { currentPassword, newPassword } = req.body;
    if (!currentPassword || !newPassword)
        return res.status(400).json({ message: "INVALID_INPUT" });

    try {
        const [rows] = await db.query("SELECT password_hash FROM users WHERE id=?", [userId]);
        if (rows.length === 0) return res.status(404).json({ message: "USER_NOT_FOUND" });

        const isMatch = await bcrypt.compare(currentPassword, rows[0].password_hash);
        if (!isMatch) return res.status(401).json({ message: "WRONG_PASSWORD" });

        const hash = await bcrypt.hash(newPassword, 10);
        await db.query("UPDATE users SET password_hash=? WHERE id=?", [hash, userId]);
        res.json({ message: "SUCCESS" });
    } catch (err) {
        console.error("CHANGE_PASSWORD_ERROR:", err);
        res.status(500).json({ message: "SERVER_ERROR" });
    }
});

// ================= USER STATS =================
// Lưu ý: DATEDIFF trong SQL Server dùng cú pháp DATEDIFF(unit, start, end)
apiRouter.get("/user/stats", authMiddleware, async (req, res) => {
    const userId = req.user.userId;
    try {
        const [rows] = await db.query(
            `SELECT 
                COUNT(*) as total_trips,
                COALESCE(SUM(total_distance), 0) as total_km,
                COALESCE(SUM(DATEDIFF(MINUTE, start_time, COALESCE(end_time, GETDATE()))), 0) as total_minutes
             FROM rental
             WHERE user_id=? AND status='done'`,
            [userId]
        );

        const stats = rows[0] || { total_trips: 0, total_km: 0, total_minutes: 0 };
        res.json({
            trips: stats.total_trips,
            km: Math.round((stats.total_km || 0) * 10) / 10,
            hours: Math.round((stats.total_minutes || 0) / 60 * 10) / 10
        });
    } catch (err) {
        console.error("USER_STATS_ERROR:", err);
        res.status(500).json({ message: "SERVER_ERROR" });
    }
});

// ================= RIDE HISTORY =================
apiRouter.get("/rides", authMiddleware, async (req, res) => {
    const userId = req.user.userId;
    try {
        const [rows] = await db.query(
            `SELECT r.id, r.start_time, r.end_time, r.total_price, r.total_distance,
                    r.status, r.payment_status, v.plate,
                    s.name as station_name
             FROM rental r
             JOIN vehicle v ON r.vehicle_id = v.id
             JOIN stations s ON v.station_id = s.id
             WHERE r.user_id=?
             ORDER BY r.start_time DESC`,
            [userId]
        );
        res.json(rows);
    } catch (err) {
        console.error("RIDES_ERROR:", err);
        res.status(500).json({ message: "SERVER_ERROR" });
    }
});

// ================= MOMO IPN CALLBACK =================
apiRouter.post("/momo/ipn", async (req, res) => {
    try {
        const { orderId, resultCode, amount } = req.body;
        console.log("[MOMO_IPN]", { orderId, resultCode, amount });

        if (resultCode !== 0) {
            await db.query("UPDATE payments SET status='failed' WHERE external_ref=?", [orderId]);
            return res.json({ message: "PAYMENT_FAILED" });
        }

        const conn = await db.getConnection();
        try {
            await conn.beginTransaction();

            const [payments] = await conn.query(
                "SELECT * FROM payments WHERE external_ref=? AND status='pending'",
                [orderId]
            );

            if (payments.length === 0) {
                await conn.rollback();
                return res.json({ message: "PAYMENT_NOT_FOUND" });
            }

            const payment = payments[0];
            await conn.query("UPDATE payments SET status='success' WHERE id=?", [payment.id]);

            if (orderId.startsWith("VIP_")) {
                if (payment.target_id) {
                    // SQL Server: dùng MERGE thay cho IF EXISTS ... UPDATE ... ELSE INSERT
                    await conn.query(`
                        MERGE user_vouchers AS target
                        USING (SELECT ? AS user_id, ? AS voucher_id) AS source
                        ON target.user_id = source.user_id AND target.voucher_id = source.voucher_id
                        WHEN MATCHED THEN
                            UPDATE SET status='ACTIVE', action_key='USING', btn_type='ORANGE', updated_at=GETDATE()
                        WHEN NOT MATCHED THEN
                            INSERT (user_id, voucher_id, status, action_key, btn_type, updated_at)
                            VALUES (?, ?, 'ACTIVE', 'USING', 'ORANGE', GETDATE());
                    `, [payment.user_id, payment.target_id, payment.user_id, payment.target_id]);
                }
            } else {
                const [wallets] = await conn.query("SELECT * FROM wallet WHERE user_id=?", [payment.user_id]);
                if (wallets.length > 0) {
                    const wallet = wallets[0];
                    const newBalance = wallet.balance + payment.amount;
                    await conn.query("UPDATE wallet SET balance=? WHERE id=?", [newBalance, wallet.id]);
                    await conn.query(
                        `INSERT INTO wallet_transactions (wallet_id, payment_id, amount, type, balance_before, balance_after, description)
                         VALUES (?, ?, ?, 'topup', ?, ?, ?)`,
                        [wallet.id, payment.id, payment.amount, wallet.balance, newBalance, 'Nạp tiền qua MoMo']
                    );
                }
            }

            await conn.query(
                "INSERT INTO notifications (user_id, title_key, message_key, type) VALUES (?, ?, ?, ?)",
                [payment.user_id, 'notif_title_voucher', 'notif_msg_voucher', 'PAYMENT']
            );

            await conn.commit();
            res.json({ message: "SUCCESS" });
        } catch (err) {
            await conn.rollback();
            throw err;
        } finally {
            conn.release();
        }
    } catch (err) {
        console.error("MOMO_IPN_ERROR:", err);
        res.status(500).json({ message: "SERVER_ERROR" });
    }
});

// ================= VEHICLE =================
apiRouter.get("/vehicle/:id", async (req, res) => {
    const { id } = req.params;
    try {
        const [vehicles] = await db.query(
            `SELECT v.*, s.name AS station_name, s.address AS station_address
             FROM vehicle v
             LEFT JOIN stations s ON v.station_id = s.id
             WHERE v.id = ?`,
            [id]
        );
        if (vehicles.length === 0)
            return res.status(404).json({ message: "Không tìm thấy xe" });
        res.json(vehicles[0]);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

apiRouter.get("/vehicle-by-plate/:plate", async (req, res) => {
    const [rows] = await db.query(
        `SELECT v.*, s.name as station_name, s.address as station_address
         FROM vehicle v
         JOIN stations s ON v.station_id = s.id
         WHERE v.plate=?`,
        [req.params.plate]
    );
    if (rows.length === 0)
        return res.status(404).json({ message: "Not found" });
    res.json(rows[0]);
});

// ================= VEHICLES BY STATION =================
// SQL Server: ORDER BY ... DESC thay vì LIMIT, dùng TOP ở đầu nếu cần giới hạn
apiRouter.get("/vehicles/:stationId", async (req, res) => {
    try {
        const [rows] = await db.query(
            `SELECT v.id, v.plate, v.pin, v.type, v.current_status,
                    s.name as station_name, s.address as station_address
             FROM vehicle v
             JOIN stations s ON v.station_id = s.id
             WHERE v.station_id=? AND v.current_status='available'
             ORDER BY v.pin DESC`,
            [req.params.stationId]
        );
        res.json(rows);
    } catch (err) {
        console.error("VEHICLES_BY_STATION_ERROR:", err);
        res.status(500).json({ message: "SERVER_ERROR" });
    }
});

// ================= RENT =================
apiRouter.post("/rent", authMiddleware, async (req, res) => {
    const { vehicleId } = req.body;
    if (!vehicleId) {
        return res.status(400).json({ message: "INVALID_VEHICLE_ID" });
    }
    const userId = req.user.userId;
    const conn = await db.getConnection();

    try {
        await conn.beginTransaction();

        const [check] = await conn.query(
            "SELECT id FROM rental WHERE user_id=? AND status='renting'",
            [userId]
        );
        if (check.length > 0) throw new Error("Đang thuê xe");

        const [vehicles] = await conn.query("SELECT * FROM vehicle WHERE id=?", [vehicleId]);
        if (vehicles.length === 0) throw new Error("Không có xe");
        if (vehicles[0].current_status !== "available") throw new Error("Xe không khả dụng");

        const [walletRows] = await conn.query("SELECT * FROM wallet WHERE user_id=?", [userId]);
        if (walletRows.length === 0) {
            await conn.rollback();
            return res.json({ message: "NO_WALLET" });
        }

        const wallet = walletRows[0];
        if (wallet.status !== "active") {
            await conn.rollback();
            return res.json({ message: "WALLET_LOCKED" });
        }

        const [configRows] = await conn.query(
            "SELECT value FROM system_config WHERE [key]='min_wallet_to_rent'"
        );
        const minBalance = configRows.length > 0 ? parseInt(configRows[0].value) : 20000;

        if (wallet.balance < minBalance) {
            await conn.rollback();
            return res.json({ message: "NOT_ENOUGH_MONEY", balance: wallet.balance, need: minBalance });
        }

        // SQL Server: OUTPUT INSERTED.id để lấy insertId
        const [result] = await conn.query(
            `INSERT INTO rental(vehicle_id, user_id, start_time, status)
             OUTPUT INSERTED.id
             VALUES (?, ?, GETDATE(), 'renting')`,
            [vehicleId, userId]
        );

        const rentalId = result[0]?.id;

        await conn.query("UPDATE vehicle SET current_status='renting' WHERE id=?", [vehicleId]);
        await conn.commit();

        res.json({ message: "SUCCESS", rental_id: rentalId });
    } catch (err) {
        await conn.rollback();
        res.json({ message: err.message || err.toString() });
    } finally {
        conn.release();
    }
});

// ================= WALLET =================
apiRouter.get("/wallet", authMiddleware, async (req, res) => {
    const userId = req.user.userId;
    try {
        let [rows] = await db.query("SELECT * FROM wallet WHERE user_id=?", [userId]);

        if (rows.length === 0) {
            await db.query(
                "INSERT INTO wallet (user_id, balance, status) VALUES (?, 0, 'active')",
                [userId]
            );
            [rows] = await db.query("SELECT * FROM wallet WHERE user_id=?", [userId]);
        }

        res.json(rows[0]);
    } catch (err) {
        res.status(500).json({ message: "SERVER_ERROR" });
    }
});

// ================= RETURN =================
apiRouter.post("/return", authMiddleware, async (req, res) => {
    const { vehicleId, lat, lng } = req.body;
    if (!vehicleId || lat == null || lng == null)
        return res.status(400).json({ message: "INVALID_INPUT" });

    const conn = await db.getConnection();
    try {
        await conn.beginTransaction();

        const userId = req.user.userId;

        const [rentals] = await conn.query(
            "SELECT * FROM rental WHERE vehicle_id=? AND user_id=? AND status='renting'",
            [vehicleId, userId]
        );
        if (rentals.length === 0) throw new Error("NO_RENTAL");

        const rental = rentals[0];

        const [stations] = await conn.query("SELECT * FROM stations");
        let minDistance = Infinity;
        for (const s of stations) {
            const d = getDistance(lat, lng, s.lat, s.lng);
            if (d < minDistance) minDistance = d;
        }

        if (minDistance > 5000) {
            await conn.rollback();
            return res.json({ message: "NOT_IN_STATION", distance: Math.floor(minDistance) });
        }

        const startTime = new Date(rental.start_time);
        const minutes = Math.ceil((Date.now() - startTime.getTime()) / 60000);

        // SQL Server: TOP 1 thay vì LIMIT 1
        const [pricingRows] = await conn.query(
            "SELECT TOP 1 * FROM pricing ORDER BY id DESC"
        );
        const pricing = pricingRows[0] || { unlock_fee: 5000, price_per_minute: 1000 };
        const totalPrice = pricing.unlock_fee + minutes * pricing.price_per_minute;

        const [activeVouchers] = await conn.query(`
            SELECT TOP 1 uv.id as user_voucher_id, uv.voucher_id, uv.status, uv.action_key, uv.expiry_date,
                   v.type, v.discount_value, v.discount_type, v.title_display
            FROM user_vouchers uv
            JOIN vouchers v ON uv.voucher_id = v.id
            WHERE uv.user_id = ? 
              AND uv.status = 'ACTIVE' 
              AND uv.action_key = 'USING'
              AND (uv.expiry_date IS NULL OR uv.expiry_date > GETDATE())
            ORDER BY uv.updated_at DESC
        `, [userId]);

        let discountApplied = 0;
        let finalPrice = totalPrice;
        const activeVoucher = activeVouchers[0];

        if (activeVoucher) {
            const val = activeVoucher.discount_value || 0;
            if (activeVoucher.discount_type === 'PERCENT') {
                discountApplied = Math.floor((totalPrice * val) / 100);
            } else if (activeVoucher.discount_type === 'CASH') {
                discountApplied = val;
            }
            discountApplied = Math.min(discountApplied, totalPrice);
            finalPrice = totalPrice - discountApplied;
        }

        const [walletRows] = await conn.query("SELECT * FROM wallet WHERE user_id=?", [userId]);
        if (walletRows.length === 0) throw new Error("NO_WALLET");

        const wallet = walletRows[0];
        if (wallet.balance < finalPrice) {
            await conn.rollback();
            return res.json({ message: "NOT_ENOUGH_MONEY", balance: wallet.balance, need: finalPrice });
        }

        const newBalance = wallet.balance - finalPrice;

        await conn.query(
            `UPDATE rental SET status='done', end_time=GETDATE(), total_price=?, payment_status='paid' WHERE id=?`,
            [finalPrice, rental.id]
        );

        await conn.query("UPDATE vehicle SET current_status='available' WHERE id=?", [vehicleId]);

        // OUTPUT INSERTED.id để lấy paymentId
        const [paymentResult] = await conn.query(
            `INSERT INTO payments(user_id, rental_id, amount, method, status)
             OUTPUT INSERTED.id
             VALUES (?, ?, ?, 'wallet', 'success')`,
            [userId, rental.id, finalPrice]
        );
        const paymentId = paymentResult[0]?.id;

        await conn.query("UPDATE wallet SET balance=? WHERE id=?", [newBalance, wallet.id]);

        const [txResult] = await conn.query(
            `INSERT INTO wallet_transactions(wallet_id, payment_id, rental_id, amount, type, balance_before, balance_after, description)
             OUTPUT INSERTED.id
             VALUES (?, ?, ?, ?, 'payment', ?, ?, ?)`,
            [wallet.id, paymentId, rental.id, -finalPrice, wallet.balance, newBalance, "Thanh toán chuyến đi"]
        );

        if (activeVoucher) {
            if (activeVoucher.type === 'TICH_QUA') {
                await conn.query(
                    "UPDATE user_vouchers SET status = 'USED', action_key = 'USED', btn_type = 'GRAY', updated_at = GETDATE() WHERE id = ?",
                    [activeVoucher.user_voucher_id]
                );
            }
        }

        await conn.commit();

        res.json({
            message: "SUCCESS",
            rental_id: rental.id,
            transaction_id: txResult[0]?.id,
            total_price: finalPrice,
            original_price: totalPrice,
            discount_applied: discountApplied,
            minutes
        });
    } catch (err) {
        await conn.rollback();
        res.status(500).json({ message: err.message || err.toString() });
    } finally {
        conn.release();
    }
});

apiRouter.get("/stations", async (req, res) => {
    try {
        const [rows] = await db.query("SELECT * FROM stations");
        res.json(rows);
    } catch (err) {
        res.status(500).json(err);
    }
});

apiRouter.post("/tracking", authMiddleware, async (req, res) => {
    try {
        const { vehicleId, lat, lng } = req.body;
        if (!vehicleId || !lat || !lng)
            return res.status(400).json({ message: "INVALID_INPUT" });

        const userId = req.user.userId;
        const [rows] = await db.query(
            "SELECT id FROM rental WHERE vehicle_id=? AND user_id=? AND status='renting'",
            [vehicleId, userId]
        );

        if (rows.length === 0) return res.json({ message: "No ride" });

        await db.query(
            "INSERT INTO rental_tracking(rental_id, lat, lng) VALUES (?, ?, ?)",
            [rows[0].id, lat, lng]
        );
        res.json({ message: "Tracked" });
    } catch (err) {
        res.status(500).json({ message: "SERVER_ERROR" });
    }
});

apiRouter.get("/history", authMiddleware, async (req, res) => {
    const userId = req.user.userId;
    const [rows] = await db.query(
        `SELECT r.*, v.plate FROM rental r JOIN vehicle v ON r.vehicle_id = v.id
         WHERE r.user_id=? ORDER BY r.id DESC`,
        [userId]
    );
    res.json(rows);
});

// ================= WALLET TOPUP =================
apiRouter.post("/wallet/topup", authMiddleware, async (req, res) => {
    const { amount } = req.body;
    const userId = req.user.userId;
    if (!Number.isInteger(amount) || amount <= 0)
        return res.status(400).json({ message: "INVALID_AMOUNT" });

    const orderId = "ORDER_" + Date.now();
    try {
        const momoRes = await momoService.createPayment(orderId, amount, "Nap tien Qride");
        await db.query(
            `INSERT INTO payments(user_id, amount, method, status, external_ref) VALUES (?, ?, 'momo', 'pending', ?)`,
            [userId, amount, orderId]
        );
        res.json({ payUrl: momoRes.payUrl, qrCodeUrl: momoRes.qrCodeUrl });
    } catch (err) {
        console.error(err);
        res.status(500).json({ message: "MOMO_ERROR" });
    }
});

apiRouter.post("/payment/vip/momo", authMiddleware, async (req, res) => {
    const { voucherId, amount } = req.body;
    const userId = req.user.userId;

    if (!voucherId || !amount)
        return res.status(400).json({ message: "INVALID_INPUT" });

    // ✅ DEBUG: kiểm tra userId có thực sự tồn tại không
    console.log("[VIP_MOMO] userId from token:", userId);

    const orderId = "VIP_" + Date.now();
    try {
        // ✅ Kiểm tra user tồn tại trước khi INSERT payments (tránh FK error)
        const [userCheck] = await db.query("SELECT id FROM users WHERE id=?", [userId]);
        if (userCheck.length === 0) {
            console.error("[VIP_MOMO] userId không tồn tại trong DB:", userId);
            return res.status(401).json({ message: "USER_NOT_FOUND" });
        }

        const momoRes = await momoService.createPayment(orderId, amount, "Mua goi VIP QRIDE");

        await db.query(
            `INSERT INTO payments(user_id, amount, method, status, external_ref, payment_type, target_id)
             VALUES (?, ?, 'momo', 'pending', ?, 'buy_vip', ?)`,
            [userId, amount, orderId, voucherId]
        );

        res.json({ payUrl: momoRes.payUrl, qrCodeUrl: momoRes.qrCodeUrl });
    } catch (err) {
        console.error("[VIP_MOMO] ERROR:", err);
        res.status(500).json({ message: "MOMO_ERROR" });
    }
});

// ================= WALLET WITHDRAW =================
apiRouter.post("/wallet/withdraw", authMiddleware, async (req, res) => {
    const { amount } = req.body;
    const userId = req.user.userId;
    if (!Number.isFinite(amount) || amount <= 0)
        return res.status(400).json({ message: "INVALID_AMOUNT" });

    const conn = await db.getConnection();
    try {
        await conn.beginTransaction();

        const [walletRows] = await conn.query("SELECT * FROM wallet WHERE user_id=?", [userId]);
        if (walletRows.length === 0) {
            await conn.rollback();
            return res.status(404).json({ message: "NO_WALLET" });
        }

        const wallet = walletRows[0];
        if (wallet.status !== "active") {
            await conn.rollback();
            return res.json({ message: "WALLET_LOCKED" });
        }

        const [configRows] = await conn.query(
            "SELECT value FROM system_config WHERE [key]='min_wallet_balance'"
        );
        const minBalance = configRows.length > 0 ? parseInt(configRows[0].value) : 10000;

        if (wallet.balance - amount < minBalance) {
            await conn.rollback();
            return res.json({ message: "NOT_ENOUGH_MONEY", balance: wallet.balance, min_required: minBalance });
        }

        const newBalance = wallet.balance - amount;
        await conn.query("UPDATE wallet SET balance=? WHERE id=?", [newBalance, wallet.id]);
        await conn.query(
            `INSERT INTO wallet_transactions(wallet_id, amount, type, balance_before, balance_after, description)
             VALUES (?, ?, 'withdraw', ?, ?, ?)`,
            [wallet.id, amount, wallet.balance, newBalance, "Rút tiền về ví MoMo"]
        );

        await conn.commit();
        res.json({ message: "SUCCESS", amount, balance: newBalance });
    } catch (err) {
        await conn.rollback();
        console.error(err);
        res.status(500).json({ message: "SERVER_ERROR" });
    } finally {
        conn.release();
    }
});

// ================= PRICING =================
apiRouter.get("/pricing", async (req, res) => {
    try {
        // SQL Server: TOP 1 thay vì LIMIT 1
        const [rows] = await db.query("SELECT TOP 1 * FROM pricing ORDER BY id DESC");
        if (rows.length === 0) return res.json({ message: "NO_PRICING" });
        res.json(rows[0]);
    } catch (err) {
        res.status(500).json(err);
    }
});

// ================= WALLET HISTORY =================
apiRouter.get("/wallet/history", authMiddleware, async (req, res) => {
    const userId = req.user.userId;
    try {
        const [rows] = await db.query(
            `SELECT wt.id, wt.amount, wt.type, wt.description, wt.balance_after, wt.rental_id, wt.created_at
             FROM wallet_transactions wt
             WHERE wt.wallet_id IN (SELECT id FROM wallet WHERE user_id=?)
             ORDER BY wt.id DESC`,
            [userId]
        );
        res.json(rows);
    } catch (err) {
        res.status(500).json({ message: "SERVER_ERROR" });
    }
});

// ================= TRANSACTION DETAIL =================
apiRouter.get("/transaction/:id", authMiddleware, async (req, res) => {
    const id = req.params.id;
    const userId = req.user.userId;
    try {
        const [rows] = await db.query(
            `SELECT wt.*, p.external_ref, p.method, p.status as payment_status
             FROM wallet_transactions wt
             LEFT JOIN payments p ON wt.payment_id = p.id
             WHERE wt.id=? AND wt.wallet_id IN (SELECT id FROM wallet WHERE user_id=?)`,
            [id, userId]
        );
        if (rows.length === 0) return res.status(404).json({ message: "NOT_FOUND" });

        const tx = rows[0];
        let rental = null;
        if (tx.rental_id) {
            const [r] = await db.query(
                `SELECT r.id, r.start_time, r.end_time, r.total_price, v.plate
                 FROM rental r JOIN vehicle v ON r.vehicle_id = v.id WHERE r.id=?`,
                [tx.rental_id]
            );
            if (r.length > 0) rental = r[0];
        }
        res.json({ ...tx, rental });
    } catch (err) {
        console.error(err);
        res.status(500).json({ message: "SERVER_ERROR" });
    }
});

apiRouter.get("/transaction-by-rental/:rentalId", authMiddleware, async (req, res) => {
    const { rentalId } = req.params;
    const userId = req.user.userId;
    try {
        const [rows] = await db.query(
            `SELECT r.id, r.start_time, r.end_time, r.total_price, v.plate
             FROM rental r JOIN vehicle v ON r.vehicle_id = v.id
             WHERE r.id=? AND r.user_id=?`,
            [rentalId, userId]
        );
        if (rows.length === 0) return res.status(404).json({ message: "NOT_FOUND" });
        res.json(rows[0]);
    } catch (err) {
        res.status(500).json({ message: "SERVER_ERROR" });
    }
});

// ================= VOUCHERS =================
apiRouter.get("/vouchers", authMiddleware, async (req, res) => {
    const userId = req.user.userId;
    const { type } = req.query;
    try {
        let query = `
            SELECT 
                v.id, v.type, v.icon_name AS icon, v.title_display AS title, v.title_key,
                v.discount_text AS discount, v.price, v.expiry_text AS expiry,
                v.has_progress, v.max_progress AS prog_max,
                v.discount_value, v.discount_type, v.duration_days,
                uv.expiry_date,
                COALESCE(uv.current_progress, 0) as prog_curr,
                COALESCE(uv.status, v.status, 'NEW') as status,
                COALESCE(uv.action_key, v.default_action_key) as action,
                COALESCE(uv.btn_type, v.default_btn_type) as btn_type
            FROM vouchers v
            LEFT JOIN user_vouchers uv ON v.id = uv.voucher_id AND uv.user_id = ?
        `;
        const params = [userId];
        if (type) {
            query += " WHERE v.type = ?";
            params.push(type);
        }

        const [rows] = await db.query(query, params);
        res.json(rows);
    } catch (err) {
        console.error(err);
        res.status(500).json({ message: "SERVER_ERROR" });
    }
});

apiRouter.post("/client-log", authMiddleware, async (req, res) => {
    const userId = req.user.userId;
    const { level, message, details } = req.body;
    console.log(`[CLIENT_LOG][${level || 'INFO'}] User ${userId}: ${message}`);
    if (details) console.log("Details:", details);
    res.json({ message: "LOG_RECEIVED" });
});

// ================= VOUCHER PROGRESS =================
apiRouter.post("/vouchers/update-progress", authMiddleware, async (req, res) => {
    const userId = req.user.userId;
    const { voucherId } = req.body;

    try {
        const [vRows] = await db.query("SELECT * FROM vouchers WHERE id = ?", [voucherId]);
        if (vRows.length === 0) return res.status(404).json({ message: "VOUCHER_NOT_FOUND" });
        const voucher = vRows[0];

        const [uvRows] = await db.query(
            "SELECT * FROM user_vouchers WHERE user_id = ? AND voucher_id = ?",
            [userId, voucherId]
        );

        let currentProgress = 0;
        let actionKey = voucher.default_action_key || "PERFORM";
        let btnType = voucher.default_btn_type || "GREEN";
        let status = "IN_PROGRESS";

        if (uvRows.length > 0) {
            const uv = uvRows[0];
            currentProgress = uv.current_progress + 1;
            actionKey = uv.action_key;
            btnType = uv.btn_type;
            status = uv.status;

            if (currentProgress >= voucher.max_progress) {
                currentProgress = voucher.max_progress;
                actionKey = "CLAIM";
                btnType = "GREEN";
                status = "ACTIVE";
            }

            await db.query(
                "UPDATE user_vouchers SET current_progress=?, action_key=?, btn_type=?, status=?, updated_at=GETDATE() WHERE id=?",
                [currentProgress, actionKey, btnType, status, uv.id]
            );
        } else {
            currentProgress = 1;
            if (currentProgress >= voucher.max_progress) {
                actionKey = "CLAIM";
                btnType = "GREEN";
                status = "ACTIVE";
            }
            await db.query(
                "INSERT INTO user_vouchers (user_id, voucher_id, current_progress, status, action_key, btn_type, updated_at) VALUES (?, ?, ?, ?, ?, ?, GETDATE())",
                [userId, voucherId, currentProgress, status, actionKey, btnType]
            );
        }

        res.json({ message: "SUCCESS", current_progress: currentProgress, max_progress: voucher.max_progress, action_key: actionKey });
    } catch (err) {
        console.error("PROGRESS_UPDATE_ERROR:", err);
        res.status(500).json({ message: "SERVER_ERROR", detail: err.message });
    }
});

apiRouter.post("/vouchers/activate", authMiddleware, async (req, res) => {
    const userId = req.user.userId;
    const { voucherId } = req.body;
    try {
        const [vouchers] = await db.query("SELECT * FROM vouchers WHERE id = ?", [voucherId]);
        if (vouchers.length === 0) return res.status(404).json({ message: "VOUCHER_NOT_FOUND" });
        const voucher = vouchers[0];
        const duration = voucher.duration_days || 30;

        await db.query(`
            MERGE user_vouchers AS target
            USING (SELECT ? AS user_id, ? AS voucher_id) AS source
            ON target.user_id = source.user_id AND target.voucher_id = source.voucher_id
            WHEN MATCHED THEN
                UPDATE SET 
                    status = 'ACTIVE', 
                    action_key = 'USING', 
                    btn_type = 'ORANGE',
                    expiry_date = DATEADD(DAY, ?, GETDATE()),
                    updated_at = GETDATE()
            WHEN NOT MATCHED THEN
                INSERT (user_id, voucher_id, status, action_key, current_progress, btn_type, expiry_date, updated_at)
                VALUES (?, ?, 'ACTIVE', 'USING', 0, 'ORANGE', DATEADD(DAY, ?, GETDATE()), GETDATE());
        `, [userId, voucherId, duration, userId, voucherId, duration]);

        res.json({ message: "SUCCESS" });
    } catch (err) {
        console.error(err);
        res.status(500).json({ message: "SERVER_ERROR" });
    }
});

apiRouter.get("/user/active-voucher", authMiddleware, async (req, res) => {
    const userId = req.user.userId;
    try {
        const [rows] = await db.query(`
            SELECT uv.id as user_voucher_id, uv.voucher_id, uv.status, uv.action_key, uv.expiry_date,
                   v.type, v.discount_value, v.discount_type, v.title_display, v.discount_text
            FROM user_vouchers uv
            JOIN vouchers v ON uv.voucher_id = v.id
            WHERE uv.user_id = ? 
              AND uv.status = 'ACTIVE' 
              AND uv.action_key = 'USING'
              AND (uv.expiry_date IS NULL OR uv.expiry_date > GETDATE())
            ORDER BY uv.updated_at DESC
        `, [userId]);
        
        if (rows.length === 0) {
            return res.json(null);
        }
        res.json(rows[0]);
    } catch (err) {
        console.error("GET_ACTIVE_VOUCHER_ERROR:", err);
        res.status(500).json({ message: "SERVER_ERROR" });
    }
});

apiRouter.post("/vouchers/buy", authMiddleware, async (req, res) => {
    const userId = req.user.userId;
    const { voucherId } = req.body;

    const conn = await db.getConnection();
    try {
        await conn.beginTransaction();
        const [vouchers] = await conn.query("SELECT * FROM vouchers WHERE id=?", [voucherId]);
        if (vouchers.length === 0) throw new Error("VOUCHER_NOT_FOUND");
        const voucher = vouchers[0];

        let price = voucher.price;
        if (price === null || price === undefined) {
            const discountStr = voucher.discount_text || voucher.discount || "0";
            price = parseInt(discountStr.replace(/[^0-9]/g, '')) || 0;
        }

        const [wallets] = await conn.query("SELECT * FROM wallet WHERE user_id=?", [userId]);
        if (wallets.length === 0) throw new Error("NO_WALLET");
        const wallet = wallets[0];
        if (wallet.balance < price) throw new Error("INSUFFICIENT_BALANCE");

        await conn.query("UPDATE wallet SET balance = balance - ? WHERE user_id=?", [price, userId]);

        try {
            await conn.query(
                "INSERT INTO wallet_transactions (wallet_id, amount, type, description, created_at) VALUES (?, ?, 'payment', ?, GETDATE())",
                [wallet.id, -price, `Mua gói: ${voucher.title_display || voucher.title}`]
            );
        } catch (e) { console.error("WALLET_TRANSACTION_LOG_ERROR:", e); }

        const duration = voucher.duration_days || 30;
        const status = (voucher.type === 'GOI_HOI_VIEN') ? 'ACTIVE' : 'IN_PROGRESS';
        const actionKey = (voucher.type === 'GOI_HOI_VIEN') ? 'USING' : 'IN_PROGRESS';
        const btnType = (voucher.type === 'GOI_HOI_VIEN') ? 'ORANGE' : 'GREEN';

        await conn.query(`
            MERGE user_vouchers AS target
            USING (SELECT ? AS user_id, ? AS voucher_id) AS source
            ON target.user_id = source.user_id AND target.voucher_id = source.voucher_id
            WHEN MATCHED THEN
                UPDATE SET 
                    status = ?, 
                    action_key = ?, 
                    btn_type = ?,
                    expiry_date = CASE WHEN ? = 'GOI_HOI_VIEN' THEN DATEADD(DAY, ?, GETDATE()) ELSE NULL END,
                    updated_at = GETDATE()
            WHEN NOT MATCHED THEN
                INSERT (user_id, voucher_id, status, action_key, current_progress, btn_type, expiry_date, updated_at)
                VALUES (?, ?, ?, ?, 0, ?, CASE WHEN ? = 'GOI_HOI_VIEN' THEN DATEADD(DAY, ?, GETDATE()) ELSE NULL END, GETDATE());
        `, [
            userId, voucherId, 
            status, actionKey, btnType, voucher.type, duration,
            userId, voucherId, status, actionKey, btnType, voucher.type, duration
        ]);

        await conn.commit();
        res.json({ message: "SUCCESS", newBalance: wallet.balance - price });
    } catch (err) {
        if (conn) await conn.rollback();
        console.error("BUY_VOUCHER_ERROR:", err);
        res.status(500).json({ message: "SERVER_ERROR", detail: err.message });
    } finally {
        if (conn) conn.release();
    }
});

// ================= REFERRAL =================
apiRouter.post("/referral/submit", authMiddleware, async (req, res) => {
    const userId = req.user.userId;
    const { code } = req.body;
    try {
        const [targetUser] = await db.query("SELECT id FROM users WHERE referral_code=?", [code]);
        if (targetUser.length === 0)
            return res.status(404).json({ message: "Mã giới thiệu không tồn tại" });
        if (targetUser[0].id === userId)
            return res.status(400).json({ message: "Bạn không thể nhập mã của chính mình" });

        await db.query(`
            UPDATE user_vouchers 
            SET current_progress = current_progress + 1, updated_at = GETDATE()
            WHERE user_id = ? AND voucher_id IN (SELECT id FROM vouchers WHERE action = 'INVITE')
        `, [targetUser[0].id]);

        res.json({ message: "Nhập mã thành công! Quà sẽ được gửi sớm." });
    } catch (err) {
        console.error(err);
        res.status(500).json({ message: "SERVER_ERROR" });
    }
});

// ================= NOTIFICATIONS =================
apiRouter.get("/notifications", authMiddleware, async (req, res) => {
    const userId = req.user.userId;
    try {
        const [rows] = await db.query(
            "SELECT * FROM notifications WHERE user_id=? ORDER BY created_at DESC",
            [userId]
        );
        res.json(rows);
    } catch (err) {
        console.error(err);
        res.status(500).json({ message: "SERVER_ERROR" });
    }
});

apiRouter.post("/notifications/add", authMiddleware, async (req, res) => {
    const userId = req.user.userId;
    const { title_key, message_key, type } = req.body;
    try {
        await db.query(
            "INSERT INTO notifications (user_id, title_key, message_key, type) VALUES (?, ?, ?, ?)",
            [userId, title_key, message_key, type]
        );
        res.json({ message: "SUCCESS" });
    } catch (err) {
        console.error(err);
        res.status(500).json({ message: "SERVER_ERROR" });
    }
});

// ================= ADMIN =================
apiRouter.get("/admin/stats/rentals", async (req, res) => {
    try {
        const [rows] = await db.query(
            `SELECT CAST(start_time AS DATE) as date, COUNT(*) as count, SUM(total_price) as revenue
             FROM rental
             WHERE start_time >= DATEADD(DAY, -30, GETDATE())
             GROUP BY CAST(start_time AS DATE)
             ORDER BY date ASC`
        );
        res.json(rows);
    } catch (err) {
        console.error("ADMIN_RENTAL_STATS_ERROR:", err);
        res.status(500).json({ message: err.message });
    }
});

apiRouter.get("/admin/stats/rental-status", async (req, res) => {
    try {
        const [rows] = await db.query("SELECT status, COUNT(*) as count FROM rental GROUP BY status");
        res.json(rows);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

apiRouter.get("/admin/stats/users", async (req, res) => {
    try {
        const [rows] = await db.query(
            `SELECT CAST(created_at AS DATE) as date, COUNT(*) as count
             FROM users
             WHERE created_at >= DATEADD(DAY, -30, GETDATE())
             GROUP BY CAST(created_at AS DATE)
             ORDER BY date ASC`
        );
        res.json(rows);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

apiRouter.get("/admin/users", async (req, res) => {
    try {
        const [rows] = await db.query(
            "SELECT id, phone, name, cccd, address, gender, birthday, created_at FROM users ORDER BY id DESC"
        );
        res.json(rows);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

apiRouter.get("/admin/users/:id/rentals", async (req, res) => {
    const userId = req.params.id;
    try {
        const [rows] = await db.query(
            `SELECT r.id, r.vehicle_id, v.plate AS vehicle_plate, v.type AS vehicle_type,
                    r.start_time, r.end_time, r.total_distance, r.total_price, r.status, r.payment_status
             FROM rental r JOIN vehicle v ON r.vehicle_id = v.id
             WHERE r.user_id=? ORDER BY r.start_time DESC`,
            [userId]
        );
        res.json(rows);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

apiRouter.delete("/admin/users/:id", async (req, res) => {
    const userId = req.params.id;
    const conn = await db.getConnection();
    try {
        await conn.beginTransaction();
        await conn.query("DELETE FROM notifications WHERE user_id=?", [userId]);
        await conn.query("DELETE FROM user_memberships WHERE user_id=?", [userId]);
        await conn.query("DELETE FROM wallet_transactions WHERE wallet_id IN (SELECT id FROM wallet WHERE user_id=?)", [userId]);
        await conn.query("DELETE FROM wallet WHERE user_id=?", [userId]);
        await conn.query("DELETE FROM payment_transactions WHERE payment_id IN (SELECT id FROM payments WHERE user_id=?)", [userId]);
        await conn.query("DELETE FROM payments WHERE user_id=?", [userId]);
        await conn.query("DELETE FROM rental_tracking WHERE rental_id IN (SELECT id FROM rental WHERE user_id=?)", [userId]);
        await conn.query("DELETE FROM rental WHERE user_id=?", [userId]);
        await conn.query("DELETE FROM users WHERE id=?", [userId]);
        await conn.commit();
        res.json({ message: "SUCCESS" });
    } catch (err) {
        await conn.rollback();
        res.status(500).json({ message: err.message });
    } finally {
        conn.release();
    }
});

apiRouter.get("/admin/vouchers", async (req, res) => {
    try {
        const [rows] = await db.query(`
            SELECT id, type, status, icon_name AS icon, title_display AS title, title_key,
                   discount_text AS discount, price, expiry_text AS expiry,
                   default_action_key AS action, default_btn_type AS btn_type,
                   has_progress, max_progress AS prog_max, created_at
            FROM vouchers ORDER BY id DESC
        `);
        res.header("X-Debug-Version", "1.0.1");
        res.json(rows);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

apiRouter.post("/admin/vouchers", async (req, res) => {
    const { type, icon, display_title, title, discount, price, expiry, action, btn_type, has_progress, prog_max } = req.body;
    try {
        await db.query(
            `INSERT INTO vouchers (type, icon_name, title_display, title_key, discount_text, price, expiry_text, default_action_key, default_btn_type, has_progress, max_progress, status)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
            [type, icon || 'ic_wallet', display_title, title, discount || '', price || 0, expiry || '', action || 'CLAIM', btn_type || 'GREEN', has_progress ? 1 : 0, prog_max || 0, 'ACTIVE']
        );
        res.json({ message: "SUCCESS" });
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

apiRouter.put("/admin/vouchers/:id", async (req, res) => {
    const { id } = req.params;
    const { type, icon, display_title, title, discount, price, expiry, action, btn_type, has_progress, prog_max } = req.body;
    try {
        await db.query(
            `UPDATE vouchers SET type=?, icon_name=?, title_display=?, title_key=?, discount_text=?, price=?, expiry_text=?, default_action_key=?, default_btn_type=?, has_progress=?, max_progress=?, status=? WHERE id=?`,
            [type, icon || 'ic_wallet', display_title, title, discount, price || 0, expiry, action, btn_type, has_progress ? 1 : 0, prog_max, req.body.status || 'ACTIVE', id]
        );
        res.json({ message: "SUCCESS" });
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

apiRouter.delete("/admin/vouchers/:id", async (req, res) => {
    try {
        await db.query("DELETE FROM vouchers WHERE id=?", [req.params.id]);
        res.json({ message: "SUCCESS" });
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

apiRouter.get("/admin/voucher-actions", async (req, res) => {
    try {
        const [rows] = await db.query("SELECT * FROM voucher_actions ORDER BY id ASC");
        res.json(rows);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

apiRouter.post("/admin/voucher-actions", async (req, res) => {
    const { action_key, label } = req.body;
    try {
        // SQL Server: MERGE thay cho INSERT IGNORE
        await db.query(`
            MERGE voucher_actions AS target
            USING (SELECT ? AS action_key) AS source ON target.action_key = source.action_key
            WHEN NOT MATCHED THEN INSERT (action_key, label) VALUES (?, ?);
        `, [action_key, action_key, label]);
        res.json({ message: "SUCCESS" });
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

apiRouter.delete("/admin/voucher-actions/:id", async (req, res) => {
    try {
        await db.query("DELETE FROM voucher_actions WHERE id=?", [req.params.id]);
        res.json({ message: "SUCCESS" });
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

apiRouter.get("/admin/rentals/active", async (req, res) => {
    try {
        const [rows] = await db.query(
            `SELECT r.id, r.user_id, u.name AS user_name, u.phone AS user_phone,
                    r.vehicle_id, v.plate AS vehicle_plate, v.type AS vehicle_type, v.current_status AS vehicle_status,
                    r.start_time, r.total_distance, r.total_price, r.payment_status
             FROM rental r
             JOIN users u ON r.user_id = u.id
             JOIN vehicle v ON r.vehicle_id = v.id
             WHERE r.status = 'renting'
             ORDER BY r.start_time DESC`
        );
        res.json(rows);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

apiRouter.get("/admin/vehicles", async (req, res) => {
    try {
        const [rows] = await db.query(
            `SELECT v.id, v.plate, v.pin, v.type, v.current_status,
                    v.station_id, s.name AS station_name, s.address AS station_address
             FROM vehicle v
             LEFT JOIN stations s ON v.station_id = s.id
             ORDER BY v.id DESC`
        );
        res.json(rows);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

apiRouter.get("/admin/stations", async (req, res) => {
    try {
        const [rows] = await db.query("SELECT * FROM stations ORDER BY id DESC");
        res.json(rows);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

apiRouter.post("/admin/pricing", async (req, res) => {
    const { unlock_fee, price_per_minute, price_per_km, min_wallet_to_rent, low_balance_warning } = req.body;
    try {
        await db.query(
            `INSERT INTO pricing (unlock_fee, price_per_minute, price_per_km, min_wallet_to_rent, low_balance_warning, created_at)
             VALUES (?, ?, ?, ?, ?, GETDATE())`,
            [unlock_fee || 0, price_per_minute || 0, price_per_km || 0, min_wallet_to_rent || 0, low_balance_warning || 0]
        );
        res.json({ message: "SUCCESS" });
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

apiRouter.get("/admin/pricing", async (req, res) => {
    try {
        const [rows] = await db.query("SELECT TOP 1 * FROM pricing ORDER BY id DESC");
        if (rows.length === 0) return res.json({});
        res.json(rows[0]);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

// ================= REVIEWS =================
apiRouter.post("/reviews", authMiddleware, async (req, res) => {
    const userId = req.user.userId;
    const { rental_id, rating, comment } = req.body;
    if (!rental_id || !rating)
        return res.status(400).json({ message: "INVALID_INPUT" });

    try {
        const [rides] = await db.query(
            "SELECT id FROM rental WHERE id=? AND user_id=? AND status='done'",
            [rental_id, userId]
        );
        if (rides.length === 0)
            return res.status(403).json({ message: "NOT_YOUR_RIDE" });

        await db.query(
            "INSERT INTO reviews (rental_id, user_id, rating, comment) VALUES (?,?,?,?)",
            [rental_id, userId, rating, comment || ""]
        );

        if (Number(rating) === 5) {
            const autoTitle = "Đã hoàn thành một chuyến đi tuyệt vời!";
            const autoContent = comment ? `"${comment}"` : "Đã đánh giá 5 sao cho chuyến xe này trên QRIDE! 🌟";
            await db.query(
                "INSERT INTO trip_posts (user_id, rental_id, title, content, location) VALUES (?, ?, ?, ?, ?)",
                [userId, rental_id, autoTitle, autoContent, "Chuyến đi QRIDE"]
            );
        }

        res.json({ message: "REVIEW_SUCCESS" });
    } catch (err) {
        console.log(err);
        // SQL Server: lỗi unique constraint có number 2627 hoặc 2601
        if (err.number === 2627 || err.number === 2601) {
            return res.status(400).json({ message: "ALREADY_REVIEWED" });
        }
        res.status(500).json({ message: "SERVER_ERROR" });
    }
});

apiRouter.get("/reviews/:rentalId", authMiddleware, async (req, res) => {
    const rentalId = req.params.rentalId;
    const [rows] = await db.query(
        "SELECT reviews.*, users.name, users.avatar FROM reviews JOIN users ON reviews.user_id=users.id WHERE rental_id=?",
        [rentalId]
    );
    res.json(rows);
});

// ================= COMMUNITY =================
apiRouter.post("/community/posts", authMiddleware, upload.single("image"), async (req, res) => {
    try {
        const userId = req.user.userId;
        const { rental_id, title, content, location, image_url } = req.body;

        let imageUrl = null;
        if (req.file) {
            imageUrl = "/uploads/" + req.file.filename;
        } else if (image_url && (image_url.startsWith("data:image") || image_url.length > 500)) {
            try {
                let base64Data = image_url;
                let ext = "jpg";
                if (image_url.startsWith("data:image")) {
                    const matches = image_url.match(/^data:image\/([A-Za-z-+\/]+);base64,(.+)$/);
                    if (matches) { ext = matches[1]; base64Data = matches[2]; }
                }
                const buffer = Buffer.from(base64Data, 'base64');
                const filename = `${Date.now()}-base64.${ext}`;
                const uploadPath = path.join(__dirname, "public/uploads", filename);
                fs.writeFileSync(uploadPath, buffer);
                imageUrl = "/uploads/" + filename;
            } catch (base64Err) {
                console.error("Lỗi chuyển đổi Base64:", base64Err);
                imageUrl = image_url;
            }
        } else if (image_url) {
            imageUrl = image_url;
        }

        const [result] = await db.query(
            `INSERT INTO trip_posts(user_id, rental_id, title, content, location, image_url)
             OUTPUT INSERTED.id
             VALUES (?, ?, ?, ?, ?, ?)`,
            [userId, rental_id || null, title, content, location, imageUrl]
        );

        res.json({ message: "SUCCESS", postId: result[0]?.id });
    } catch (err) {
        console.error("POST_TRIP_ERROR:", err);
        res.status(500).json({ message: "SERVER_ERROR", detail: err.message });
    }
});

apiRouter.get("/community/feed", authMiddleware, async (req, res) => {
    const [rows] = await db.query(`
        SELECT p.id, p.title, p.content, p.location, p.image_url, p.created_at,
               u.name, u.avatar,
               COUNT(DISTINCT l.id) AS likes,
               COUNT(DISTINCT c.id) AS comments
        FROM trip_posts p
        JOIN users u ON p.user_id = u.id
        LEFT JOIN post_likes l ON p.id = l.post_id
        LEFT JOIN post_comments c ON p.id = c.post_id
        GROUP BY p.id, p.title, p.content, p.location, p.image_url, p.created_at, u.name, u.avatar
        ORDER BY p.created_at DESC
    `);
    res.json(rows);
});

apiRouter.post("/community/like", authMiddleware, async (req, res) => {
    const userId = req.user.userId;
    const { postId } = req.body;
    const [check] = await db.query(
        "SELECT id FROM post_likes WHERE post_id=? AND user_id=?",
        [postId, userId]
    );
    if (check.length > 0) {
        await db.query("DELETE FROM post_likes WHERE post_id=? AND user_id=?", [postId, userId]);
        return res.json({ liked: false });
    }
    await db.query("INSERT INTO post_likes (post_id, user_id) VALUES(?,?)", [postId, userId]);
    res.json({ liked: true });
});

apiRouter.post("/community/upload", authMiddleware, upload.single("image"), (req, res) => {
    if (!req.file) return res.status(400).json({ message: "NO_IMAGE" });
    res.json({ url: "/uploads/" + req.file.filename });
});

apiRouter.post("/community/comment", authMiddleware, async (req, res) => {
    try {
        const userId = req.user.userId;
        await db.query(
            "INSERT INTO post_comments (post_id, user_id, content) VALUES(?,?,?)",
            [req.body.postId, userId, req.body.content]
        );
        res.json({ message: "COMMENT_ADDED" });
    } catch (e) {
        res.status(500).json({ message: e.message });
    }
});

apiRouter.get("/community/comments/:postId", authMiddleware, async (req, res) => {
    const [rows] = await db.query(
        "SELECT c.*, u.name, u.avatar FROM post_comments c JOIN users u ON c.user_id=u.id WHERE post_id=? ORDER BY created_at DESC",
        [req.params.postId]
    );
    res.json(rows);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, "0.0.0.0", () => {
    console.log(`Server running: http://localhost:${PORT}`);
});