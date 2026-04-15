package com.example.qride.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.qride.R;

public class SecurityActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security);

        //  Xử lý nút Back trên Header
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        //  Ánh xạ các mục Menu (dùng include)
        View menuChangePass = findViewById(R.id.menuChangePass);
        View menuChangePhone = findViewById(R.id.menuChangePhone);

        //  Cấu hình cho mục Face ID
        View menuFaceID = findViewById(R.id.menuFaceID);
        TextView tvFaceID = menuFaceID.findViewById(R.id.tvSwitchTitle);
        tvFaceID.setText("Bật xác thực khuôn mặt");
        androidx.appcompat.widget.SwitchCompat switchFaceID = menuFaceID.findViewById(R.id.switchWidget);
        switchFaceID.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Code khi bật
            } else {
                // Code khi tắt
            }
        });

        if (menuChangePass != null) {
            TextView tvTitle = menuChangePass.findViewById(R.id.tvMenuTitle);
            tvTitle.setText("Đổi mật khẩu");
            menuChangePass.setOnClickListener(v -> {
                 Intent intent = new Intent(this, ChangePasswordActivity.class);
                 startActivity(intent);
            });
        }

        if (menuChangePhone != null) {
            TextView tvTitle = menuChangePhone.findViewById(R.id.tvMenuTitle);
            tvTitle.setText("Thay đổi số điện thoại");

            menuChangePhone.setOnClickListener(v -> {
                // GƯỚC 1: Lấy số điện thoại đã lưu lúc đăng nhập (trong SharedPreferences)
                // Đây là "chìa khóa" để bạn tìm đúng người dùng trong DB
                android.content.SharedPreferences sharedPreferences = getSharedPreferences("login_check", MODE_PRIVATE);
                String savedPhone = sharedPreferences.getString("phone", "");

                // BƯỚC 2: Truyền số điện thoại này sang ChangePhoneActivity
                Intent intent = new Intent(this, ChangePhoneActivity.class);
                intent.putExtra("current_phone", savedPhone);
                startActivity(intent);
            });
        }
    }
}