package com.example.qride.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "qride.db";
    private static final int DATABASE_VERSION = 10;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tạo bảng users với id tự động tăng
        db.execSQL("CREATE TABLE users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "phone TEXT UNIQUE," +
                "password TEXT," +
                "name TEXT," +
                "cccd TEXT UNIQUE," + // CCCD không được trùng
                "address TEXT," +
                "gender TEXT," +
                "birthday TEXT)");

        // Thêm 9 tài khoản mẫu CÓ ĐẦY ĐỦ THÔNG TIN
        for (int i = 2; i <= 10; i++) {
            // Thêm số 0 ở đầu cho đủ 10 số
            String phone = "098765432" + i;
            String password = "Nguyet21@" + i;
            String name = "Người dùng " + i;
            String cccd = "00109900000" + i; // CCCD mẫu đủ 12 số

            // Insert full dữ liệu mẫu
            db.execSQL("INSERT INTO users (phone, password, name, cccd, address, gender, birthday) " +
                    "VALUES ('" + phone + "', '" + password + "', '" + name + "', '" + cccd + "', 'Hà Nội', 'Nam', '01/01/2000')");
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS users");
        onCreate(db);
    }
}