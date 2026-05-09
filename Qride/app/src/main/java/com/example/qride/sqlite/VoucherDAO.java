package com.example.qride.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.qride.model.VoucherModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho Voucher.
 * Đã refactor để tương thích với VoucherModel mới.
 */
public class VoucherDAO {
    private final DatabaseHelper dbHelper;
    private final Context context;

    public VoucherDAO(Context context) {
        this.context = context;
        this.dbHelper = new DatabaseHelper(context);
    }

    public List<VoucherModel> getVouchersByType(String typeCode) {
        List<VoucherModel> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM vouchers WHERE type = ? ORDER BY id ASC", new String[]{typeCode});

        if (cursor.moveToFirst()) {
            do {
                list.add(hydrateFromCursor(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    private VoucherModel hydrateFromCursor(Cursor cursor) {
        org.json.JSONObject mockJson = new org.json.JSONObject();
        try {
            mockJson.put("id", cursor.getInt(cursor.getColumnIndexOrThrow("id")));
            mockJson.put("type", cursor.getString(cursor.getColumnIndexOrThrow("type")));
            mockJson.put("status", cursor.getString(cursor.getColumnIndexOrThrow("status")));
            mockJson.put("action", cursor.getString(cursor.getColumnIndexOrThrow("action")));
            mockJson.put("icon", cursor.getString(cursor.getColumnIndexOrThrow("icon")));
            mockJson.put("title", cursor.getString(cursor.getColumnIndexOrThrow("title")));
            mockJson.put("title_key", cursor.getString(cursor.getColumnIndexOrThrow("title_key")));
            mockJson.put("discount", cursor.getString(cursor.getColumnIndexOrThrow("discount")));
            mockJson.put("price", cursor.getInt(cursor.getColumnIndexOrThrow("price")));
            mockJson.put("expiry", cursor.getString(cursor.getColumnIndexOrThrow("expiry")));
            mockJson.put("btn_type", cursor.getString(cursor.getColumnIndexOrThrow("btn_type")));
            mockJson.put("has_progress", cursor.getInt(cursor.getColumnIndexOrThrow("has_progress")) == 1);
            mockJson.put("prog_curr", cursor.getInt(cursor.getColumnIndexOrThrow("prog_curr")));
            mockJson.put("prog_max", cursor.getInt(cursor.getColumnIndexOrThrow("prog_max")));
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        VoucherModel model = VoucherModel.fromJson(mockJson);
        if (model != null) {
            model.setIconResId(getIconRes(context, model.getIconName()));
        }
        return model;
    }

    public void saveVouchers(List<VoucherModel> list, String type) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (VoucherModel item : list) {
                ContentValues v = new ContentValues();
                v.put("id", item.getId());
                v.put("type", type);
                v.put("status", item.getStatus() != null ? item.getStatus().name() : "NEW");
                v.put("icon", item.getIconName());
                v.put("title", item.getTitle());
                v.put("title_key", item.getTitleKey());
                v.put("discount", item.getDiscountText());
                v.put("price", item.getPrice());
                v.put("expiry", item.getExpiry());
                v.put("action", item.getActionLabel());
                v.put("btn_type", item.getButtonType() != null ? item.getButtonType().name() : "GREEN");
                v.put("has_progress", item.isHasProgress() ? 1 : 0);
                v.put("prog_curr", item.getProgressCurrent());
                v.put("prog_max", item.getProgressMax());

                db.insertWithOnConflict("vouchers", null, v, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private int getIconRes(Context context, String name) {
        if (name == null || name.isEmpty()) return com.example.qride.R.drawable.ic_wallet;
        int resId = context.getResources().getIdentifier(name, "drawable", context.getPackageName());
        return resId != 0 ? resId : com.example.qride.R.drawable.ic_wallet;
    }

    public void deleteVouchersByType(String type) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("vouchers", "type = ?", new String[]{type});
    }
}
