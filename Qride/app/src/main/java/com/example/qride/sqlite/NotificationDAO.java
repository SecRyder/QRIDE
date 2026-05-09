package com.example.qride.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.qride.model.NotificationModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationDAO {
    private DatabaseHelper dbHelper;

    public NotificationDAO(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public void addNotification(int userId, String title, String message, String type) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("title", title);
        values.put("message", message);
        values.put("type", type);
        values.put("timestamp", new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()));
        values.put("is_read", 0);
        db.insert("notifications", null, values);
    }

    public List<NotificationModel> getAllNotifications(int userId) {
        List<NotificationModel> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM notifications WHERE user_id = ? ORDER BY id DESC", new String[]{String.valueOf(userId)});
        if (cursor.moveToFirst()) {
            do {
                list.add(new NotificationModel(
                        cursor.getInt(0),             // id
                        cursor.getString(2),          // title
                        cursor.getString(3),          // message
                        cursor.getString(4),          // timestamp
                        cursor.getInt(5) == 1,        // is_read (1 = true)
                        cursor.getString(6)           // type
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public int getUnreadCount(int userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0", new String[]{String.valueOf(userId)});
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    public void markAllAsRead(int userId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_read", 1);
        db.update("notifications", values, "user_id = ?", new String[]{String.valueOf(userId)});
    }
}
