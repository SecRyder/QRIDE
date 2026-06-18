require("dotenv").config();
const multer = require("multer");
const path = require("path");
const express = require("express");
const mysql = require("mysql2/promise");
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

// Request Logger để Debug 404/500
app.use((req, res, next) => {
    console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
    next();
});

const storage = multer.diskStorage({
    destination: function (req, file, cb) {
        cb(null, "public/uploads");
    },
    filename: function (req, file, cb) {
        cb(
            null,
            Date.now() + "-" + file.originalname
        );
    }
});
const upload = multer({
    storage: storage
});

// ================= ROUTER =================
const apiRouter = express.Router();
app.use("/api", apiRouter);

// ================= DB MYSQL =================
const pool = mysql.createPool({
    host: process.env.DB_SERVER || "localhost",
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    port: process.env.DB_PORT || 3306,
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0,
    timezone: '+07:00',
    dateStrings: true
});

// Kiểm tra kết nối khi khởi động
pool.getConnection()
    .then(conn => {
        console.log("Connected to MySQL: " + (process.env.DB_SERVER || "localhost"));
        conn.release();
    })
    .catch(err => {
        console.error("Database Connection Failed! ", err.message);
    });

const db = {
    query: async (queryText, params) => {
        const [rows, fields] = await pool.execute(queryText, params || []);
        return [rows, fields];
    },
    getConnection: async () => {
        const conn = await pool.getConnection();
        await conn.beginTransaction();
        return {
            beginTransaction: async () => { },
            commit: () => conn.commit(),
            rollback: () => conn.rollback(),
            query: async (queryText, params) => {
                const [rows, fields] = await conn.execute(queryText, params || []);
                return [rows, fields];
            },
            release: () => conn.release()
        };
    }
};


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

// ================= TEST =================
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
            user: {
                id: user.id,
                phone: user.phone,
                name: user.name
            }
        });

    } catch (err) {
        res.status(500).json({ message: "SERVER_ERROR" });
    }
});

function authMiddleware(req, res, next) {
    const authHeader = req.headers["authorization"];

    if (!authHeader || !authHeader.startsWith("Bearer ")) {
        return res.status(401).json({ message: "INVALID_TOKEN_FORMAT" });
    }

    const token = authHeader.split(" ")[1];

    try {
        const decoded = jwt.verify(token, SECRET_KEY);
        req.user = decoded;
        next();
    } catch (err) {
        return res.status(401).json({ message: "INVALID_TOKEN" });
    }
}


// ================= USER =================
apiRouter.get("/user", authMiddleware, async (req, res) => {
    const userId = req.user.userId;

    try {
        // Tự động kiểm tra và thêm cột referral_code nếu chưa có (Migration)
        try {
            await db.query("ALTER TABLE users ADD referral_code VARCHAR(20) NULL");
        } catch (e) {
            // Cột đã tồn tại hoặc lỗi khác, bỏ qua
        }

        const [rows] = await db.query(
            "SELECT id, phone, name, referral_code, cccd, address, gender, birthday, avatar FROM users WHERE id=?",
            [userId]
        );

        if (rows.length === 0)
            return res.status(404).json({ message: "USER_NOT_FOUND" });

        let user = rows[0];
        // Nếu chưa có mã, tạo mới ngẫu nhiên (8 ký tự: A-Z, 0-9)
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
        // Nếu có gửi avatar mới thì cập nhật, không thì giữ nguyên (tránh bị null đè lên ảnh cũ)
        if (avatar) {
            await db.query(
                `UPDATE users
                 SET name=?, cccd=?, address=?, gender=?, birthday=?, avatar=?
                 WHERE id=?`,
                [name, cccd, address, gender, birthday, avatar, userId]
            );
        } else {
            await db.query(
                `UPDATE users
                 SET name=?, cccd=?, address=?, gender=?, birthday=?
                 WHERE id=?`,
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

        // Kiểm tra số điện thoại tồn tại
        const [exist] = await db.query(
            "SELECT id FROM users WHERE phone=?",
            [phone]
        );

        if (exist.length > 0)
            return res.status(400).json({ message: "PHONE_EXISTS" });

        // Xử lý ngày sinh nếu để trống
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

    await db.query(
        "UPDATE users SET phone=? WHERE id=?",
        [newPhone, userId]
    );

    res.json({ message: "SUCCESS" });
});

apiRouter.post("/reset-password", async (req, res) => {
    const { phone, newPassword } = req.body;

    if (!phone || !newPassword)
        return res.status(400).json({ message: "INVALID_INPUT" });

    const hash = await bcrypt.hash(newPassword, 10);

    const [result] = await db.query(
        "UPDATE users SET password_hash=? WHERE phone=?",
        [hash, phone]
    );

    if (result.affectedRows === 0)
        return res.status(404).json({ message: "USER_NOT_FOUND" });

    res.json({ message: "SUCCESS" });
});

// ================= CHANGE PASSWORD (Authenticated) =================
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

// ================= USER STATS (trips/km/hours for Profile) =================
apiRouter.get("/user/stats", authMiddleware, async (req, res) => {
    const userId = req.user.userId;
    try {
        const [rows] = await db.query(
            `SELECT 
                COUNT(*) as total_trips,
                COALESCE(SUM(total_distance), 0) as total_km,
                COALESCE(
    SUM(
        TIMESTAMPDIFF(
            MINUTE,
            start_time,
            COALESCE(end_time, NOW())
        )
    ),
    0
) as total_minutes
 
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
            // Payment failed
            await db.query("UPDATE payments SET status='failed' WHERE external_ref=?", [orderId]);
            return res.json({ message: "PAYMENT_FAILED" });
        }

        // Payment success
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

            // Check if it's a VIP purchase or wallet topup
            if (orderId.startsWith("VIP_")) {
                // VIP purchase - update user_vouchers
                if (payment.target_id) {
                    const [uvRows] = await conn.query(
                        "SELECT * FROM user_vouchers WHERE user_id=? AND voucher_id=?",
                        [payment.user_id, payment.target_id]
                    );
                    if (uvRows.length > 0) {
                        await conn.query(
                            "UPDATE user_vouchers SET status='ACTIVE', action_key='USING', btn_type='ORANGE', updated_at=NOW() WHERE user_id=? AND voucher_id=?",
                            [payment.user_id, payment.target_id]
                        );
                    } else {
                        await conn.query(
                            "INSERT INTO user_vouchers (user_id, voucher_id, status, action_key, btn_type, updated_at) VALUES (?, ?, 'ACTIVE', 'USING', 'ORANGE', NOW())",
                            [payment.user_id, payment.target_id]
                        );
                    }
                }
            } else {
                // Wallet topup
                const [wallets] = await conn.query("SELECT * FROM wallet WHERE user_id=?", [payment.user_id]);
                if (wallets.length > 0) {
                    const wallet = wallets[0];
                    const newBalance = wallet.balance + payment.amount;
                    await conn.query("UPDATE wallet SET balance=? WHERE id=?", [newBalance, wallet.id]);
                    await conn.query(
                        `INSERT INTO wallet_transactions (wallet_id, payment_id, amount, type, balance_before, balance_after, description, created_at)
                         VALUES (?, ?, ?, 'topup', ?, ?, ?, NOW())`,
                        [wallet.id, payment.id, payment.amount, wallet.balance, newBalance, 'Nạp tiền qua MoMo']
                    );
                }
            }

            // Add notification
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

        if (vehicles.length === 0) {
            return res.status(404).json({ message: "Không tìm thấy xe" });
        }

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

        // const [userRows] = await conn.query(
        //     "SELECT id FROM users WHERE phone=?",
        //     [phone]
        // );

        // if (userRows.length === 0)
        //     throw "User không tồn tại";

        // const userId = userRows[0].id;

        const [check] = await conn.query(
            "SELECT id FROM rental WHERE user_id=? AND status='renting'",
            [userId]
        );

        if (check.length > 0)
            throw new Error("Đang thuê xe");

        const [vehicles] = await conn.query(
            "SELECT * FROM vehicle WHERE id=?",
            [vehicleId]
        );

        if (vehicles.length === 0)
            throw "Không có xe";

        if (vehicles[0].current_status !== "available")
            throw "Xe không khả dụng";

        // ===== CHECK WALLET =====
        const [walletRows] = await conn.query(
            "SELECT * FROM wallet WHERE user_id=?",
            [userId]
        );

        if (walletRows.length === 0) {
            await conn.rollback();
            return res.json({ message: "NO_WALLET" });
        }

        const wallet = walletRows[0];

        // check trạng thái ví
        if (wallet.status !== "active") {
            await conn.rollback();
            return res.json({ message: "WALLET_LOCKED" });
        }

        // check số dư tối thiểu 20k
        // lấy config
        const [configRows] = await conn.query(
            "SELECT value FROM system_config WHERE `key`='min_wallet_to_rent'"
        );

        const minBalance = configRows.length > 0 ? parseInt(configRows[0].value) : 20000;

        // check số dư
        if (wallet.balance < minBalance) {
            await conn.rollback();
            return res.json({
                message: "NOT_ENOUGH_MONEY",
                balance: wallet.balance,
                need: minBalance
            });
        }

        const [result] = await conn.query(
            `INSERT INTO rental(vehicle_id, user_id, start_time, status)
             VALUES (?, ?, NOW(), 'renting')`,
            [vehicleId, userId]
        );


        await conn.query(
            "UPDATE vehicle SET current_status='renting' WHERE id=?",
            [vehicleId]
        );

        await conn.commit();

        res.json({ message: "SUCCESS", rental_id: result.insertId });

    } catch (err) {
        await conn.rollback();
        res.json({ message: err.message || err.toString() });
    } finally {
        conn.release();
    }
});
apiRouter.get("/wallet", authMiddleware, async (req, res) => {
    const userId = req.user.userId;
    try {
        let [rows] = await db.query("SELECT * FROM wallet WHERE user_id=?", [userId]);

        if (rows.length === 0) {
            // Tự tạo ví mới với số dư 0 nếu chưa có
            await db.query("INSERT INTO wallet (user_id, balance, status) VALUES (?, 0, 'active')", [userId]);
            [rows] = await db.query("SELECT * FROM wallet WHERE user_id=?", [userId]);
        }

        res.json(rows[0]);
    } catch (err) {
        res.status(500).json({ message: "SERVER_ERROR" });
    }
});

// ================= RETURN =================
// app.post("/api/return", authMiddleware, async (req, res) => {
//     const { vehicleId, lat, lng } = req.body;

//     const conn = await db.getConnection();

//     try {
//         await conn.beginTransaction();

//         const userId = req.user.userId;

//         const [rentals] = await conn.query(
//             "SELECT * FROM rental WHERE vehicle_id=? AND user_id=? AND status='renting'",
//             [vehicleId, userId]
//         );

//         if (rentals.length === 0)
//             throw "NO_RENTAL";

//         const rental = rentals[0];

//         // ===== CHECK STATION =====
//         const [stations] = await conn.query("SELECT * FROM stations");

//         let minDistance = Infinity;

//         for (const s of stations) {
//             const d = getDistance(lat, lng, s.lat, s.lng);
//             if (d < minDistance) minDistance = d;
//         }

//         if (minDistance > 5000) {
//             await conn.rollback();
//             return res.json({
//                 message: "NOT_IN_STATION",
//                 distance: Math.floor(minDistance)
//             });
//         }

//         // ===== TÍNH THỜI GIAN =====
//         const startTime = new Date(rental.start_time);
//         const endTime = new Date();

//         const minutes = Math.ceil((endTime - startTime) / 60000);

//         // ===== LẤY PRICING =====
//         const [pricingRows] = await conn.query(
//             "SELECT * FROM pricing ORDER BY id DESC LIMIT 1"
//         );

//         const pricing = pricingRows[0] || {
//             price_per_minute: 1000,
//             unlock_fee: 5000
//         };

//         const totalPrice =
//             pricing.unlock_fee + (minutes * pricing.price_per_minute);

//         // ===== TRỪ TIỀN VÍ =====
//         const [walletRows] = await conn.query(
//             "SELECT * FROM wallet WHERE user_id=? FOR UPDATE",
//             [userId]
//         );

//         if (walletRows.length === 0) throw "NO_WALLET";
// 
//         const wallet = walletRows[0];



//         if (wallet.balance < totalPrice) {
//             await conn.rollback();
//             return res.json({
//                 message: "NOT_ENOUGH_MONEY",
//                 balance: wallet.balance,
//                 need: totalPrice
//             });
//         }

//         const newBalance = wallet.balance - totalPrice;

//         // ===== UPDATE DB =====
//         await conn.query(
//             "UPDATE rental SET status='done', end_time=NOW(), total_price=? WHERE id=?",
//             [totalPrice, rental.id]
//         );

//         await conn.query(
//             "UPDATE vehicle SET current_status='available' WHERE id=?",
//             [vehicleId]
//         );

//         await conn.query(
//             "UPDATE wallet SET balance=? WHERE id=?",
//             [newBalance, wallet.id]
//         );

//         const [txResult] = await conn.query(
//             `INSERT INTO wallet_transactions
//             (wallet_id, amount, type, balance_before, balance_after, description)
//             VALUES (?, ?, 'payment', ?, ?, ?)`,
//             [
//                 wallet.id,
//                 totalPrice,
//                 wallet.balance,
//                 newBalance,
//                 "Thanh toán chuyến đi"
//             ]
//         );

//         const transactionId = txResult.insertId;

//         await conn.commit();

//         res.json({
//             message: "SUCCESS",
//             total_price: totalPrice,
//             minutes: minutes,
//             transaction_id: transactionId
//         });

//     } catch (err) {
//         await conn.rollback();
//         res.json({ message: err.toString() });
//     } finally {
//         conn.release();
//     }
// });

apiRouter.post("/return", authMiddleware, async (req, res) => {
    const { vehicleId, lat, lng } = req.body;

    if (!vehicleId || lat == null || lng == null) {
        return res.status(400).json({ message: "INVALID_INPUT" });
    }

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

        // ===== CHECK STATION =====
        const [stations] = await conn.query("SELECT * FROM stations");

        let minDistance = Infinity;

        for (const s of stations) {
            const d = getDistance(lat, lng, s.lat, s.lng);
            if (d < minDistance) minDistance = d;
        }

        if (minDistance > 5000) {
            await conn.rollback();
            return res.json({
                message: "NOT_IN_STATION",
                distance: Math.floor(minDistance)
            });
        }

        // ===== TIME =====
        const startTime = new Date(rental.start_time);
        const minutes = Math.ceil((Date.now() - startTime.getTime()) / 60000);

        // ===== PRICING =====
        const [pricingRows] = await conn.query(
            "SELECT * FROM pricing ORDER BY id DESC LIMIT 1"
        );

        const pricing = pricingRows[0] || {
            unlock_fee: 5000,
            price_per_minute: 1000
        };

        const totalPrice =
            pricing.unlock_fee + minutes * pricing.price_per_minute;

        // ===== WALLET =====
        const [walletRows] = await conn.query(
            "SELECT * FROM wallet WHERE user_id=?",
            [userId]
        );

        if (walletRows.length === 0) throw new Error("NO_WALLET");

        const wallet = walletRows[0];

        if (wallet.balance < totalPrice) {
            await conn.rollback();
            return res.json({
                message: "NOT_ENOUGH_MONEY",
                balance: wallet.balance,
                need: totalPrice
            });
        }

        const newBalance = wallet.balance - totalPrice;

        // ===== UPDATE RENTAL =====
        await conn.query(
            `UPDATE rental 
             SET status='done', end_time=NOW(), total_price=?, payment_status='paid'
             WHERE id=?`,
            [totalPrice, rental.id]
        );

        // ===== UPDATE VEHICLE =====
        await conn.query(
            "UPDATE vehicle SET current_status='available' WHERE id=?",
            [vehicleId]
        );

        // ===== CREATE PAYMENT =====
        const [paymentResult] = await conn.query(
            `INSERT INTO payments(user_id, rental_id, amount, method, status)
             VALUES (?, ?, ?, 'wallet', 'success')`,
            [userId, rental.id, totalPrice]
        );

        const paymentId = paymentResult.insertId;

        // ===== UPDATE WALLET =====
        await conn.query(
            "UPDATE wallet SET balance=? WHERE id=?",
            [newBalance, wallet.id]
        );

        // ===== WALLET TRANSACTION =====
        const [txResult] = await conn.query(
            `INSERT INTO wallet_transactions
            (wallet_id, payment_id, rental_id, amount, type, balance_before, balance_after, description, created_at)
            VALUES (?, ?, ?, ?, 'payment', ?, ?, ?, NOW())`,
            [
                wallet.id,
                paymentId,
                rental.id,
                -totalPrice,
                wallet.balance,
                newBalance,
                "Thanh toán chuyến đi"
            ]
        );

        await conn.commit();

        res.json({
            message: "SUCCESS",
            rental_id: rental.id,
            transaction_id: txResult.insertId,
            total_price: totalPrice,
            minutes
        });

    } catch (err) {
        await conn.rollback();
        res.status(500).json({
            message: err.message || err.toString()
        });
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
        if (!vehicleId || !lat || !lng) {
            return res.status(400).json({ message: "INVALID_INPUT" });
        }

        const userId = req.user.userId;

        const [rows] = await db.query(
            "SELECT id FROM rental WHERE vehicle_id=? AND user_id=? AND status='renting'",
            [vehicleId, userId]
        );

        if (rows.length === 0) {
            return res.json({ message: "No ride" });
        }

        await db.query(
            "INSERT INTO rental_tracking(rental_id, lat, lng) VALUES (?, ?, ?)",
            [rows[0].id, lat, lng]
        );

        res.json({ message: "Tracked" });

    } catch (err) {
        res.status(500).json({ message: "SERVER_ERROR" });
    }
});

// app.get("/api/history/:phone", async (req, res) => {
//     try {
//         const [rows] = await db.query(
//             `SELECT r.*, v.plate
//              FROM rental r
//              JOIN vehicle v ON r.vehicle_id = v.id
//              JOIN users u ON r.user_id = u.id
//              WHERE u.phone=? 
//              ORDER BY r.id DESC`,
//             [req.params.phone]
//         );

//         res.json(rows);

//     } catch (err) {
//         res.status(500).json(err);
//     }
// });
apiRouter.get("/history", authMiddleware, async (req, res) => {
    const userId = req.user.userId;

    const [rows] = await db.query(
        `SELECT r.*, v.plate
         FROM rental r
         JOIN vehicle v ON r.vehicle_id = v.id
         WHERE r.user_id=? 
         ORDER BY r.id DESC`,
        [userId]
    );

    res.json(rows);
});

apiRouter.get("/wallet", authMiddleware, async (req, res) => {
    const userId = req.user.userId;

    const [rows] = await db.query(
        "SELECT * FROM wallet WHERE user_id=?",
        [userId]
    );

    if (rows.length === 0)
        return res.status(404).json({ message: "NO_WALLET" });

    res.json(rows[0]);
});


// ================= API NAP TIEN ======================
// app.post("/api/wallet/topup", authMiddleware, async (req, res) => {
//     const { amount } = req.body;
//     if (!Number.isFinite(amount) || amount <= 0) {
//         return res.status(400).json({ message: "INVALID_AMOUNT" });
//     }
//     const userId = req.user.userId;

//     try {
//         const conn = await db.getConnection();
//         await conn.beginTransaction();

//         const [walletRows] = await conn.query(
//             "SELECT * FROM wallet WHERE user_id=? FOR UPDATE",
//             [userId]
//         );

//         if (walletRows.length === 0) throw "NO_WALLET";

//         const wallet = walletRows[0];
//         const newBalance = wallet.balance + amount;

//         const [paymentResult] = await conn.query(
//             `INSERT INTO payments(user_id, amount, method, status)
//              VALUES (?, ?, 'momo', 'success')`,
//             [userId, amount]
//         );

//         const paymentId = paymentResult.insertId;

//         await conn.query(
//             "UPDATE wallet SET balance=? WHERE id=?",
//             [newBalance, wallet.id]
//         );

//         await conn.query(
//             `INSERT INTO wallet_transactions(wallet_id, payment_id, amount, type, balance_before, balance_after, description)
//              VALUES (?, ?, ?, 'topup', ?, ?, ?)`,
//             [
//                 wallet.id,
//                 paymentId,
//                 amount,
//                 wallet.balance,
//                 newBalance,
//                 "Nạp tiền từ MoMo"
//             ]
//         );

//         await conn.commit();

//         res.json({ message: "SUCCESS", balance: newBalance });

//     } catch (err) {
//         await conn.rollback();
//         res.status(500).json({ message: err });
//     }
// });
apiRouter.post("/wallet/topup", authMiddleware, async (req, res) => {
    const { amount } = req.body;
    const userId = req.user.userId;

    if (!Number.isInteger(amount) || amount <= 0) {
        return res.status(400).json({ message: "INVALID_AMOUNT" });
    }

    const orderId = "ORDER_" + Date.now();

    try {
        const momoRes = await momoService.createPayment(
            orderId,
            amount,
            "Nap tien Qride"
        );

        await db.query(
            `INSERT INTO payments(user_id, amount, method, status, external_ref)
             VALUES (?, ?, 'momo', 'pending', ?)`,
            [userId, amount, orderId]
        );

        res.json({
            payUrl: momoRes.payUrl,
            qrCodeUrl: momoRes.qrCodeUrl
        });

    } catch (err) {
        console.error(err);
        res.status(500).json({ message: "MOMO_ERROR" });
    }
});

apiRouter.post("/payment/vip/momo", authMiddleware, async (req, res) => {
    const { voucherId, amount } = req.body;
    const userId = req.user.userId;

    if (!voucherId || !amount) {
        return res.status(400).json({ message: "INVALID_INPUT" });
    }

    const orderId = "VIP_" + Date.now();

    try {
        const momoRes = await momoService.createPayment(
            orderId,
            amount,
            "Mua goi VIP QRIDE"
        );

        await db.query(
            `INSERT INTO payments(user_id, amount, method, status, external_ref, payment_type, target_id)
             VALUES (?, ?, 'momo', 'pending', ?, 'buy_vip', ?)`,
            [userId, amount, orderId, voucherId]
        );

        res.json({
            payUrl: momoRes.payUrl,
            qrCodeUrl: momoRes.qrCodeUrl
        });

    } catch (err) {
        console.error(err);
        res.status(500).json({ message: "MOMO_ERROR" });
    }
});


// =================== API RUT TIEN ==================
apiRouter.post("/wallet/withdraw", authMiddleware, async (req, res) => {
    const { amount } = req.body;
    const userId = req.user.userId;

    // ===== VALIDATE =====
    if (!Number.isFinite(amount) || amount <= 0) {
        return res.status(400).json({ message: "INVALID_AMOUNT" });
    }

    const conn = await db.getConnection();

    try {
        await conn.beginTransaction();

        // ===== LOCK WALLET =====
        const [walletRows] = await conn.query(
            "SELECT * FROM wallet WHERE user_id=?",
            [userId]
        );

        if (walletRows.length === 0) {
            await conn.rollback();
            return res.status(404).json({ message: "NO_WALLET" });
        }

        const wallet = walletRows[0];

        // ===== CHECK STATUS =====
        if (wallet.status !== "active") {
            await conn.rollback();
            return res.json({ message: "WALLET_LOCKED" });
        }

        // ===== LẤY MIN BALANCE (config) =====
        const [configRows] = await conn.query(
            "SELECT value FROM system_config WHERE `key`='min_wallet_balance'"
        );

        const minBalance = configRows.length > 0
            ? parseInt(configRows[0].value)
            : 10000;

        // ===== CHECK SỐ DƯ =====
        if (wallet.balance - amount < minBalance) {
            await conn.rollback();
            return res.json({
                message: "NOT_ENOUGH_MONEY",
                balance: wallet.balance,
                min_required: minBalance
            });
        }

        const newBalance = wallet.balance - amount;

        // ===== UPDATE WALLET =====
        await conn.query(
            "UPDATE wallet SET balance=? WHERE id=?",
            [newBalance, wallet.id]
        );

        // ===== LOG TRANSACTION =====
        await conn.query(
            `INSERT INTO wallet_transactions
            (wallet_id, amount, type, balance_before, balance_after, description, created_at)
            VALUES (?, ?, 'withdraw', ?, ?, ?, NOW())`,
            [
                wallet.id,
                amount,
                wallet.balance,
                newBalance,
                "Rút tiền về ví MoMo"
            ]
        );

        await conn.commit();

        res.json({
            message: "SUCCESS",
            amount: amount,
            balance: newBalance
        });

    } catch (err) {
        await conn.rollback();
        console.error(err);
        res.status(500).json({ message: "SERVER_ERROR" });
    } finally {
        conn.release();
    }
});

apiRouter.get("/pricing", async (req, res) => {
    try {
        const [rows] = await db.query(
            "SELECT * FROM pricing ORDER BY id DESC LIMIT 1"
        );

        if (rows.length === 0)
            return res.json({ message: "NO_PRICING" });

        res.json(rows[0]);

    } catch (err) {
        res.status(500).json(err);
    }
});

// GET /api/wallet/history
// app.get("/api/wallet/history", authMiddleware, async (req, res) => {
//     const userId = req.user.userId;

//     try {
//         const [rows] = await db.query(
//             `SELECT 
//                 wt.id,
//                 wt.amount,
//                 wt.type,
//                 wt.description,
//                 wt.balance_after,
//                 wt.created_at
//              FROM wallet_transactions wt
//              WHERE wt.wallet_id IN (
//                 SELECT id FROM wallet WHERE user_id=?
//              )
//              ORDER BY wt.id DESC`,
//             [userId]
//         );

//         res.json(rows);

//     } catch (err) {
//         console.error(err);
//         res.status(500).json({ message: "SERVER_ERROR" });
//     }
// });
apiRouter.get("/wallet/history", authMiddleware, async (req, res) => {
    const userId = req.user.userId;

    try {
        const [rows] = await db.query(
            `SELECT 
                wt.id,
                wt.amount,
                wt.type,
                wt.description,
                wt.balance_after,
                wt.rental_id,
                wt.created_at
             FROM wallet_transactions wt
             WHERE wt.wallet_id IN (
                SELECT id FROM wallet WHERE user_id=?
             )
             ORDER BY wt.id DESC`,
            [userId]
        );

        res.json(rows);

    } catch (err) {
        res.status(500).json({ message: "SERVER_ERROR" });
    }
});

// ================ CHI TIET GIAO DICH =============
// GET /api/transaction/:id
apiRouter.get("/transaction/:id", authMiddleware, async (req, res) => {
    const id = req.params.id;
    const userId = req.user.userId;

    try {
        const [rows] = await db.query(
            `SELECT wt.*, p.external_ref, p.method, COALESCE(p.status, 'success') as payment_status
             FROM wallet_transactions wt
             LEFT JOIN payments p ON wt.payment_id = p.id
             WHERE wt.id=? 
             AND wt.wallet_id IN (
                SELECT id FROM wallet WHERE user_id=?
             )`,
            [id, userId]
        );

        if (rows.length === 0) {
            return res.status(404).json({ message: "NOT_FOUND" });
        }

        const tx = rows[0];

        let rental = null;

        if (tx.rental_id) {
            const [r] = await db.query(
                `SELECT r.id, r.start_time, r.end_time, r.total_price, v.plate
                 FROM rental r
                 JOIN vehicle v ON r.vehicle_id = v.id
                 WHERE r.id=?`,
                [tx.rental_id]
            );

            if (r.length > 0) rental = r[0];
        }

        res.json({
            ...tx,
            rental
        });

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
            `SELECT 
                r.id,
                r.start_time,
                r.end_time,
                r.total_price,
                v.plate
             FROM rental r
             JOIN vehicle v ON r.vehicle_id = v.id
             WHERE r.id=? AND r.user_id=?`,
            [rentalId, userId]
        );

        if (rows.length === 0) {
            return res.status(404).json({ message: "NOT_FOUND" });
        }

        res.json(rows[0]);

    } catch (err) {
        res.status(500).json({ message: "SERVER_ERROR" });
    }
});

// ================= VOUCHERS =================
apiRouter.get("/vouchers", authMiddleware, async (req, res) => {
    const userId = req.user.userId;
    const { type } = req.query; // TICH_QUA or GOI_HOI_VIEN

    try {
        // Tự động kiểm tra và thêm cột price nếu chưa có
        try {
            await db.query("ALTER TABLE vouchers ADD price INT NULL");
        } catch (e) { }

        let query = `
            SELECT 
                v.id, v.type, v.icon_name AS icon, v.title_display AS title, v.title_key, v.discount_text AS discount, 
                v.price, v.expiry_text AS expiry, v.has_progress, v.max_progress AS prog_max,
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

// Endpoint nhận log lỗi từ Client
apiRouter.post("/client-log", authMiddleware, async (req, res) => {
    const userId = req.user.userId;
    const { level, message, details } = req.body;

    console.log(`[CLIENT_LOG][${level || 'INFO'}] User ${userId}: ${message}`);
    if (details) console.log("Details:", details);

    res.json({ message: "LOG_RECEIVED" });
});

// ================= VOUCHERS API =================

// 1. Cập nhật tiến trình nhiệm vụ (POST)
apiRouter.post("/vouchers/update-progress", authMiddleware, async (req, res) => {
    const userId = req.user.userId;
    const { voucherId } = req.body;
    console.log(`DEBUG: Updating progress for User ${userId}, Voucher ${voucherId}`);

    try {
        // Kiểm tra xem voucher có tồn tại không
        const [vRows] = await db.query("SELECT * FROM vouchers WHERE id = ?", [voucherId]);
        if (vRows.length === 0) return res.status(404).json({ message: "VOUCHER_NOT_FOUND" });
        const voucher = vRows[0];

        // Tìm bản ghi của người dùng
        const [uvRows] = await db.query(
            "SELECT * FROM user_vouchers WHERE user_id = ? AND voucher_id = ?",
            [userId, voucherId]
        );

        let currentProgress = 0;
        let actionKey = voucher.default_action_key || "PERFORM";
        let btnType = voucher.default_btn_type || "GREEN";
        let status = "IN_PROGRESS";

        if (uvRows.length > 0) {
            // Đã có bản ghi -> Tăng tiến trình
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
                "UPDATE user_vouchers SET current_progress = ?, action_key = ?, btn_type = ?, status = ?, updated_at = NOW() WHERE id = ?",
                [currentProgress, actionKey, btnType, status, uv.id]
            );
        } else {
            // Chưa có bản ghi -> Tạo mới với tiến trình = 1
            currentProgress = 1;
            if (currentProgress >= voucher.max_progress) {
                actionKey = "CLAIM";
                btnType = "GREEN";
                status = "ACTIVE";
            }

            await db.query(
                "INSERT INTO user_vouchers (user_id, voucher_id, current_progress, status, action_key, btn_type, updated_at) VALUES (?, ?, ?, ?, ?, ?, NOW())",
                [userId, voucherId, currentProgress, status, actionKey, btnType]
            );
        }

        res.json({
            message: "SUCCESS",
            current_progress: currentProgress,
            max_progress: voucher.max_progress,
            action_key: actionKey
        });

    } catch (err) {
        console.error("PROGRESS_UPDATE_ERROR:", err);
        res.status(500).json({ message: "SERVER_ERROR", detail: err.message });
    }
});

apiRouter.post("/vouchers/activate", authMiddleware, async (req, res) => {
    const userId = req.user.userId;
    const { voucherId } = req.body;

    if (!voucherId) {
        return res.status(400).json({ message: "MISSING_VOUCHER_ID" });
    }

    try {
        // Kiểm tra voucher có tồn tại không
        const [vRows] = await db.query("SELECT id FROM vouchers WHERE id = ?", [voucherId]);
        if (vRows.length === 0) {
            return res.status(404).json({ message: "VOUCHER_NOT_FOUND" });
        }

        // Kiểm tra bản ghi user_vouchers đã tồn tại chưa
        const [uvRows] = await db.query(
            "SELECT id FROM user_vouchers WHERE user_id = ? AND voucher_id = ?",
            [userId, voucherId]
        );

        if (uvRows.length > 0) {
            // Đã có bản ghi -> UPDATE
            await db.query(
                "UPDATE user_vouchers SET status = 'ACTIVE', action_key = 'USING', btn_type = 'ORANGE', updated_at = NOW() WHERE user_id = ? AND voucher_id = ?",
                [userId, voucherId]
            );
        } else {
            // Chưa có bản ghi -> INSERT mới (trường hợp voucher CLAIM không có user_vouchers trước)
            await db.query(
                "INSERT INTO user_vouchers (user_id, voucher_id, status, action_key, btn_type, current_progress, updated_at) VALUES (?, ?, 'ACTIVE', 'USING', 'ORANGE', 0, NOW())",
                [userId, voucherId]
            );
        }

        console.log(`[ACTIVATE_VOUCHER] User ${userId} activated voucher ${voucherId}`);
        res.json({ message: "SUCCESS" });
    } catch (err) {
        console.error("ACTIVATE_VOUCHER_ERROR:", err);
        res.status(500).json({ message: "SERVER_ERROR", detail: err.message });
    }
});

apiRouter.post("/vouchers/buy-with-wallet", authMiddleware, async (req, res) => {
    const userId = req.user.userId;
    const { voucherId } = req.body;

    if (!voucherId) {
        return res.status(400).json({ message: "MISSING_VOUCHER_ID" });
    }

    const conn = await db.getConnection();
    try {
        await conn.beginTransaction();

        const [vouchers] = await conn.query(
            "SELECT * FROM vouchers WHERE id = ? FOR UPDATE",
            [voucherId]
        );
        if (vouchers.length === 0) {
            await conn.rollback();
            return res.status(404).json({ message: "VOUCHER_NOT_FOUND" });
        }

        const voucher = vouchers[0];
        const price = Number(voucher.price || 0);
        if (!Number.isFinite(price) || price < 0) {
            await conn.rollback();
            return res.status(400).json({ message: "INVALID_PRICE" });
        }

        const [wallets] = await conn.query(
            "SELECT * FROM wallet WHERE user_id = ? FOR UPDATE",
            [userId]
        );
        if (wallets.length === 0) {
            await conn.rollback();
            return res.status(404).json({ message: "NO_WALLET" });
        }

        const wallet = wallets[0];
        if (wallet.status && wallet.status !== "active") {
            await conn.rollback();
            return res.status(403).json({ message: "WALLET_LOCKED" });
        }

        if (wallet.balance < price) {
            await conn.rollback();
            return res.status(402).json({
                message: "INSUFFICIENT_BALANCE",
                balance: wallet.balance,
                need: price
            });
        }

        const [activeRows] = await conn.query(
            "SELECT id FROM user_vouchers WHERE user_id = ? AND voucher_id = ? AND status = 'ACTIVE' AND action_key = 'USING'",
            [userId, voucherId]
        );
        if (activeRows.length > 0) {
            await conn.rollback();
            return res.status(409).json({ message: "VOUCHER_ALREADY_ACTIVE" });
        }

        const newBalance = wallet.balance - price;
        const [paymentResult] = await conn.query(
            `INSERT INTO payments(user_id, amount, method, status, payment_type, target_id)
             VALUES (?, ?, 'wallet', 'success', 'buy_vip', ?)`,
            [userId, price, voucherId]
        );

        await conn.query(
            "UPDATE wallet SET balance = ? WHERE id = ?",
            [newBalance, wallet.id]
        );

        await conn.query(
            `INSERT INTO wallet_transactions
             (wallet_id, payment_id, amount, type, balance_before, balance_after, description, created_at)
             VALUES (?, ?, ?, 'payment', ?, ?, ?, NOW())`,
            [
                wallet.id,
                paymentResult.insertId,
                -price,
                wallet.balance,
                newBalance,
                `Mua gói VIP: ${voucher.title_display || voucher.title_key || voucher.title || voucherId}`
            ]
        );

        await conn.query(
            `INSERT INTO user_vouchers (user_id, voucher_id, status, action_key, current_progress, btn_type, updated_at)
             VALUES (?, ?, 'ACTIVE', 'USING', 0, 'ORANGE', NOW())
             ON DUPLICATE KEY UPDATE status='ACTIVE', action_key='USING', btn_type='ORANGE', updated_at=NOW()`,
            [userId, voucherId]
        );

        await conn.commit();
        res.json({
            message: "SUCCESS",
            newBalance,
            paymentId: paymentResult.insertId
        });
    } catch (err) {
        if (conn) await conn.rollback();
        console.error("BUY_VOUCHER_WITH_WALLET_ERROR:", err);
        res.status(500).json({ message: "SERVER_ERROR", detail: err.message });
    } finally {
        if (conn) conn.release();
    }
});

apiRouter.post("/vouchers/buy", authMiddleware, async (req, res) => {
    const userId = req.user.userId;
    const { voucherId } = req.body;

    const conn = await db.getConnection();
    try {
        await conn.beginTransaction();
        const [vouchers] = await conn.query("SELECT * FROM vouchers WHERE id = ?", [voucherId]);
        if (vouchers.length === 0) throw new Error("VOUCHER_NOT_FOUND");
        const voucher = vouchers[0];

        // Sửa lỗi: Sử dụng discount_text thay vì discount
        const discountStr = voucher.discount_text || voucher.discount || "0";
        const price = parseInt(discountStr.replace(/[^0-9]/g, '')) || 0;

        const [wallets] = await conn.query("SELECT * FROM wallet WHERE user_id = ?", [userId]);
        if (wallets.length === 0) throw new Error("NO_WALLET");
        const wallet = wallets[0];
        if (wallet.balance < price) throw new Error("INSUFFICIENT_BALANCE");

        await conn.query("UPDATE wallet SET balance = balance - ? WHERE user_id = ?", [price, userId]);

        try {
            await conn.query(
                "INSERT INTO wallet_transactions (wallet_id, amount, type, description, created_at) VALUES (?, ?, 'payment', ?, NOW())",
                [wallet.id, -price, `Mua gói: ${voucher.title}`,]
            );
        } catch (e) { console.error("WALLET_TRANSACTION_LOG_ERROR:", e); }

        await conn.query(
            `INSERT INTO user_vouchers (user_id, voucher_id, status, action_key, current_progress, btn_type, updated_at)
             VALUES (?, ?, 'IN_PROGRESS', 'IN_PROGRESS', 0, 'GREEN', NOW())
             ON DUPLICATE KEY UPDATE status='IN_PROGRESS', action_key='IN_PROGRESS', updated_at=NOW()`,
            [userId, voucherId]
        );

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

// ================= REFERRAL SYSTEM =================
apiRouter.post("/referral/submit", authMiddleware, async (req, res) => {
    const userId = req.user.userId;
    const { code } = req.body;

    try {
        const [targetUser] = await db.query("SELECT id FROM users WHERE referral_code = ?", [code]);
        if (targetUser.length === 0) {
            return res.status(404).json({ message: "Mã giới thiệu không tồn tại" });
        }
        if (targetUser[0].id === userId) {
            return res.status(400).json({ message: "Bạn không thể nhập mã của chính mình" });
        }

        // Logic tặng quà: Ví dụ tặng voucher hoặc cộng tiền
        // Ở đây ta có thể cập nhật tiến trình nhiệm vụ INVITE cho người mời
        await db.query(`
            UPDATE user_vouchers 
            SET current_progress = current_progress + 1, updated_at = NOW()
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
            "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC",
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

// ================= ADMIN AUTH =================
apiRouter.post("/admin/login", async (req, res) => {
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

        // Kiểm tra role admin
        if (user.role !== "admin")
            return res.status(403).json({ message: "Bạn không có quyền truy cập" });

        const isMatch = await bcrypt.compare(password, user.password_hash);
        if (!isMatch)
            return res.status(401).json({ message: "Sai tài khoản hoặc mật khẩu" });

        const token = jwt.sign(
            { userId: user.id, phone: user.phone, role: "admin" },
            SECRET_KEY,
            { expiresIn: "1d" }
        );

        res.json({
            message: "Login success",
            token: token,
            user: { id: user.id, phone: user.phone, name: user.name }
        });

    } catch (err) {
        console.error("ADMIN_LOGIN_ERROR:", err);
        res.status(500).json({ message: "SERVER_ERROR" });
    }
});

function adminAuthMiddleware(req, res, next) {
    const authHeader = req.headers["authorization"];

    if (!authHeader || !authHeader.startsWith("Bearer ")) {
        return res.status(401).json({ message: "UNAUTHORIZED" });
    }

    const token = authHeader.split(" ")[1];

    try {
        const decoded = jwt.verify(token, SECRET_KEY);

        if (decoded.role !== "admin") {
            return res.status(403).json({ message: "FORBIDDEN" });
        }

        req.user = decoded;
        next();
    } catch (err) {
        return res.status(401).json({ message: "INVALID_TOKEN" });
    }
}

// ================= ADMIN API =================
apiRouter.get("/admin/stats/rentals", adminAuthMiddleware, async (req, res) => {
    try {
        const [rows] = await db.query(
            `SELECT DATE(start_time) as date, COUNT(*) as count, SUM(total_price) as revenue
             FROM rental
             WHERE start_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)
             GROUP BY DATE(start_time)
             ORDER BY date ASC`
        );
        res.json(rows);
    } catch (err) {
        console.error("ADMIN_RENTAL_STATS_ERROR:", err);
        res.status(500).json({ message: err.message });
    }
});

apiRouter.get("/admin/stats/rental-status", adminAuthMiddleware, async (req, res) => {
    try {
        const [rows] = await db.query(
            `SELECT status, COUNT(*) as count FROM rental GROUP BY status`
        );
        res.json(rows);
    } catch (err) {
        console.error("ADMIN_RENTAL_STATUS_ERROR:", err);
        res.status(500).json({ message: err.message });
    }
});

apiRouter.get("/admin/stats/users", adminAuthMiddleware, async (req, res) => {
    try {
        const [rows] = await db.query(
            `SELECT DATE(created_at) as date, COUNT(*) as count
             FROM users
             WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
             GROUP BY DATE(created_at)
             ORDER BY date ASC`
        );
        res.json(rows);
    } catch (err) {
        console.error("ADMIN_USERS_STATS_ERROR:", err);
        res.status(500).json({ message: err.message });
    }
});

apiRouter.get("/admin/users", adminAuthMiddleware, async (req, res) => {
    try {
        const [rows] = await db.query("SELECT id, phone, name, cccd, address, gender, birthday, created_at FROM users ORDER BY id DESC");
        res.json(rows);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

apiRouter.get("/admin/users/:id/rentals", adminAuthMiddleware, async (req, res) => {
    const userId = req.params.id;
    try {
        const [rows] = await db.query(
            `SELECT r.id, r.vehicle_id, v.plate AS vehicle_plate, v.type AS vehicle_type,
                    r.start_time, r.end_time, r.total_distance, r.total_price, r.status, r.payment_status
             FROM rental r
             JOIN vehicle v ON r.vehicle_id = v.id
             WHERE r.user_id = ?
             ORDER BY r.start_time DESC`,
            [userId]
        );
        res.json(rows);
    } catch (err) {
        console.error("ADMIN_USER_RENTAL_HISTORY_ERROR:", err);
        res.status(500).json({ message: err.message });
    }
});

apiRouter.delete("/admin/users/:id", adminAuthMiddleware, async (req, res) => {
    const userId = req.params.id;
    const conn = await db.getConnection();
    try {
        await conn.beginTransaction();

        // Xóa các bảng liên quan trước (cascade)
        await conn.query("DELETE FROM notifications WHERE user_id=?", [userId]);
        await conn.query("DELETE FROM user_memberships WHERE user_id=?", [userId]);
        await conn.query("DELETE FROM wallet_transactions WHERE wallet_id IN (SELECT id FROM wallet WHERE user_id=?)", [userId]);
        await conn.query("DELETE FROM wallet WHERE user_id=?", [userId]);
        await conn.query("DELETE FROM payment_transactions WHERE payment_id IN (SELECT id FROM payments WHERE user_id=?)", [userId]);
        await conn.query("DELETE FROM payments WHERE user_id=?", [userId]);
        await conn.query("DELETE FROM rental_tracking WHERE rental_id IN (SELECT id FROM rental WHERE user_id=?)", [userId]);
        await conn.query("DELETE FROM rental WHERE user_id=?", [userId]);

        // Cuối cùng xóa user
        await conn.query("DELETE FROM users WHERE id=?", [userId]);

        await conn.commit();
        res.json({ message: "SUCCESS" });
    } catch (err) {
        await conn.rollback();
        console.error("ADMIN_DELETE_USER_ERROR:", err);
        res.status(500).json({ message: err.message });
    } finally {
        conn.release();
    }
});

// API Admin chỉnh sửa thông tin một User bất kỳ qua ID
apiRouter.put("/admin/users/:id", async (req, res) => {
    const userId = req.params.id;
    const { name, phone, cccd, address, gender, birthday } = req.body;

    try {
        const [result] = await db.query(
            `UPDATE users 
             SET name=?, phone=?, cccd=?, address=?, gender=?, birthday=? 
             WHERE id=?`,
            [name, phone, cccd, address, gender, birthday, userId]
        );

        if (result.affectedRows === 0) {
            return res.status(404).json({ message: "Không tìm thấy người dùng" });
        }

        res.json({ message: "SUCCESS" });
    } catch (err) {
        console.error("ADMIN_UPDATE_USER_ERROR:", err);
        res.status(500).json({ message: "SERVER_ERROR", detail: err.message });
    }
});

// API Admin chỉnh sửa thông tin Xe qua ID
// API Admin chỉnh sửa thông tin Xe qua ID
apiRouter.put("/admin/vehicles/:id", async (req, res) => {
    const vehicleId = req.params.id;
    
    const plate = req.body.plate || "";
    const pin = req.body.pin !== undefined ? req.body.pin : 100;
    
    // Kiểm tra loại xe: Nếu Android gửi lên là "motor" thì lấy "motor", ngược lại tất cả đều là "bike"
    const type = (req.body.type === "motor") ? "motor" : "bike";
    
    const current_status = req.body.current_status || "available";
    
    // Nếu station_id truyền lên bị thiếu hoặc trống, ép nó về giá trị JS null để MySQL hiểu là NULL
    const station_id = (req.body.station_id === undefined || req.body.station_id === null || req.body.station_id === "") 
        ? null 
        : req.body.station_id;

    try {
        const [result] = await db.query(
            `UPDATE vehicle 
             SET plate=?, pin=?, type=?, current_status=?, station_id=? 
             WHERE id=?`,
            [plate, pin, type, current_status, station_id, vehicleId]
        );

        if (result.affectedRows === 0) {
            return res.status(404).json({ message: "Không tìm thấy xe" });
        }

        res.json({ message: "SUCCESS" });
    } catch (err) {
        console.error("ADMIN_UPDATE_VEHICLE_ERROR:", err);
        res.status(500).json({ message: "SERVER_ERROR", detail: err.message });
    }
});

// API Admin thêm xe mới
apiRouter.post("/admin/vehicles", async (req, res) => {
    const plate = req.body.plate || "";
    const pin = req.body.pin !== undefined ? req.body.pin : 100;
    
    // Kiểm tra loại xe: Nếu Android gửi lên là "motor" thì lấy "motor", ngược lại tất cả đều là "bike"
    const type = (req.body.type === "motor") ? "motor" : "bike";
    
    const current_status = req.body.current_status || "available";
    const station_id = (req.body.station_id === undefined || req.body.station_id === null || req.body.station_id === "") 
        ? null 
        : req.body.station_id;

    try {
        const [result] = await db.query(
            `INSERT INTO vehicle (plate, pin, type, current_status, station_id) VALUES (?, ?, ?, ?, ?)`,
            [plate, pin, type, current_status, station_id]
        );
        res.json({ message: "SUCCESS", insertId: result.insertId });
    } catch (err) {
        console.error("ADMIN_ADD_VEHICLE_ERROR:", err);
        res.status(500).json({ message: "SERVER_ERROR" });
    }
});

// API Admin xóa xe (Sử dụng Xóa mềm để không bị lỗi khóa ngoại lịch sử thuê)
apiRouter.delete("/admin/vehicles/:id", async (req, res) => {
    const vehicleId = req.params.id;

    try {
        // Cập nhật trạng thái xe thành 'deleted' thay vì xóa hẳn dòng trong DB
        const [result] = await db.query(
            "UPDATE vehicle SET current_status = 'deleted' WHERE id = ?",
            [vehicleId]
        );

        if (result.affectedRows === 0) {
            return res.status(404).json({ message: "Không tìm thấy xe cần xóa" });
        }

        res.json({ message: "SUCCESS" });
    } catch (err) {
        console.error("ADMIN_DELETE_VEHICLE_ERROR:", err);
        res.status(500).json({ message: "SERVER_ERROR", detail: err.message });
    }
});

apiRouter.get("/admin/vouchers", adminAuthMiddleware, async (req, res) => {
    try {
        const [rows] = await db.query(`
            SELECT id, type, status, icon_name AS icon, title_display AS title, title_key,
                   discount_text AS discount, price, expiry_text AS expiry, 
                   default_action_key AS action, default_btn_type AS btn_type,
                   has_progress, max_progress AS prog_max, created_at
            FROM vouchers 
            ORDER BY id DESC
        `);
        res.header("X-Debug-Version", "1.0.1");
        res.json(rows);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

apiRouter.post("/admin/vouchers", adminAuthMiddleware, async (req, res) => {
    console.log("DEBUG_ADMIN_POST_VOUCHER_BODY:", req.body);
    const {
        type, icon,
        display_title, // Khớp với name="display_title" trong React
        title,         // Khớp với name="title" (Mã Key) trong React
        discount, price, expiry, action, btn_type, has_progress, prog_max
    } = req.body;

    try {
        await db.query(
            `INSERT INTO vouchers (type, icon_name, title_display, title_key, discount_text, price, expiry_text, default_action_key, default_btn_type, has_progress, max_progress, status)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
            [
                type,
                icon || 'ic_wallet',
                display_title,
                title,
                discount || '',
                price || 0,
                expiry || '',
                action || 'CLAIM',
                btn_type || 'GREEN',
                has_progress ? 1 : 0,
                prog_max || 0,
                'ACTIVE' // Mặc định cho status mới
            ]
        );
        res.json({ message: "SUCCESS" });
    } catch (err) {
        console.error("ADMIN_VOUCHER_POST_ERROR:", err);
        res.status(500).json({ message: err.message });
    }
});

apiRouter.put("/admin/vouchers/:id", adminAuthMiddleware, async (req, res) => {
    const { id } = req.params;
    console.log("DEBUG_ADMIN_PUT_VOUCHER_BODY:", req.body);
    const {
        type, icon,
        display_title,
        title,
        discount, price, expiry, action, btn_type, has_progress, prog_max
    } = req.body;

    try {
        await db.query(
            `UPDATE vouchers 
             SET type=?, icon_name=?, title_display=?, title_key=?, discount_text=?, price=?, expiry_text=?, default_action_key=?, default_btn_type=?, has_progress=?, max_progress=?, status=?
             WHERE id=?`,
            [
                type,
                icon || 'ic_wallet',
                display_title,
                title,
                discount,
                price || 0,
                expiry,
                action,
                btn_type,
                has_progress ? 1 : 0,
                prog_max,
                req.body.status || 'ACTIVE',
                id
            ]
        );
        res.json({ message: "SUCCESS" });
    } catch (err) {
        console.error("ADMIN_VOUCHER_PUT_ERROR:", err);
        res.status(500).json({ message: err.message });
    }
});

apiRouter.delete("/admin/vouchers/:id", adminAuthMiddleware, async (req, res) => {
    const { id } = req.params;
    try {
        // Thay vì xóa cứng, ta có thể ẩn hoặc xóa thật. Ở đây tôi xóa thật để demo
        await db.query("DELETE FROM vouchers WHERE id=?", [id]);
        res.json({ message: "SUCCESS" });
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

apiRouter.get("/admin/voucher-actions", adminAuthMiddleware, async (req, res) => {
    try {
        const [rows] = await db.query("SELECT * FROM voucher_actions ORDER BY id ASC");
        res.json(rows);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

apiRouter.post("/admin/voucher-actions", adminAuthMiddleware, async (req, res) => {
    const { action_key, label } = req.body;
    try {
        await db.query(
            "INSERT IGNORE INTO voucher_actions (action_key, label) VALUES (?, ?)",
            [action_key, label]
        );
        res.json({ message: "SUCCESS" });
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

apiRouter.delete("/admin/voucher-actions/:id", adminAuthMiddleware, async (req, res) => {
    try {
        await db.query("DELETE FROM voucher_actions WHERE id = ?", [req.params.id]);
        res.json({ message: "SUCCESS" });
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

// ================= ADMIN: VEHICLES, STATIONS, PRICING =================
apiRouter.get("/admin/rentals/active", adminAuthMiddleware, async (req, res) => {
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
        console.error("ADMIN_ACTIVE_RENTALS_ERROR:", err);
        res.status(500).json({ message: err.message });
    }
});

apiRouter.get("/admin/vehicles", adminAuthMiddleware, async (req, res) => {
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
        console.error("ADMIN_VEHICLES_ERROR:", err);
        res.status(500).json({ message: err.message });
    }
});

apiRouter.get("/admin/stations", adminAuthMiddleware, async (req, res) => {
    try {
        const [rows] = await db.query("SELECT * FROM stations ORDER BY id DESC");
        res.json(rows);
    } catch (err) {
        console.error("ADMIN_STATIONS_ERROR:", err);
        res.status(500).json({ message: err.message });
    }
});

apiRouter.post("/admin/pricing", adminAuthMiddleware, async (req, res) => {
    const { unlock_fee, price_per_minute, price_per_km, min_wallet_to_rent, low_balance_warning } = req.body;
    try {
        await db.query(
            `INSERT INTO pricing (unlock_fee, price_per_minute, price_per_km, min_wallet_to_rent, low_balance_warning, created_at)
             VALUES (?, ?, ?, ?, ?, NOW())`,
            [unlock_fee || 0, price_per_minute || 0, price_per_km || 0, min_wallet_to_rent || 0, low_balance_warning || 0]
        );
        res.json({ message: "SUCCESS" });
    } catch (err) {
        console.error("ADMIN_PRICING_ERROR:", err);
        res.status(500).json({ message: err.message });
    }
});

apiRouter.get("/admin/pricing", adminAuthMiddleware, async (req, res) => {
    try {
        const [rows] = await db.query("SELECT * FROM pricing ORDER BY id DESC LIMIT 1");
        if (rows.length === 0) return res.json({});
        res.json(rows[0]);
    } catch (err) {
        console.error("ADMIN_PRICING_GET_ERROR:", err);
        res.status(500).json({ message: err.message });
    }
});





// Danh gia chuyen di REVIEW
// apiRouter.post("/reviews", authMiddleware, async (req, res) => {
//     const userId = req.user.userId;
//     const {
//         rental_id,
//         rating,
//         comment
//     } = req.body;

//     if (!rental_id || !rating) {
//         return res.status(400).json({
//             message: "INVALID_INPUT"
//         });
//     }

//     try {
//         // kiểm tra chuyến đi thuộc user
//         const [rides] = await db.query(
//             `SELECT id FROM rental WHERE id=? AND user_id=? AND status='done'`,
//             [
//                 rental_id,
//                 userId
//             ]
//         );


//         if (rides.length === 0) {

//             return res.status(403).json({
//                 message: "NOT_YOUR_RIDE"
//             });

//         }

//         // thêm review
//         await db.query(
//             `INSERT INTO reviews
//             (
//                 rental_id,
//                 user_id,
//                 rating,
//                 comment
//             )
//             VALUES (?,?,?,?)
//             `,
//             [
//                 rental_id,
//                 userId,
//                 rating,
//                 comment
//             ]
//         );


//         res.json({
//             message: "REVIEW_SUCCESS"
//         });

//     } catch (err) {

//         console.log(err);

//         if (err.code === "ER_DUP_ENTRY") {
//             return res.status(400).json({
//                 message: "ALREADY_REVIEWED"
//             });
//         }

//         res.status(500).json({
//             message: "SERVER_ERROR"
//         });

//     }
// });
// Danh gia chuyen di REVIEW
apiRouter.post("/reviews", authMiddleware, async (req, res) => {
    const userId = req.user.userId;
    const { rental_id, rating, comment } = req.body;

    if (!rental_id || !rating) {
        return res.status(400).json({ message: "INVALID_INPUT" });
    }

    try {
        // Kiểm tra chuyến đi thuộc user và đã hoàn thành
        const [rides] = await db.query(
            `SELECT id FROM rental WHERE id=? AND user_id=? AND status='done'`,
            [rental_id, userId]
        );

        if (rides.length === 0) {
            return res.status(403).json({ message: "NOT_YOUR_RIDE" });
        }

        // 1. Thêm vào bảng reviews
        await db.query(
            `INSERT INTO reviews (rental_id, user_id, rating, comment) VALUES (?,?,?,?)`,
            [rental_id, userId, rating, comment || ""]
        );

        // 2. TỰ ĐỘNG ĐẨY LÊN CỘNG ĐỒNG NẾU ĐÁNH GIÁ 5 SAO
        if (Number(rating) === 5) {
            const autoTitle = "Đã hoàn thành một chuyến đi tuyệt vời!";
            const autoContent = comment ? `"${comment}"` : "Đã đánh giá 5 sao cho chuyến xe này trên QRIDE! 🌟";

            await db.query(
                `INSERT INTO trip_posts (user_id, rental_id, title, content, location) VALUES (?, ?, ?, ?, ?)`,
                [userId, rental_id, autoTitle, autoContent, "Chuyến đi QRIDE"]
            );
        }

        res.json({ message: "REVIEW_SUCCESS" });

    } catch (err) {
        console.log(err);
        if (err.code === "ER_DUP_ENTRY") {
            return res.status(400).json({ message: "ALREADY_REVIEWED" });
        }
        res.status(500).json({ message: "SERVER_ERROR" });
    }
});

// API lay danh gia cua chuyen di
apiRouter.get("/reviews/:rentalId", authMiddleware, async (req, res) => {
    const rentalId = req.params.rentalId;
    const [rows] = await db.query(
        `SELECT reviews.*, users.name, users.avatar FROM reviews JOIN users ON reviews.user_id=users.id WHERE rental_id=?`,
        [rentalId]
    );

    res.json(rows);
});

// API tao bai viet
apiRouter.post("/community/posts", authMiddleware, upload.single("image"), async (req, res) => {
    try {
        const userId = req.user.userId;
        const { rental_id, title, content, location, image_url } = req.body;

        let imageUrl = null;

        // 1. Nếu client up file qua định dạng form-data
        if (req.file) {
            imageUrl = "/uploads/" + req.file.filename;
        }
        // 2. Nếu client gửi chuỗi Base64 (Cả loại có header lẫn loại thô)
        else if (image_url && (image_url.startsWith("data:image") || image_url.length > 500)) {
            try {
                let base64Data = image_url;
                let ext = "jpg"; // Định dạng mặc định nếu là chuỗi thô

                // Nếu chuỗi có chứa header chuẩn của Base64
                if (image_url.startsWith("data:image")) {
                    const matches = image_url.match(/^data:image\/([A-Za-z-+\/]+);base64,(.+)$/);
                    if (matches) {
                        ext = matches[1]; // png, jpeg, webp...
                        base64Data = matches[2];
                    }
                }

                // Tiến hành chuyển đổi chuỗi chữ thành file ảnh vật lý
                const buffer = Buffer.from(base64Data, 'base64');
                const filename = `${Date.now()}-base64.${ext}`;
                const uploadPath = path.join(__dirname, "public/uploads", filename);

                fs.writeFileSync(uploadPath, buffer);
                imageUrl = "/uploads/" + filename;
            } catch (base64Err) {
                console.error("Lỗi chuyển đổi Base64:", base64Err);
                imageUrl = image_url; // Nếu lỗi nặng quá thì giữ lại chuỗi gốc
            }
        }
        // 3. Trường hợp là một đường link URL thông thường (ngắn)
        else if (image_url) {
            imageUrl = image_url;
        }

        // Thực hiện ghi vào database
        const [result] = await db.query(
            `INSERT INTO trip_posts(user_id, rental_id, title, content, location, image_url)
             VALUES (?, ?, ?, ?, ?, ?)`,
            [userId, rental_id || null, title, content, location, imageUrl]
        );

        res.json({ message: "SUCCESS", postId: result.insertId });
    } catch (err) {
        console.error("POST_TRIP_ERROR:", err);
        res.status(500).json({ message: "SERVER_ERROR", detail: err.message });
    }
});


// API lay danh sach bai viet
apiRouter.get("/community/feed", authMiddleware, async (req, res) => {
    const [rows] = await db.query(`SELECT p.id, p.title, p.content, p.location, p.image_url,p.created_at,u.name,u.avatar,
    COUNT(DISTINCT l.id) likes,
    COUNT(DISTINCT c.id) comments

    FROM trip_posts p
    JOIN users u
    ON p.user_id=u.id

    LEFT JOIN post_likes l
    ON p.id=l.post_id

    LEFT JOIN post_comments c
    ON p.id=c.post_id

    GROUP BY p.id
    ORDER BY p.created_at DESC`
    );
    res.json(rows);
});

// API like bai viet
apiRouter.post("/community/like", authMiddleware, async (req, res) => {
    const userId = req.user.userId;
    const { postId } = req.body;
    const [check] = await db.query(
        `SELECT id FROM post_likes WHERE post_id=? AND user_id=?`,
        [
            postId,
            userId
        ]
    );
    if (check.length > 0) {
        await db.query(
            `DELETE FROM post_likes WHERE post_id=? AND user_id=?`,
            [
                postId,
                userId
            ]
        );

        return res.json({
            liked: false
        });

    }

    await db.query(
        `INSERT INTO post_likes (post_id,user_id) VALUES(?,?)`,
        [
            postId,
            userId
        ]
    );

    res.json({
        liked: true
    });
});

// API upload anh
apiRouter.post("/community/upload", authMiddleware, upload.single("image"), (req, res) => {
    if (!req.file) {
        return res.status(400).json({
            message: "NO_IMAGE"
        });
    }

    res.json({
        url: "/uploads/" + req.file.filename
    });
});

// API Comment bai viet
apiRouter.post("/community/comment", authMiddleware, async (req, res) => {
    try {
        const userId = req.user.userId;
        await db.query(
            `INSERT INTO post_comments (post_id,user_id,content)VALUES(?,?,?)`,
            [
                req.body.postId,
                userId,
                req.body.content
            ]
        );
        res.json({
            message: "COMMENT_ADDED"
        });
    } catch (e) {
        res.status(500).json({
            message: e.message
        });
    }


});

// API lay comment bai viet
apiRouter.get("/community/comments/:postId", authMiddleware, async (req, res) => {
        const [rows] = await db.query(
            `SELECT c.*, u.name, u.avatar FROM post_comments c JOIN users u ON c.user_id=u.id WHERE post_id=? ORDER BY created_at DESC `,
            [
                req.params.postId
            ]
        );
        res.json(rows);
    });



const PORT = process.env.PORT || 3000;
app.listen(PORT, "0.0.0.0", () => {
    console.log(`Server running: http://localhost:${PORT}`);
});
