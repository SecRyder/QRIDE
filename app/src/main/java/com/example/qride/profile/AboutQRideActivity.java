package com.example.qride.profile; // Nhớ đổi đúng tên package của bạn

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.qride.R;

public class AboutQRideActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_qride);

        // Xử lý nút Back để quay lại trang trước
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            finish(); // Đóng Activity này để quay lại Profile
        });
    }
}