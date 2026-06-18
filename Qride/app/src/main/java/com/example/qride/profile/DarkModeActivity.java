package com.example.qride.profile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.qride.R;

public class DarkModeActivity extends AppCompatActivity {
    private RadioGroup rgDarkMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dark_mode);
// Fullscreen
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
        rgDarkMode = findViewById(R.id.rgDarkMode);
        SharedPreferences pref = getSharedPreferences("Settings", MODE_PRIVATE);

        // 1. Đọc chế độ ĐÃ LƯU (mặc định là Follow System nếu chưa từng lưu)
        int savedMode = pref.getInt("Dark_Mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // 2. Cập nhật đúng trạng thái RadioButton dựa trên giá trị đã lưu
        if (savedMode == AppCompatDelegate.MODE_NIGHT_YES) {
            rgDarkMode.check(R.id.rbOn);
        } else if (savedMode == AppCompatDelegate.MODE_NIGHT_NO) {
            rgDarkMode.check(R.id.rbOff);
        } else {
            rgDarkMode.check(R.id.rbSystem);
        }

        // 3. Xử lý nút Cập nhật
        findViewById(R.id.btnUpdate).setOnClickListener(v -> {
            int checkedId = rgDarkMode.getCheckedRadioButtonId();
            int modeSelected;

            if (checkedId == R.id.rbOn) {
                modeSelected = AppCompatDelegate.MODE_NIGHT_YES;
            } else if (checkedId == R.id.rbOff) {
                modeSelected = AppCompatDelegate.MODE_NIGHT_NO;
            } else {
                modeSelected = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            }

            // Lưu vào SharedPreferences
            pref.edit().putInt("Dark_Mode", modeSelected).apply();

            // Áp dụng ngay lập tức
            AppCompatDelegate.setDefaultNightMode(modeSelected);

            // Kết thúc activity
            finish();
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}