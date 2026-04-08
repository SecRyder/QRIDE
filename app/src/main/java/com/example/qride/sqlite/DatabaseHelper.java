package com.example.qride.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "qride.db";
    private static final int DATABASE_VERSION = 3;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tạo bảng users
        db.execSQL("CREATE TABLE users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "phone TEXT UNIQUE," +
                "password TEXT)");
        // Them 9 tai khoan mau
        for (int i = 2; i <= 10; i++) {
            String phone = "98765432" + i;
            String password = "Nguyet21@" + i;
            db.execSQL("INSERT INTO users (phone, password) VALUES ('" + phone + "', '" + password + "')");
        }
        // bảng vehicles, bookings, payments...
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS users");
        onCreate(db);
    }
}
