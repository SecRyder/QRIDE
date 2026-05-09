package com.example.qride.profile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import com.example.qride.R;

public class ChangeLanguageActivity extends AppCompatActivity {

    private RadioButton btnTiengViet, btnTiengAnh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_language); // File XML có RadioGroup bạn vừa làm

        btnTiengViet = findViewById(R.id.btnTiengViet);
        btnTiengAnh = findViewById(R.id.btnTiengAnh);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 1. Kiểm tra ngôn ngữ hiện tại để tích chọn đúng RadioButton
        LocaleListCompat currentAppLocales = AppCompatDelegate.getApplicationLocales();
        if (!currentAppLocales.isEmpty() && currentAppLocales.get(0).getLanguage().equals("en")) {
            btnTiengAnh.setChecked(true);
        } else {
            btnTiengViet.setChecked(true);
        }

        // 2. Xử lý khi chọn Tiếng Việt
        btnTiengViet.setOnClickListener(v -> changeLanguage("vi"));

        // 3. Xử lý khi chọn Tiếng Anh
        btnTiengAnh.setOnClickListener(v -> changeLanguage("en"));
    }

    private void changeLanguage(String languageCode) {
        //Lưu lại vào bộ nhớ máy
        getSharedPreferences("Settings", MODE_PRIVATE)
                .edit()
                .putString("My_Lang", languageCode)
                .apply();

        LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(languageCode);
        AppCompatDelegate.setApplicationLocales(appLocale);
        // Sau lệnh này, toàn bộ App sẽ tự động refresh lại với ngôn ngữ mới
        Intent intent = new Intent(this, com.example.qride.MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Đóng luôn màn hình chọn ngôn ngữ này
    }
}