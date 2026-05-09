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

async function prepareTest() {
    try {
        let pool = await sql.connect(dbConfig);
        
        console.log("--- Đang chuẩn bị dữ liệu Test VIP (Version 2) ---");

        // Cập nhật các gói GOI_HOI_VIEN
        // Sửa tên cột: default_action_key và default_btn_type
        await pool.request().query(`
            UPDATE vouchers 
            SET default_action_key = 'REGISTER_VIP', 
                default_btn_type = 'GREEN',
                status = 'ACTIVE'
            WHERE type = 'GOI_HOI_VIEN'
        `);

        // Xóa các bản ghi user_vouchers cũ của các gói VIP để nó hiện nút "Đăng ký" (REGISTER_VIP)
        // Thay vì hiện "Đang sử dụng" (USING)
        await pool.request().query(`
            DELETE FROM user_vouchers 
            WHERE voucher_id IN (SELECT id FROM vouchers WHERE type = 'GOI_HOI_VIEN')
        `);

        console.log("THÀNH CÔNG: Các gói Hội viên đã được đặt về trạng thái chờ Đăng ký.");
        console.log("Bây giờ bạn có thể mở App để test thanh toán MoMo.");
        await pool.close();
    } catch (err) {
        console.error("LỖI:", err);
    }
}

prepareTest();
