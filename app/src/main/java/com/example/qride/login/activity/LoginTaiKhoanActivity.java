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
    private ImageView imgBackLoginActivity, imgFlag, imgHideShowPass;
    private TextView tvTieuDe, tvSoDienThoai, tvCountry, tvPassword, tvQuenPass, tvAccount, tvDangKy;
    private EditText edtPhone, edtPassword;
    private CheckBox cbLuuTaiKhoan;
    private Button btnDangNhap;
    private boolean isPasswordVisible = false;

    // Luu thong tin tai khoan: Luu tai khoan
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_taikhoan);
        imgBackLoginActivity = findViewById(R.id.imgBackLoginActivity);
        imgFlag = findViewById(R.id.imgFlag);
        imgHideShowPass = findViewById(R.id.imgHideShowPass);
        tvTieuDe = findViewById(R.id.tvTieuDe);
        tvSoDienThoai = findViewById(R.id.tvSoDienThoai);
        tvCountry = findViewById(R.id.tvCountry);
        tvPassword = findViewById(R.id.tvPassword);
        tvQuenPass = findViewById(R.id.tvQuenPass);
        tvAccount = findViewById(R.id.tvAccount);
        tvDangKy = findViewById(R.id.tvDangKy);
        edtPhone = findViewById(R.id.edtPhone);
        edtPassword = findViewById(R.id.edtPassword);
        cbLuuTaiKhoan = findViewById(R.id.cbLuuTaiKhoan);
        btnDangNhap = findViewById(R.id.btnDangNhap);
        togglePasswordVisibility();

        sharedPreferences = getSharedPreferences("login_check", MODE_PRIVATE);
        checkLogin();
        // Lay du lieu
        String savedPhone = sharedPreferences.getString("phone", "");
        String savePassword = sharedPreferences.getString("password", "");
        boolean isRememberAccount = sharedPreferences.getBoolean("remember", false);
        edtPhone.setText(savedPhone);
        edtPassword.setText(savePassword);
        cbLuuTaiKhoan.setChecked(isRememberAccount);

        btnDangNhap.setOnClickListener(v -> {
            String phone = edtPhone.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (phone.isEmpty()) {
                edtPhone.setError(getString(R.string.seterror_edtphone));
                return;
            }
            if (password.isEmpty()) {
                edtPassword.setError(getString(R.string.seterror_edtpassword));
                return;
            }

            UserDAO userDAO = new UserDAO(LoginTaiKhoanActivity.this);
            if (userDAO.checkLogin(phone, password)) {
                Toast.makeText(LoginTaiKhoanActivity.this,
                        getString(R.string.message_login_success),
                        Toast.LENGTH_SHORT).show();
                // Luu thong tin neu tick cnLuuTaiKhoan
                SharedPreferences.Editor editor = sharedPreferences.edit();
                if (cbLuuTaiKhoan.isChecked()) {
                    editor.putString("phone", phone);
                    editor.putString("password", password);
                    editor.putBoolean("remember", true);
                } else {
                    editor.clear();
                }
                editor.apply();

                // TODO: Chuyển sang màn hình chính
                startActivity(new Intent(LoginTaiKhoanActivity.this, MainActivity.class));
                finish();

            } else {
                Toast.makeText(LoginTaiKhoanActivity.this,
                        getString(R.string.message_login_fail),
                        Toast.LENGTH_SHORT).show();
            }
        });

        tvQuenPass.setOnClickListener(v -> {
            Intent intent = new Intent(LoginTaiKhoanActivity.this, QuenPassActivity.class);
            startActivity(intent);
        });

        backLoginActivity();
        switchRegisterActivity();
    }

    // Ham quay lai LoginActivity
    private void backLoginActivity() {
        imgBackLoginActivity.setOnClickListener(v -> {
            Intent intent = new Intent(LoginTaiKhoanActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }

    // Ham chuyen den RegisterActivity khi nhan tvDangKy
    private void switchRegisterActivity() {
        tvDangKy.setOnClickListener(v -> {
            Intent intent = new Intent(LoginTaiKhoanActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    // Ham cho phep hien thi password
    private void togglePasswordVisibility() {
        imgHideShowPass.setOnClickListener(v -> {
            if (isPasswordVisible) {
                // An mat khau
                edtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                imgHideShowPass.setImageResource(R.drawable.show);
                isPasswordVisible = false;
            } else {
                // Hien mat khau
                edtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                imgHideShowPass.setImageResource(R.drawable.hide);
                isPasswordVisible = false;
            }

            // Dat con tro ve cuoi chuoi
            edtPassword.setSelection(edtPassword.getText().length());
        });
    }

    // Ham kiem tra login - Luu tai khoan
    private void checkLogin() {
        boolean isRemembered = sharedPreferences.getBoolean("remember", false);
        if (isRemembered) {
            startActivity(new Intent(LoginTaiKhoanActivity.this, MainActivity.class));
            finish();
        }
    }

}
