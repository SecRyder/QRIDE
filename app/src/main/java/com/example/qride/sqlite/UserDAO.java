package com.example.qride.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class UserDAO {
    private DatabaseHelper dbHelper;

    public UserDAO(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    // 1. Thêm user mới (Lúc đăng ký chỉ cần số điện thoại và mật khẩu)
    // Thêm user mới (Nhận full thông tin lúc đăng ký)
    public long insertUser(String phone, String password, String name, String cccd, String address, String gender, String birthday) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("phone", phone);
        values.put("password", password);
        values.put("name", name);
        values.put("cccd", cccd);
        values.put("address", address);
        values.put("gender", gender);
        values.put("birthday", birthday);

        return db.insert("users", null, values);
    }

    // 2. Kiểm tra đăng nhập
    public boolean checkLogin(String phone, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("users",
                new String[]{"id"},
                "phone=? AND password=?",
                new String[]{phone, password},
                null, null, null);
        boolean result = cursor.moveToFirst();
        cursor.close();
        return result;
    }

    // 3. Cập nhật thông tin Profile
    public boolean updateUserInfo(String phone, String name, String cccd, String address, String gender, String birthday) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("name", name);
        values.put("cccd", cccd);
        values.put("address", address);
        values.put("gender", gender);
        values.put("birthday", birthday);

        // Cập nhật thông tin vào dòng có số điện thoại tương ứng
        int rowsAffected = db.update("users", values, "phone=?", new String[]{phone});

        return rowsAffected > 0;
    }

    // 4. Lấy thông tin chi tiết của người dùng
    public Cursor getUserInfo(String phone) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        // Lấy tất cả các cột của user có số điện thoại tương ứng
        return db.query("users", null, "phone=?", new String[]{phone}, null, null, null);
    }
}