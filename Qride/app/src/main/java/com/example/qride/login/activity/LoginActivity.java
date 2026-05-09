package com.example.qride.login.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.qride.R;
import com.example.qride.login.adapter.LanguageAdapter;
import com.example.qride.login.model.Language;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LoginActivity extends AppCompatActivity {
    private Spinner spnNgonNgu;
    private TextView tvLogo, tvSlogan, tvWelcome, tvCauHoi, tvDangKy;
    private ImageView imgLogin;
    private Button btnDangNhap;
    boolean isFirstLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        // Load lai ngon ngu da luu
        loadLocale();
        setContentView(R.layout.activity_login);

        // Anh xa:
        spnNgonNgu = findViewById(R.id.spnNgonNgu);
        tvLogo = findViewById(R.id.tvLogo);
        tvSlogan = findViewById(R.id.tvSlogan);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvCauHoi = findViewById(R.id.tvCauHoi);
        tvDangKy = findViewById(R.id.tvDangKy);
        imgLogin = findViewById(R.id.imgLogin);
        btnDangNhap = findViewById(R.id.btnDangNhap);
        setupSpinner();
        setupLogin();
        setupRegister();
    }

    // Ham Spinner ngon ngu
    private void setupSpinner() {
        List<Language> languages = new ArrayList<>();
        languages.add(new Language("Tiếng Việt", R.drawable.vietnam_flag));
        languages.add(new Language("English", R.drawable.english_flag));

        LanguageAdapter adapter = new LanguageAdapter(this, languages);
        spnNgonNgu.setAdapter(adapter);

        // set vi tri theo ngon ngu da luu
        String lang = getSavedLanguage();
        if (lang.equals("vi")) {
            spnNgonNgu.setSelection(0);
        } else {
            spnNgonNgu.setSelection(1);
        }

        spnNgonNgu.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isFirstLoad) {
                    isFirstLoad = false;
                    return;
                }
                String selectedLang = (position == 0) ? "vi" : "en";
                if (!selectedLang.equals(getSavedLanguage())) {
                    setLocale(selectedLang);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }


    // Xu ly login
    private void setupLogin() {
        btnDangNhap.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, LoginTaiKhoanActivity.class);
            startActivity(intent);
        });
    }

    // Xu ly dang ky
    private void setupRegister(){
        tvDangKy.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this,RegisterActivity.class);
            startActivity(intent);
        });
    }

    // Doi ngon ngu
    private void setLocale(String lang) {
        saveLanguage(lang);
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        // reload activity
        recreate();
    }

    // Luu ngon ngu
    private void saveLanguage(String lang) {
        SharedPreferences sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("My_lang", lang);
        editor.apply();
    }

    private String getSavedLanguage() {
        SharedPreferences sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE);
        return sharedPreferences.getString("My_lang", "vi"); // mac dinh
    }

    private void loadLocale() {
        String lang = getSavedLanguage();
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration configuration = new Configuration();
        configuration.setLocale(locale);
        getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
    }
}
