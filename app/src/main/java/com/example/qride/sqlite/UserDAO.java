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

    // Them user moi
    public long insertUser(String phone, String password) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("phone", phone);
        values.put("password", password);
        return db.insert("users", null, values);
    }

    // Kiem tra dang nhap
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
}

