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

            com.example.qride.sqlite.UserDAO userDAO = new com.example.qride.sqlite.UserDAO(this);
            String token = userDAO.getToken();

            if (!isFinished) {
                // Lần đầu cài app -> Đi tới Onboarding
                startActivity(new Intent(this, OnboardingActivity.class));
            } else if (token != null && !token.isEmpty()) {
                // Đã đăng nhập và có session -> Vào thẳng trang chủ
                startActivity(new Intent(this, MainActivity.class));
            } else {
                // Chưa đăng nhập hoặc session hết hạn -> Đi tới Login
                startActivity(new Intent(this, LoginActivity.class));
            }
            finish();
        }, 2000); // Đợi 2 giây
    }
}