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

        SharedPreferences pref = getSharedPreferences("Settings", MODE_PRIVATE);

        int activeMode = AppCompatDelegate.getDefaultNightMode();

        if (activeMode == AppCompatDelegate.MODE_NIGHT_UNSPECIFIED) {
            activeMode = pref.getInt("Dark_Mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }

        if (activeMode == AppCompatDelegate.MODE_NIGHT_YES) {
            rgDarkMode.check(R.id.rbOn);
        } else if (activeMode == AppCompatDelegate.MODE_NIGHT_NO) {
            rgDarkMode.check(R.id.rbOff);
        } else {
            rgDarkMode.check(R.id.rbSystem);
        }

        findViewById(R.id.btnUpdate).setOnClickListener(v -> {
            int checkedId = rgDarkMode.getCheckedRadioButtonId();

            if (checkedId == R.id.rbOn) {
                modeSelected = AppCompatDelegate.MODE_NIGHT_YES;
            } else if (checkedId == R.id.rbOff) {
                modeSelected = AppCompatDelegate.MODE_NIGHT_NO;
            } else {
                modeSelected = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            }

            pref.edit().putInt("Dark_Mode", modeSelected).apply();
            AppCompatDelegate.setDefaultNightMode(modeSelected);
            finish();
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}
