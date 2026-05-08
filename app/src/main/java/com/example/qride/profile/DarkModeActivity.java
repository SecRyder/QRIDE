package com.example.qride.profile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.qride.R;

public class DarkModeActivity extends AppCompatActivity {
    private RadioGroup rgDarkMode;
    private int modeSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dark_mode);

        rgDarkMode = findViewById(R.id.rgDarkMode);

        // 1. Khai báo pref ở đây để cả onCreate và onClick đều dùng được
        SharedPreferences pref = getSharedPreferences("Settings", MODE_PRIVATE);

        // 2. Đọc trạng thái đang thực thi của App
        int activeMode = AppCompatDelegate.getDefaultNightMode();

        // Nếu chưa từng set (mới cài app), lấy từ SharedPreferences
        if (activeMode == AppCompatDelegate.MODE_NIGHT_UNSPECIFIED) {
            activeMode = pref.getInt("Dark_Mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }

        // 3. Check đúng nút dựa trên trạng thái thực tế
        if (activeMode == AppCompatDelegate.MODE_NIGHT_YES) {
            rgDarkMode.check(R.id.rbOn);
        } else if (activeMode == AppCompatDelegate.MODE_NIGHT_NO) {
            rgDarkMode.check(R.id.rbOff);
        } else {
            rgDarkMode.check(R.id.rbSystem);
        }

        // 4. Nút Cập nhật - Thực thi đổi màu
        findViewById(R.id.btnUpdate).setOnClickListener(v -> {
            int checkedId = rgDarkMode.getCheckedRadioButtonId();

            if (checkedId == R.id.rbOn) {
                modeSelected = AppCompatDelegate.MODE_NIGHT_YES;
            } else if (checkedId == R.id.rbOff) {
                modeSelected = AppCompatDelegate.MODE_NIGHT_NO;
            } else {
                modeSelected = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            }

            // 1. Lưu vào SharedPreferences
            pref.edit().putInt("Dark_Mode", modeSelected).apply();

            // 2. Áp dụng mode mới cho toàn hệ thống
            AppCompatDelegate.setDefaultNightMode(modeSelected);

            // 3. THOÁT RA để về màn hình trước đó
            // Lưu ý: Khi gọi setDefaultNightMode, các Activity ở Backstack
            // cũng sẽ tự động được làm mới khi bạn quay lại.
            finish();
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}
