package com.example.qride.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.qride.model.VoucherModel;

import java.util.ArrayList;
import java.util.List;

public class VoucherDAO {
    private DatabaseHelper dbHelper;

    public VoucherDAO(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public List<VoucherModel> getVouchersByType(String type) {
        List<VoucherModel> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM vouchers WHERE [type] = ?", new String[]{type});

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                int icon = cursor.getInt(cursor.getColumnIndexOrThrow("icon"));
                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                String discount = cursor.getString(cursor.getColumnIndexOrThrow("discount"));
                String expiry = cursor.getString(cursor.getColumnIndexOrThrow("expiry"));
                String action = cursor.getString(cursor.getColumnIndexOrThrow("action"));
                String btnTypeStr = cursor.getString(cursor.getColumnIndexOrThrow("btn_type"));
                int hasProg = cursor.getInt(cursor.getColumnIndexOrThrow("has_progress"));
                int curr = cursor.getInt(cursor.getColumnIndexOrThrow("prog_curr"));
                int max = cursor.getInt(cursor.getColumnIndexOrThrow("prog_max"));

                VoucherModel.ButtonType btnType = "GREEN".equals(btnTypeStr) ? 
                        VoucherModel.ButtonType.GREEN : VoucherModel.ButtonType.ORANGE;

                if (hasProg == 1) {
                    list.add(new VoucherModel(id, icon, title, discount, expiry, action, btnType, curr, max));
                } else {
                    list.add(new VoucherModel(id, icon, title, discount, expiry, action, btnType));
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public boolean updateProgress(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT [prog_curr], [prog_max] FROM vouchers WHERE id = ?", 
                new String[]{String.valueOf(id)});
        
        boolean success = false;
        if (cursor.moveToFirst()) {
            int curr = cursor.getInt(0);
            int max = cursor.getInt(1);
            
            if (curr < max) {
                curr++;
                ContentValues values = new ContentValues();
                values.put("prog_curr", curr);
                
                if (curr >= max) {
                    values.put("action", "action_use_now"); // Cập nhật KEY thay vì text
                    values.put("btn_type", "GREEN");
                }
                
                int rows = db.update("vouchers", values, "id = ?", new String[]{String.valueOf(id)});
                success = rows > 0;
            }
        }
        cursor.close();
        return success;
    }

    public boolean activateVoucher(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("action", "action_using"); // Cập nhật KEY
        values.put("btn_type", "ORANGE");
        
        int rows = db.update("vouchers", values, "id = ?", new String[]{String.valueOf(id)});
        return rows > 0;
    }

    public boolean activateMembership(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("action", "action_in_progress"); // Cập nhật KEY
        values.put("btn_type", "ORANGE");
        
        int rows = db.update("vouchers", values, "id = ?", new String[]{String.valueOf(id)});
        return rows > 0;
    }
}
