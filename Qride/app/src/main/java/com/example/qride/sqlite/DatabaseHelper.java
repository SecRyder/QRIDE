package com.example.qride.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.qride.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "qride.db";
    private static final int DATABASE_VERSION = 25; // Cập nhật bản 25: thêm cột price cho voucher
    private Context context;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. Bảng Users (Local info)
        db.execSQL("CREATE TABLE users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "phone TEXT UNIQUE," +
                "password TEXT," +
                "name TEXT," +
                "cccd TEXT UNIQUE," +
                "address TEXT," +
                "gender TEXT," +
                "birthday TEXT," +
                "created_at TEXT)");

        // 2. Bảng Vouchers
        db.execSQL("CREATE TABLE vouchers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "\"type\" TEXT," + 
                "status TEXT," + 
                "icon TEXT," +
                "title TEXT," +
                "title_key TEXT," +
                "discount TEXT," +
                "price INTEGER," + // Thêm cột price
                "expiry TEXT," +
                "\"action\" TEXT," + 
                "btn_type TEXT," +
                "has_progress INTEGER," +
                "prog_curr INTEGER," +
                "prog_max INTEGER)");

        // 3. Bảng Notifications
        db.execSQL("CREATE TABLE notifications (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER," + 
                "title TEXT," +
                "message TEXT," +
                "timestamp TEXT," +
                "is_read INTEGER DEFAULT 0," +
                "\"type\" TEXT)");

        // 4. Bảng Session & Cache (Cần thiết cho API)
        db.execSQL("CREATE TABLE user_session (" +
                "user_id INTEGER PRIMARY KEY," +
                "phone TEXT," +
                "token TEXT," +
                "login_time INTEGER)");

        db.execSQL("CREATE TABLE vehicles_cache (" +
                "id INTEGER PRIMARY KEY," +
                "plate TEXT," +
                "status TEXT," +
                "pin INTEGER," +
                "station_id INTEGER)");

        db.execSQL("CREATE TABLE stations_cache (" +
                "id INTEGER PRIMARY KEY," +
                "name TEXT," +
                "address TEXT," +
                "lat REAL," +
                "lng REAL)");

        db.execSQL("CREATE TABLE rental_local (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "vehicle_id INTEGER," +
                "start_time TEXT," +
                "status TEXT)");

        // 5. Các bảng nghiệp vụ khác
        db.execSQL("CREATE TABLE vehicles (id INTEGER PRIMARY KEY AUTOINCREMENT, plate TEXT UNIQUE, \"type\" TEXT, status TEXT)");
        db.execSQL("CREATE TABLE bookings (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, vehicle_id INTEGER, start_time TEXT, end_time TEXT, total_price REAL, status TEXT)");
        db.execSQL("CREATE TABLE payments (id INTEGER PRIMARY KEY AUTOINCREMENT, booking_id INTEGER, amount REAL, method TEXT, status TEXT)");

        insertSampleData(db);
    }

    private void insertSampleData(SQLiteDatabase db) {
        String currentTime = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());

        for (int i = 2; i <= 10; i++) {
            String phone = "098765432" + i;
            ContentValues userValues = new ContentValues();
            userValues.put("phone", phone);
            userValues.put("password", "Nguyet21@" + i);
            userValues.put("name", "Người dùng " + i);
            userValues.put("cccd", "00109900000" + i);
            userValues.put("address", "Hà Nội");
            userValues.put("gender", "Nam");
            userValues.put("birthday", "01/01/2000");
            userValues.put("created_at", currentTime);
            
            long userId = db.insert("users", null, userValues);

            if (userId != -1) {
                insertNotification(db, userId, "notif_title_welcome", "notif_msg_welcome", currentTime, "GENERAL");
                insertNotification(db, userId, "notif_title_voucher", "notif_msg_voucher", currentTime, "VOUCHER");
            }
        }

        // Vouchers mẫu
        insertVoucher(db, "TICH_QUA", "ic_gift", "Quà chào mừng", "v_title_welcome", "v_discount_50", 0, "v_expiry_30d", "action_use_now", "GREEN", 0, 0, 0);
        insertVoucher(db, "TICH_QUA", "ic_membership", "Mời bạn nhận quà", "v_title_invite", "v_discount_free", 0, "v_expiry_none", "action_invite_now", "ORANGE", 1, 0, 5);
        insertVoucher(db, "GOI_HOI_VIEN", "ic_membership_crown", "Gói Cơ bản", "v_title_m_basic", "v_discount_basic", 0, "v_expiry_31_12", "action_register", "GREEN", 0, 0, 0);
        insertVoucher(db, "GOI_HOI_VIEN", "ic_membership_crown", "Gói Cao cấp", "v_title_m_premium", "v_discount_premium", 0, "v_expiry_31_12", "action_register", "ORANGE", 0, 0, 0);
    }

    private void insertVoucher(SQLiteDatabase db, String type, String icon, String title, String titleKey, String discount, int price, String expiry, String action, String btnType, int hasProg, int curr, int max) {
        ContentValues values = new ContentValues();
        values.put("type", type);
        values.put("icon", icon);
        values.put("title", title);
        values.put("title_key", titleKey);
        values.put("discount", discount);
        values.put("price", price);
        values.put("expiry", expiry);
        values.put("action", action);
        values.put("btn_type", btnType);
        values.put("has_progress", hasProg);
        values.put("prog_curr", curr);
        values.put("prog_max", max);
        db.insert("vouchers", null, values);
    }

    private void insertNotification(SQLiteDatabase db, long userId, String title, String message, String timestamp, String type) {
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("title", title);
        values.put("message", message);
        values.put("timestamp", timestamp);
        values.put("is_read", 0);
        values.put("type", type);
        db.insert("notifications", null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS users");
        db.execSQL("DROP TABLE IF EXISTS vouchers");
        db.execSQL("DROP TABLE IF EXISTS vehicles");
        db.execSQL("DROP TABLE IF EXISTS bookings");
        db.execSQL("DROP TABLE IF EXISTS payments");
        db.execSQL("DROP TABLE IF EXISTS notifications");
        db.execSQL("DROP TABLE IF EXISTS user_session");
        db.execSQL("DROP TABLE IF EXISTS vehicles_cache");
        db.execSQL("DROP TABLE IF EXISTS stations_cache");
        db.execSQL("DROP TABLE IF EXISTS rental_local");
        onCreate(db);
    }
}
