package com.example.qride.profile;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.qride.R;

public class NotificationSettingsActivity extends AppCompatActivity {

    private SwitchCompat swSystem, swTransaction, swOther;
    private SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);

        pref = getSharedPreferences("NotifyConfig", MODE_PRIVATE);

        // 1. Ánh xạ
        swSystem = findViewById(R.id.swSystem);
        swTransaction = findViewById(R.id.swTransaction);
        swOther = findViewById(R.id.swOther);
        ImageView btnBack = findViewById(R.id.btnBack);

        // 2. Load trạng thái cũ và áp dụng màu sắc ngay lập tức
        loadAndRefreshSwitch(swSystem, "notif_system");
        loadAndRefreshSwitch(swTransaction, "notif_transaction");
        loadAndRefreshSwitch(swOther, "notif_other");

        // 3. Lắng nghe sự kiện thay đổi
        swSystem.setOnCheckedChangeListener((v, isChecked) -> {
            saveConfig("notif_system", isChecked);
            updateSwitchColor(swSystem, isChecked);
        });

        swTransaction.setOnCheckedChangeListener((v, isChecked) -> {
            saveConfig("notif_transaction", isChecked);
            updateSwitchColor(swTransaction, isChecked);
        });

        swOther.setOnCheckedChangeListener((v, isChecked) -> {
            saveConfig("notif_other", isChecked);
            updateSwitchColor(swOther, isChecked);
        });

        btnBack.setOnClickListener(v -> finish());
    }

    // Hàm load dữ liệu và set màu lần đầu
    private void loadAndRefreshSwitch(SwitchCompat sw, String key) {
        boolean status = pref.getBoolean(key, true);
        sw.setChecked(status);
        updateSwitchColor(sw, status);
    }

    // Hàm quan trọng nhất: Đổi màu track dựa trên trạng thái Bật/Tắt
    private void updateSwitchColor(SwitchCompat sw, boolean isChecked) {
        if (isChecked) {
            // Khi Bật: Màu xanh ngọc (Lấy từ colors.xml của bạn)
            int colorXanh = getResources().getColor(R.color.xanhNgoc);
            sw.setTrackTintList(ColorStateList.valueOf(colorXanh));
        } else {
            // Khi Tắt: Màu xám nhạt (Mã màu #D3D3D3)
            sw.setTrackTintList(ColorStateList.valueOf(Color.parseColor("#D3D3D3")));
        }
    }

    private void saveConfig(String key, boolean value) {
        pref.edit().putBoolean(key, value).apply();
    }
}