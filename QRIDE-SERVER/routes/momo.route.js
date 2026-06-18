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
            console.log(`[MOMO_IPN] Payment failed for orderId=${orderId}, resultCode=${resultCode}`);
            return res.json({ message: "FAILED" });
        }

        const conn = await db.getConnection();

        try {
            await conn.beginTransaction();

            // SQL Server: bỏ "FOR UPDATE" (không hỗ trợ), dùng WITH (UPDLOCK) nếu cần
            const [payments] = await conn.query(
                "SELECT * FROM payments WHERE external_ref=?",
                [orderId]
            );

            if (payments.length === 0) throw new Error("PAYMENT_NOT_FOUND");

            const payment = payments[0];

            if (payment.status === "success") {
                await conn.commit();
                return res.json({ message: "ALREADY_DONE" });
            }

            if (parseInt(amount) !== parseInt(payment.amount)) {
                throw new Error("AMOUNT_MISMATCH");
            }

            await conn.query(
                "UPDATE payments SET status='success' WHERE id=?",
                [payment.id]
            );

            if (payment.payment_type === 'buy_vip') {
                // ===== XỬ LÝ MUA GÓI VIP QUA MOMO =====
                console.log(`[MOMO_IPN] Activating VIP for user=${payment.user_id}, voucher=${payment.target_id}`);

                // Lấy thông tin voucher để biết duration_days
                const [voucherRows] = await conn.query(
                    "SELECT * FROM vouchers WHERE id=?",
                    [payment.target_id]
                );
                const duration = voucherRows.length > 0 ? (voucherRows[0].duration_days || 30) : 30;

                // Upsert user_vouchers (INSERT if not exists, UPDATE if exists)
                const [uvRows] = await conn.query(
                    "SELECT * FROM user_vouchers WHERE user_id=? AND voucher_id=?",
                    [payment.user_id, payment.target_id]
                );

                if (uvRows.length > 0) {
                    await conn.query(
                        "UPDATE user_vouchers SET status='ACTIVE', action_key='USING', btn_type='ORANGE', expiry_date=DATE_ADD(NOW(), INTERVAL ? DAY), updated_at=NOW() WHERE user_id=? AND voucher_id=?",
                        [duration, payment.user_id, payment.target_id]
                    );
                } else {
                    await conn.query(
                        "INSERT INTO user_vouchers (user_id, voucher_id, status, action_key, current_progress, btn_type, expiry_date, updated_at) VALUES (?, ?, 'ACTIVE', 'USING', 0, 'ORANGE', DATE_ADD(NOW(), INTERVAL ? DAY), NOW())",
                        [payment.user_id, payment.target_id, duration]
                    );
                }

                // Ghi lịch sử membership (bỏ qua lỗi nếu bảng chưa tồn tại)
                try {
                    await conn.query(
                        "INSERT INTO user_memberships (user_id, voucher_id, start_date, end_date) VALUES (?, ?, NOW(), DATE_ADD(NOW(), INTERVAL ? DAY))",
                        [payment.user_id, payment.target_id, duration]
                    );
                } catch (e) {
                    console.warn("[MOMO_IPN] user_memberships insert skipped:", e.message);
                }

                console.log(`[MOMO_IPN] VIP activated successfully for user=${payment.user_id}`);

            } else {
                // ===== MẶC ĐỊNH: NẠP TIỀN VÀO VÍ =====
                const [walletRows] = await conn.query(
                    "SELECT * FROM wallet WHERE user_id=?",
                    [payment.user_id]
                );

                if (walletRows.length === 0) throw new Error("WALLET_NOT_FOUND");

                const wallet = walletRows[0];
                const newBalance = wallet.balance + parseInt(payment.amount);

                await conn.query(
                    "UPDATE wallet SET balance=? WHERE id=?",
                    [newBalance, wallet.id]
                );

                await conn.query(
                    `INSERT INTO wallet_transactions(wallet_id, payment_id, amount, type, balance_before, balance_after, description, created_at)
                     VALUES (?, ?, ?, 'topup', ?, ?, ?, NOW())`,
                    [wallet.id, payment.id, payment.amount, wallet.balance, newBalance, "Nạp tiền qua MoMo"]
                );

                console.log(`[MOMO_IPN] Wallet topup success for user=${payment.user_id}, +${payment.amount}`);
            }

            await conn.commit();
            res.json({ message: "SUCCESS" });

        } catch (err) {
            await conn.rollback();
            console.error("[MOMO_IPN] ERROR:", err.message || err);
            res.status(500).json({ message: err.message || err.toString() });
        } finally {
            conn.release();
        }
    });

    return router;
};