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

    public void addNotification(String title, String message, String type) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("message", message);
        values.put("type", type);
        values.put("timestamp", new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()));
        values.put("is_read", 0);
        db.insert("notifications", null, values);
    }

    public List<NotificationModel> getAllNotifications() {
        List<NotificationModel> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM notifications ORDER BY id DESC", null);
        if (cursor.moveToFirst()) {
            do {
                list.add(new NotificationModel(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getInt(4) == 1,
                        cursor.getString(5)
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public int getUnreadCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM notifications WHERE is_read = 0", null);
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    public void markAllAsRead() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_read", 1);
        db.update("notifications", values, null, null);
    }
}
