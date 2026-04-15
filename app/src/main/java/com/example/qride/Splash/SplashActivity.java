package com.example.qride.Splash;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.example.qride.MainActivity;
import com.example.qride.Onboarding.OnboardingActivity;
import com.example.qride.R;
import com.example.qride.login.activity.LoginActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        new Handler().postDelayed(() -> {
            SharedPreferences prefOnboard = getSharedPreferences("Onboarding", MODE_PRIVATE);
            boolean isFinished = prefOnboard.getBoolean("Finished", false);

            SharedPreferences prefLogin = getSharedPreferences("login_check", MODE_PRIVATE);
            boolean isRemembered = prefLogin.getBoolean("remember", false);
            long lastCloseTime = prefLogin.getLong("lastCloseTime", 0);

            if (!isFinished) {
                // Lần đầu cài app -> Đi tới Onboarding
                startActivity(new Intent(this, OnboardingActivity.class));
            } else if (!isRemembered || System.currentTimeMillis() - lastCloseTime > 5 * 60 * 1000) {
                // Đã xem onboard nhưng chưa đăng nhập/không nhớ -> Đi tới Login
                prefLogin.edit().clear().apply();
                startActivity(new Intent(this, LoginActivity.class));
            } else {
                // Đã OK hết -> Vào thẳng trang chủ
                startActivity(new Intent(this, MainActivity.class));
            }
            finish();
        }, 2000); // Đợi 2 giây
    }
}