package com.example.qride.login.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.qride.R;
import com.example.qride.sqlite.UserDAO;

public class QuenPassResetActivity extends AppCompatActivity {
    private ImageView imgBackLoginActivity, imgHideShowPass, imgHideShowPassNhapLai;
    private EditText edtPassword, edtPasswordNhapLai;
    private Button btnSavePass;
    private TextView tvRuleLength, tvRuleCase, tvRuleSpecial;

    private boolean isPasswordVisible = false;
    private boolean isPasswordNhapLaiVisible = false;
    private String phone; // lấy từ Intent

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_datlaipass);

        initViews();
        phone = getIntent().getStringExtra("phone");

        imgBackLoginActivity.setOnClickListener(v -> {
            Intent intent = new Intent(QuenPassResetActivity.this,LoginTaiKhoanActivity.class);
            startActivity(intent);
        });
        imgHideShowPass.setOnClickListener(v -> togglePasswordVisibility());
        imgHideShowPassNhapLai.setOnClickListener(v -> togglePasswordNhapLaiVisibility());
        btnSavePass.setOnClickListener(v -> handleSavePassword());
    }

    private void initViews() {
        imgBackLoginActivity = findViewById(R.id.imgBackLoginActivity);
        imgHideShowPass = findViewById(R.id.imgHideShowPass);
        imgHideShowPassNhapLai = findViewById(R.id.imgHideShowPassNhapLai);
        edtPassword = findViewById(R.id.edtPassword);
        edtPasswordNhapLai = findViewById(R.id.edtPasswordNhapLai);
        btnSavePass = findViewById(R.id.btnSavePass);
        tvRuleLength = findViewById(R.id.tvRuleLength);
        tvRuleCase = findViewById(R.id.tvRuleCase);
        tvRuleSpecial = findViewById(R.id.tvRuleSpecial);
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

    private void togglePasswordNhapLaiVisibility() {
        if (isPasswordNhapLaiVisible) {
            edtPasswordNhapLai.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            imgHideShowPassNhapLai.setImageResource(R.drawable.show);
        } else {
            edtPasswordNhapLai.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            imgHideShowPassNhapLai.setImageResource(R.drawable.hide);
        }
        isPasswordNhapLaiVisible = !isPasswordNhapLaiVisible;
        edtPasswordNhapLai.setSelection(edtPasswordNhapLai.getText().length());
    }

    private void handleSavePassword() {
        String password = edtPassword.getText().toString().trim();
        String passwordNhapLai = edtPasswordNhapLai.getText().toString().trim();

        // Kiểm tra rỗng
        if (password.isEmpty()) {
            edtPassword.setError(getString(R.string.seterror_edtpassword));
            return;
        }
        if (passwordNhapLai.isEmpty()) {
            edtPasswordNhapLai.setError(getString(R.string.seterror_edtpassword));
            return;
        }

        // Kiểm tra điều kiện mật khẩu
        if (!isValidPassword(password)) {
            edtPassword.setError(getString(R.string.seterror_edtpassword_invalid));
            return;
        }

        // Kiểm tra nhập lại
        if (!password.equals(passwordNhapLai)) {
            edtPasswordNhapLai.setError(getString(R.string.seterror_edtpassword_invalid));
            return;
        }

        // Lưu vào DB
        UserDAO userDAO = new UserDAO(this);
        boolean result = userDAO.updatePassword(phone, password);

        if (result) {
            Toast.makeText(this, getString(R.string.success_authen), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginTaiKhoanActivity.class));
            finish();
        } else {
            Toast.makeText(this, getString(R.string.db_save_fail), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidPassword(String password) {
        String regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+]).{8,20}$";
        return password.matches(regex);
    }
}
