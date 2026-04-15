package com.example.qride.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.qride.R;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "qride.db";
    private static final int DATABASE_VERSION = 14;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "phone TEXT UNIQUE," +
                "password TEXT," +
                "name TEXT," +
                "cccd TEXT UNIQUE," +
                "address TEXT," +
                "gender TEXT," +
                "birthday TEXT)");

        db.execSQL("CREATE TABLE vouchers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "[type] TEXT," +
                "icon INTEGER," +
                "title TEXT," +
                "discount TEXT," +
                "expiry TEXT," +
                "[action] TEXT," +
                "btn_type TEXT," +
                "has_progress INTEGER," +
                "prog_curr INTEGER," +
                "prog_max INTEGER)");

        // Bảng mới: Notifications
        db.execSQL("CREATE TABLE notifications (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT," +
                "message TEXT," +
                "timestamp TEXT," +
                "is_read INTEGER DEFAULT 0," +
                "[type] TEXT)"); // e.g., TRIP, VOUCHER, CHECKIN

        db.execSQL("CREATE TABLE vehicles (id INTEGER PRIMARY KEY AUTOINCREMENT, plate TEXT UNIQUE, [type] TEXT, status TEXT)");
        db.execSQL("CREATE TABLE bookings (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, vehicle_id INTEGER, start_time TEXT, end_time TEXT, total_price REAL, status TEXT)");
        db.execSQL("CREATE TABLE payments (id INTEGER PRIMARY KEY AUTOINCREMENT, booking_id INTEGER, amount REAL, method TEXT, status TEXT)");

        insertSampleData(db);
    }

    private void insertSampleData(SQLiteDatabase db) {
        for (int i = 2; i <= 10; i++) {
            String phone = "098765432" + i;
            String password = "Nguyet21@" + i;
            String name = "Người dùng " + i;
            String cccd = "00109900000" + i;
            db.execSQL("INSERT INTO users (phone, password, name, cccd, address, gender, birthday) " +
                    "VALUES ('" + phone + "', '" + password + "', '" + name + "', '" + cccd + "', 'Hà Nội', 'Nam', '01/01/2000')");
        }

        // Tab Tích quà - Sử dụng KEY từ strings.xml
        insertVoucher(db, "TICH_QUA", R.drawable.ic_wallet, "v_title_topup", "v_desc_topup", "HSD: 31/12/2025", "action_use_now", "GREEN", 0, 0, 0);
        insertVoucher(db, "TICH_QUA", R.drawable.ic_bike_station, "v_title_first_trip", "v_desc_50", "HSD: 31/12/2025", "action_use_now", "GREEN", 0, 0, 0);
        insertVoucher(db, "TICH_QUA", R.drawable.ic_membership_crown, "v_title_membership", "v_desc_30", "HSD: 31/12/2025", "action_use_now", "GREEN", 0, 0, 0);
        insertVoucher(db, "TICH_QUA", R.drawable.ic_membership, "v_title_invite", "v_desc_invite", "", "action_invite_now", "ORANGE", 1, 3, 5);
        insertVoucher(db, "TICH_QUA", R.drawable.ic_calendar, "v_title_checkin", "v_desc_3k", "", "action_checkin", "ORANGE", 1, 1, 3);
        insertVoucher(db, "TICH_QUA", R.drawable.ic_star, "v_title_rate", "v_desc_10", "", "action_perform", "ORANGE", 1, 1, 3);

        // Tab Gói hội viên - Sử dụng KEY từ strings.xml
        insertVoucher(db, "GOI_HOI_VIEN", R.drawable.ic_membership, "v_title_m_basic", "v_desc_m_basic", "HSD: 31/12/2025", "action_register", "GREEN", 0, 0, 0);
        insertVoucher(db, "GOI_HOI_VIEN", R.drawable.ic_membership, "v_title_m_std", "v_desc_m_std", "HSD: 31/12/2025", "action_register", "GREEN", 0, 0, 0);
        insertVoucher(db, "GOI_HOI_VIEN", R.drawable.ic_membership, "v_title_m_premium", "v_desc_m_premium", "HSD: 31/12/2025", "action_register", "ORANGE", 0, 0, 0);

        // Chèn thông báo mẫu
        db.execSQL("INSERT INTO notifications (title, message, timestamp, [type]) VALUES ('notif_title_welcome', 'notif_msg_welcome', '2023-10-27 10:00', 'GENERAL')");
        db.execSQL("INSERT INTO notifications (title, message, timestamp, [type]) VALUES ('notif_title_voucher', 'notif_msg_voucher', '2023-10-27 10:05', 'VOUCHER')");
    }

    private void insertVoucher(SQLiteDatabase db, String type, int icon, String title, String discount, String expiry, String action, String btnType, int hasProg, int curr, int max) {
        db.execSQL("INSERT INTO vouchers ([type], icon, title, discount, expiry, [action], btn_type, has_progress, prog_curr, prog_max) " +
                "VALUES ('" + type + "', " + icon + ", '" + title + "', '" + discount + "', '" + expiry + "', '" + action + "', '" + btnType + "', " + hasProg + ", " + curr + ", " + max + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS users");
        db.execSQL("DROP TABLE IF EXISTS vouchers");
        db.execSQL("DROP TABLE IF EXISTS vehicles");
        db.execSQL("DROP TABLE IF EXISTS bookings");
        db.execSQL("DROP TABLE IF EXISTS payments");
        db.execSQL("DROP TABLE IF EXISTS notifications");
        onCreate(db);
    }
}
