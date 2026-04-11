package com.example.qride.login.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.qride.MainActivity;
import com.example.qride.R;
import com.example.qride.sqlite.UserDAO;

public class LoginTaiKhoanActivity extends AppCompatActivity {
    private ImageView imgBackLoginActivity, imgHideShowPass;
    private EditText edtPhone, edtPassword;
    private CheckBox cbLuuTaiKhoan;
    private Button btnDangNhap;
    private TextView tvDangKy, tvQuenPass;
    private boolean isPasswordVisible = false;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_taikhoan);

        initViews();
        sharedPreferences = getSharedPreferences("login_check", MODE_PRIVATE);
        loadSavedAccount();

        btnDangNhap.setOnClickListener(v -> handleLogin());
        tvDangKy.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        tvQuenPass.setOnClickListener(v -> startActivity(new Intent(this, QuenPassActivity.class)));
        imgBackLoginActivity.setOnClickListener(v -> finish());
        imgHideShowPass.setOnClickListener(v -> togglePasswordVisibility());
    }

    private void initViews() {
        imgBackLoginActivity = findViewById(R.id.imgBackLoginActivity);
        imgHideShowPass = findViewById(R.id.imgHideShowPass);
        edtPhone = findViewById(R.id.edtPhone);
        edtPassword = findViewById(R.id.edtPassword);
        cbLuuTaiKhoan = findViewById(R.id.cbLuuTaiKhoan);
        btnDangNhap = findViewById(R.id.btnDangNhap);
        tvDangKy = findViewById(R.id.tvDangKy);
        tvQuenPass = findViewById(R.id.tvQuenPass);
    }

    private void loadSavedAccount() {
        if (sharedPreferences.getBoolean("remember", false)) {
            edtPhone.setText(sharedPreferences.getString("phone", ""));
            edtPassword.setText(sharedPreferences.getString("password", ""));
            cbLuuTaiKhoan.setChecked(true);
        }
    }

    private void handleLogin() {
        String phone = edtPhone.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        UserDAO userDAO = new UserDAO(this);
        if (userDAO.checkLogin(phone, password)) {
            // 1. Lưu trạng thái đăng nhập
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (cbLuuTaiKhoan.isChecked()) {
                editor.putString("phone", phone);
                editor.putString("password", password);
                editor.putBoolean("remember", true);
            } else {
                editor.clear();
                // ĐÃ SỬA: Bắt buộc phải lưu số điện thoại để làm "chìa khóa" (Session)
                // cho các màn hình khác (như Profile) biết ai đang đăng nhập
                editor.putString("phone", phone);
                editor.putBoolean("remember", false);
            }
            editor.apply();

            Toast.makeText(this, getString(R.string.message_login_success), Toast.LENGTH_SHORT).show();

            // 2. CHUYỂN SANG MAINACTIVITY
            Intent intent = new Intent(LoginTaiKhoanActivity.this, MainActivity.class);
            // Xóa hết các màn hình cũ để không quay lại được bằng nút Back
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, getString(R.string.message_login_fail), Toast.LENGTH_SHORT).show();
        }
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            edtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            imgHideShowPass.setImageResource(R.drawable.show);
        } else {
            edtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            imgHideShowPass.setImageResource(R.drawable.hide);
        }
        isPasswordVisible = !isPasswordVisible;
        edtPassword.setSelection(edtPassword.getText().length());
    }
}