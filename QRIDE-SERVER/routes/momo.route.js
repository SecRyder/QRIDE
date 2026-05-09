const express = require("express");
const router = express.Router();
const { verifySignature } = require("../utils/momo.util");

module.exports = (db) => {

    router.post("/ipn", async (req, res) => {
        console.log("Received MoMo IPN:", req.body);

        const data = req.body;

        if (!verifySignature(
            data,
            process.env.MOMO_SECRET_KEY,
            process.env.MOMO_ACCESS_KEY
        )) {
            return res.status(400).json({ message: "INVALID_SIGNATURE" });
        }

        const { orderId, resultCode, amount } = data;

        if (resultCode != 0) {
            return res.json({ message: "FAILED" });
        }

        const conn = await db.getConnection();

        try {
            await conn.beginTransaction();

            const [payments] = await conn.query(
                "SELECT * FROM payments WHERE external_ref=? FOR UPDATE",
                [orderId]
            );

            if (payments.length === 0) throw "PAYMENT_NOT_FOUND";

            const payment = payments[0];

            if (payment.status === "success") {
                await conn.commit();
                return res.json({ message: "ALREADY_DONE" });
            }

            if (parseInt(amount) !== payment.amount) {
                throw "AMOUNT_MISMATCH";
            }

            await conn.query(
                "UPDATE payments SET status='success' WHERE id=?",
                [payment.id]
            );

            if (payment.payment_type === 'buy_vip') {
                // 1. Cập nhật trạng thái Voucher người dùng thành ACTIVE
                try {
                    await conn.query(
                        "UPDATE user_vouchers SET status = 'ACTIVE', action_key = 'USING', btn_type = 'ORANGE', updated_at = GETDATE() WHERE user_id = ? AND voucher_id = ?",
                        [payment.user_id, payment.target_id]
                    );
                    
                    // 2. Thêm vào lịch sử Membership
                    await conn.query(
                        "INSERT INTO user_memberships (user_id, voucher_id, start_date, end_date) VALUES (?, ?, GETDATE(), DATEADD(day, 30, GETDATE()))",
                        [payment.user_id, payment.target_id]
                    );
                } catch (e) {
                    console.error("Error activating VIP membership:", e);
                }
            } else {
                // Mặc định là topup
                const [walletRows] = await conn.query(
                    "SELECT * FROM wallet WHERE user_id=? FOR UPDATE",
                    [payment.user_id]
                );

                const wallet = walletRows[0];
                const newBalance = wallet.balance + payment.amount;

                await conn.query(
                    "UPDATE wallet SET balance=? WHERE id=?",
                    [newBalance, wallet.id]
                );
            }

            await conn.commit();

            res.json({ message: "SUCCESS" });

        } catch (err) {
            await conn.rollback();
            res.status(500).json({ message: err.toString() });
        } finally {
            conn.release();
        }
    });

    return router;
};