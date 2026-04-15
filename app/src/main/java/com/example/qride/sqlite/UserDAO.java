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

    // 1. Thêm user mới
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

        int rowsAffected = db.update("users", values, "phone=?", new String[]{phone});
        return rowsAffected > 0;
    }

    // 4. Lấy thông tin chi tiết của người dùng
    public Cursor getUserInfo(String phone) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.query("users", null, "phone=?", new String[]{phone}, null, null, null);
    }

    public boolean updatePassword(String phone, String oldPass, String newPass) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Bước 1: Kiểm tra mật khẩu cũ có khớp không
        Cursor cursor = db.query("users",
                new String[]{"id"},
                "phone=? AND password=?",
                new String[]{phone, oldPass},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            // Bước 2: Nếu đúng mật khẩu cũ, tiến hành cập nhật mật khẩu mới
            ContentValues values = new ContentValues();
            values.put("password", newPass);

            int rows = db.update("users", values, "phone=?", new String[]{phone});
            cursor.close();
            return rows > 0;
        }

        if (cursor != null) cursor.close();
        return false; // Sai mật khẩu cũ
    }

    // Kiểm tra xem số điện thoại đã tồn tại trong DB chưa
    public boolean isPhoneExist(String phone) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("users",
                new String[]{"phone"},
                "phone=?",
                new String[]{phone},
                null, null, null);

        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        return exists;
    }

    // Kiem tra so dien thoai da ton tai
    public boolean checkPhoneExists(String phone) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE phone=?", new String[]{phone});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public boolean updatePhoneNumber(String oldPhone, String newPhone) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Log để kiểm tra thực tế giá trị truyền vào
        android.util.Log.d("SQL_CHECK", "Dang tim de update: " + oldPhone + " thanh " + newPhone);

        ContentValues values = new ContentValues();
        values.put("phone", newPhone);

        int rowsAffected = db.update("users", values, "phone=?", new String[]{oldPhone});

        android.util.Log.d("SQL_CHECK", "So dong bi thay doi: " + rowsAffected);

        return rowsAffected > 0;
    }

    // Ham update password
    public boolean updatePassword(String phone, String newPassword) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("password", newPassword);

        int rows = db.update("users", values, "phone=?", new String[]{phone});
        return rows > 0; // true nếu cập nhật thành công
    }
}