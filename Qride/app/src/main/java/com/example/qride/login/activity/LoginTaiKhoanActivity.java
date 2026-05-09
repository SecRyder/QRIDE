package com.example.qride.login.activity;

import static com.example.qride.helper.APIHelper.LOGIN;

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

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.MainActivity;
import com.example.qride.R;
import com.example.qride.sqlite.UserDAO;

import org.json.JSONObject;

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

    // ================= LOGIN API =================
    private void handleLogin() {

        String phone = edtPhone.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        // ===== VALIDATE =====
        if (phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = LOGIN;

        JSONObject json = new JSONObject();
        try {
            json.put("phone", phone);
            json.put("password", password);
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi tạo dữ liệu", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                json,

                // ===== SUCCESS =====
                response -> {
                    try {
                        // ===== VALIDATE RESPONSE =====
                        if (!response.has("token") || !response.has("user")) {
                            Toast.makeText(this, "Response không hợp lệ", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String token = response.optString("token", null);
                        JSONObject userObj = response.optJSONObject("user");
                        if (userObj == null) {
                            Toast.makeText(this, "User null từ server", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        int userId = userObj.optInt("id", -1);

                        if (userId == -1) {
                            Toast.makeText(this, "User ID không tồn tại", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        // ===== SAVE SQLITE (SESSION CHUẨN) =====
                        UserDAO dao = new UserDAO(this);
                        dao.saveUserSession(userId, phone, token);

                        // ===== SHARED PREF (LƯU TRẠNG THÁI ĐĂNG NHẬP) =====
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("phone", phone);
                        editor.putBoolean("remember", cbLuuTaiKhoan.isChecked());

                        if (cbLuuTaiKhoan.isChecked()) {
                            editor.putString("password", password);
                        } else {
                            editor.remove("password");
                        }

                        editor.apply();

                        Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();

                        // ===== CHUYỂN MÀN =====
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(LoginTaiKhoanActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                },

                // ===== ERROR =====
                error -> {
                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;

                        if (statusCode == 401) {
                            Toast.makeText(this, "Sai tài khoản hoặc mật khẩu", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Lỗi server: " + statusCode, Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(this, "Không kết nối được server", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        request.setRetryPolicy(new DefaultRetryPolicy(
                10000, // timeout 10s
                2,     // retry 2 lần
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        queue.add(request);
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