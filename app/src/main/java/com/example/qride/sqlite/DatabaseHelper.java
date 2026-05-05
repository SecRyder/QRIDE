//package com.example.qride.sqlite;
//
//import android.content.Context;
//import android.database.sqlite.SQLiteDatabase;
//import android.database.sqlite.SQLiteOpenHelper;
//
//import com.example.qride.R;
//
//public class DatabaseHelper extends SQLiteOpenHelper {
//    private static final String DATABASE_NAME = "qride.db";
//    private static final int DATABASE_VERSION = 14;
//
//    public DatabaseHelper(Context context) {
//        super(context, DATABASE_NAME, null, DATABASE_VERSION);
//    }
//
//    @Override
//    public void onCreate(SQLiteDatabase db) {
//        db.execSQL("CREATE TABLE users (" +
//                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
//                "phone TEXT UNIQUE," +
//                "password TEXT," +
//                "name TEXT," +
//                "cccd TEXT UNIQUE," +
//                "address TEXT," +
//                "gender TEXT," +
//                "birthday TEXT)");
//
//        db.execSQL("CREATE TABLE vouchers (" +
//                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
//                "[type] TEXT," +
//                "icon INTEGER," +
//                "title TEXT," +
//                "discount TEXT," +
//                "expiry TEXT," +
//                "[action] TEXT," +
//                "btn_type TEXT," +
//                "has_progress INTEGER," +
//                "prog_curr INTEGER," +
//                "prog_max INTEGER)");
//
//        // Bảng mới: Notifications
//        db.execSQL("CREATE TABLE notifications (" +
//                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
//                "title TEXT," +
//                "message TEXT," +
//                "timestamp TEXT," +
//                "is_read INTEGER DEFAULT 0," +
//                "[type] TEXT)"); // e.g., TRIP, VOUCHER, CHECKIN
//
//        db.execSQL("CREATE TABLE vehicles (id INTEGER PRIMARY KEY AUTOINCREMENT, plate TEXT UNIQUE, [type] TEXT, status TEXT)");
//        db.execSQL("CREATE TABLE bookings (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, vehicle_id INTEGER, start_time TEXT, end_time TEXT, total_price REAL, status TEXT)");
//        db.execSQL("CREATE TABLE payments (id INTEGER PRIMARY KEY AUTOINCREMENT, booking_id INTEGER, amount REAL, method TEXT, status TEXT)");
//
//        insertSampleData(db);
//    }
//
//    private void insertSampleData(SQLiteDatabase db) {
//        for (int i = 2; i <= 10; i++) {
//            String phone = "098765432" + i;
//            String password = "Nguyet21@" + i;
//            String name = "Người dùng " + i;
//            String cccd = "00109900000" + i;
//            db.execSQL("INSERT INTO users (phone, password, name, cccd, address, gender, birthday) " +
//                    "VALUES ('" + phone + "', '" + password + "', '" + name + "', '" + cccd + "', 'Hà Nội', 'Nam', '01/01/2000')");
//        }
//
//        // Tab Tích quà - Sử dụng KEY từ strings.xml
//        insertVoucher(db, "TICH_QUA", R.drawable.ic_wallet, "v_title_topup", "v_desc_topup", "HSD: 31/12/2025", "action_use_now", "GREEN", 0, 0, 0);
//        insertVoucher(db, "TICH_QUA", R.drawable.ic_bike_station, "v_title_first_trip", "v_desc_50", "HSD: 31/12/2025", "action_use_now", "GREEN", 0, 0, 0);
//        insertVoucher(db, "TICH_QUA", R.drawable.ic_membership_crown, "v_title_membership", "v_desc_30", "HSD: 31/12/2025", "action_use_now", "GREEN", 0, 0, 0);
//        insertVoucher(db, "TICH_QUA", R.drawable.ic_membership, "v_title_invite", "v_desc_invite", "", "action_invite_now", "ORANGE", 1, 3, 5);
//        insertVoucher(db, "TICH_QUA", R.drawable.ic_calendar, "v_title_checkin", "v_desc_3k", "", "action_checkin", "ORANGE", 1, 1, 3);
//        insertVoucher(db, "TICH_QUA", R.drawable.ic_star, "v_title_rate", "v_desc_10", "", "action_perform", "ORANGE", 1, 1, 3);
//
//        // Tab Gói hội viên - Sử dụng KEY từ strings.xml
//        insertVoucher(db, "GOI_HOI_VIEN", R.drawable.ic_membership, "v_title_m_basic", "v_desc_m_basic", "HSD: 31/12/2025", "action_register", "GREEN", 0, 0, 0);
//        insertVoucher(db, "GOI_HOI_VIEN", R.drawable.ic_membership, "v_title_m_std", "v_desc_m_std", "HSD: 31/12/2025", "action_register", "GREEN", 0, 0, 0);
//        insertVoucher(db, "GOI_HOI_VIEN", R.drawable.ic_membership, "v_title_m_premium", "v_desc_m_premium", "HSD: 31/12/2025", "action_register", "ORANGE", 0, 0, 0);
//
//        // Chèn thông báo mẫu
//        db.execSQL("INSERT INTO notifications (title, message, timestamp, [type]) VALUES ('notif_title_welcome', 'notif_msg_welcome', '2023-10-27 10:00', 'GENERAL')");
//        db.execSQL("INSERT INTO notifications (title, message, timestamp, [type]) VALUES ('notif_title_voucher', 'notif_msg_voucher', '2023-10-27 10:05', 'VOUCHER')");
//    }
//
//    private void insertVoucher(SQLiteDatabase db, String type, int icon, String title, String discount, String expiry, String action, String btnType, int hasProg, int curr, int max) {
//        db.execSQL("INSERT INTO vouchers ([type], icon, title, discount, expiry, [action], btn_type, has_progress, prog_curr, prog_max) " +
//                "VALUES ('" + type + "', " + icon + ", '" + title + "', '" + discount + "', '" + expiry + "', '" + action + "', '" + btnType + "', " + hasProg + ", " + curr + ", " + max + ")");
//    }
//
//    @Override
//    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        db.execSQL("DROP TABLE IF EXISTS users");
//        db.execSQL("DROP TABLE IF EXISTS vouchers");
//        db.execSQL("DROP TABLE IF EXISTS vehicles");
//        db.execSQL("DROP TABLE IF EXISTS bookings");
//        db.execSQL("DROP TABLE IF EXISTS payments");
//        db.execSQL("DROP TABLE IF EXISTS notifications");
//        onCreate(db);
//    }
//}

package com.example.qride.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.qride.R;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "qride.db";
    private static final int DATABASE_VERSION = 18;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // ================== USER SESSION ==================
        db.execSQL("CREATE TABLE user_session (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER UNIQUE," +
                "phone TEXT," +
                "token TEXT," +
                "login_time INTEGER)");

        // ================== STATIONS CACHE ==================
        db.execSQL("CREATE TABLE stations_cache (" +
                "id INTEGER PRIMARY KEY," +
                "name TEXT," +
                "address TEXT," +
                "lat REAL," +
                "lng REAL," +
                "last_updated INTEGER)");

        // ================== RENTAL LOCAL ==================
        db.execSQL("CREATE TABLE rental_local (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "server_id INTEGER," +
                "vehicle_id INTEGER," +
                "start_time TEXT," +
                "end_time TEXT," +
                "status TEXT," +
                "synced INTEGER DEFAULT 0)");

        // ================== VEHICLES CACHE ==================
        db.execSQL("CREATE TABLE vehicles_cache (" +
                "id INTEGER PRIMARY KEY," +
                "plate TEXT NOT NULL," +
                "lat REAL," +
                "lng REAL," +
                "status TEXT NOT NULL," +
                "pin INTEGER," +
                "station_id INTEGER," +
                "last_updated INTEGER)");

        db.execSQL("CREATE INDEX idx_vehicle_status ON vehicles_cache(status)");
        db.execSQL("CREATE INDEX idx_vehicle_station ON vehicles_cache(station_id)");
        db.execSQL("CREATE INDEX idx_rental_status ON rental_local(status)");
        db.execSQL("CREATE INDEX idx_session_user ON user_session(user_id)");



        // ================== TRACKING LOCAL ==================
        db.execSQL("CREATE TABLE tracking_local (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "rental_id INTEGER," +
                "lat REAL," +
                "lng REAL," +
                "created_at INTEGER," +
                "synced INTEGER DEFAULT 0)");

        db.execSQL("CREATE INDEX idx_tracking_rental ON tracking_local(rental_id)");

        // ================== NOTIFICATIONS ==================
        db.execSQL("CREATE TABLE notifications (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT," +
                "message TEXT," +
                "timestamp INTEGER," +
                "is_read INTEGER DEFAULT 0," +
                "type TEXT)");

        // ================== VOUCHERS ==================
        db.execSQL("CREATE TABLE vouchers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "type TEXT," +
                "icon INTEGER," +
                "title TEXT," +
                "discount TEXT," +
                "expiry TEXT," +
                "[action] TEXT," +
                "btn_type TEXT," +
                "has_progress INTEGER," +
                "prog_curr INTEGER," +
                "prog_max INTEGER)");

        // ================== SETTINGS ==================
        db.execSQL("CREATE TABLE settings (" +
                "[key] TEXT PRIMARY KEY," +
                "value TEXT)");

        insertSampleData(db);
    }

    // ================== SAMPLE DATA ==================
    private void insertSampleData(SQLiteDatabase db) {

        // ===== VOUCHERS =====
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

        // ===== NOTIFICATION SAMPLE =====
        db.execSQL("INSERT INTO notifications (title, message, timestamp, type) VALUES ('welcome', 'Chào mừng bạn!', strftime('%s','now'), 'GENERAL')");
    }

    private void insertVoucher(SQLiteDatabase db, String type, int icon, String title,
                               String discount, String expiry, String action,
                               String btnType, int hasProg, int curr, int max) {

        db.execSQL("INSERT INTO vouchers (type, icon, title, discount, expiry, [action], btn_type, has_progress, prog_curr, prog_max) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[]{type, icon, title, discount, expiry, action, btnType, hasProg, curr, max});
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS user_session");
        db.execSQL("DROP TABLE IF EXISTS vehicles_cache");
        db.execSQL("DROP TABLE IF EXISTS stations_cache");
        db.execSQL("DROP TABLE IF EXISTS rental_local");
        db.execSQL("DROP TABLE IF EXISTS tracking_local");
        db.execSQL("DROP TABLE IF EXISTS notifications");
        db.execSQL("DROP TABLE IF EXISTS vouchers");
        db.execSQL("DROP TABLE IF EXISTS settings");

        onCreate(db);
    }
}