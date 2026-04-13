package com.example.qride;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
                Toast.makeText(this, "Mở màn hình Đổi mật khẩu", Toast.LENGTH_SHORT).show();
                // Intent intent = new Intent(this, ChangePasswordActivity.class);
                // startActivity(intent);
            });
        }

        if (menuChangePhone != null) {
            TextView tvTitle = menuChangePhone.findViewById(R.id.tvMenuTitle);
            tvTitle.setText("Thay đổi số điện thoại");

            menuChangePhone.setOnClickListener(v -> {
                Toast.makeText(this, "Mở màn hình Đổi số điện thoại", Toast.LENGTH_SHORT).show();
            });
        }
    }
}