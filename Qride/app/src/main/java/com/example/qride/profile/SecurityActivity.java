package com.example.qride.profile;

import android.content.Intent;
import android.content.SharedPreferences;
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
// Fullscreen
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
        //  Xử lý nút Back trên Header
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        //  Ánh xạ các mục Menu (dùng include)
        View menuChangePass = findViewById(R.id.menuChangePass);
        View menuChangePhone = findViewById(R.id.menuChangePhone);

        // ================= CẤU HÌNH FACE ID =================
        View menuFaceID = findViewById(R.id.menuFaceID);
        TextView tvFaceID = menuFaceID.findViewById(R.id.tvSwitchTitle);
        tvFaceID.setText("Bật xác thực khuôn mặt");

        androidx.appcompat.widget.SwitchCompat switchFaceID = menuFaceID.findViewById(R.id.switchWidget);

        // Bước 1: Khởi tạo SharedPreferences để quản lý cài đặt bảo mật
        SharedPreferences securityPref = getSharedPreferences("security_settings", MODE_PRIVATE);

        // Bước 2: Đọc trạng thái đã lưu trước đó (mặc định là false nếu chưa từng bật)
        boolean isFaceIDEnabled = securityPref.getBoolean("face_id_status", false);

        // Bước 3: Đặt trạng thái ban đầu cho nút gạt dựa trên dữ liệu đã đọc
        switchFaceID.setChecked(isFaceIDEnabled);

        // Bước 4: Lắng nghe sự kiện người dùng gạt nút để lưu lại trạng thái mới
        switchFaceID.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = securityPref.edit();
            editor.putBoolean("face_id_status", isChecked);
            editor.apply(); // Xác nhận lưu vào bộ nhớ máy

            if (isChecked) {
                Toast.makeText(this, "Đã bật xác thực khuôn mặt", Toast.LENGTH_SHORT).show();
                // Code xử lý thêm khi bật Face ID đặt ở đây
            } else {
                Toast.makeText(this, "Đã tắt xác thực khuôn mặt", Toast.LENGTH_SHORT).show();
                // Code xử lý thêm khi tắt Face ID đặt ở đây
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