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

    // ================= SAVE SESSION (sau khi login API) =================
    public long saveUserSession(int userId, String phone, String token) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        db.delete("user_session", null, null);

        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("phone", phone);
        values.put("token", token);
        values.put("login_time", System.currentTimeMillis());

        return db.insert("user_session", null, values);
    }

    // ================= GET SESSION =================
    public Cursor getUserSession() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery("SELECT * FROM user_session LIMIT 1", null);
    }

    public int getUserId() {
        Cursor cursor = getUserSession();
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow("user_id"));
            cursor.close();
            return id;
        }
        if (cursor != null) cursor.close();
        return -1;
    }

    public String getToken() {
        Cursor cursor = getUserSession();
        if (cursor != null && cursor.moveToFirst()) {
            String token = cursor.getString(cursor.getColumnIndexOrThrow("token"));
            cursor.close();
            return token;
        }
        if (cursor != null) cursor.close();
        return null;
    }

    public String getPhone() {
        Cursor cursor = getUserSession();
        if (cursor != null && cursor.moveToFirst()) {
            String phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
            cursor.close();
            return phone;
        }
        if (cursor != null) cursor.close();
        return null;
    }

    // ================= LOGOUT =================
    public void clearSession() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("user_session", null, null);
    }

    // ================= VEHICLE CACHE =================
    public void cacheVehicles(Cursor serverData) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("vehicles_cache", null, null);

        if (serverData != null && serverData.moveToFirst()) {
            do {
                ContentValues values = new ContentValues();
                values.put("id", serverData.getInt(serverData.getColumnIndexOrThrow("id")));
                values.put("plate", serverData.getString(serverData.getColumnIndexOrThrow("plate")));
                values.put("status", serverData.getString(serverData.getColumnIndexOrThrow("current_status")));
                values.put("pin", serverData.getInt(serverData.getColumnIndexOrThrow("pin")));
                values.put("station_id", serverData.getInt(serverData.getColumnIndexOrThrow("station_id")));

                db.insert("vehicles_cache", null, values);
            } while (serverData.moveToNext());
        }
    }

    public Cursor getCachedVehicles() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery("SELECT * FROM vehicles_cache", null);
    }

    // ================= STATION CACHE =================
    public void cacheStations(Cursor serverData) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("stations_cache", null, null);

        if (serverData != null && serverData.moveToFirst()) {
            do {
                ContentValues values = new ContentValues();
                values.put("id", serverData.getInt(serverData.getColumnIndexOrThrow("id")));
                values.put("name", serverData.getString(serverData.getColumnIndexOrThrow("name")));
                values.put("address", serverData.getString(serverData.getColumnIndexOrThrow("address")));
                values.put("lat", serverData.getDouble(serverData.getColumnIndexOrThrow("lat")));
                values.put("lng", serverData.getDouble(serverData.getColumnIndexOrThrow("lng")));

                db.insert("stations_cache", null, values);
            } while (serverData.moveToNext());
        }
    }

    public Cursor getCachedStations() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery("SELECT * FROM stations_cache", null);
    }

    // ================= RENTAL LOCAL =================
    public void saveRentalLocal(int vehicleId, String startTime) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("vehicle_id", vehicleId);
        values.put("start_time", startTime);
        values.put("status", "renting");

        db.insert("rental_local", null, values);
    }

    public Cursor getCurrentRentalLocal() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery("SELECT * FROM rental_local WHERE status='renting' LIMIT 1", null);
    }

    public void clearRentalLocal() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("rental_local", null, null);
    }
}
