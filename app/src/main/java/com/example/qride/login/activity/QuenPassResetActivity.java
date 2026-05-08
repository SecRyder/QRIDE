package com.example.qride.login.activity;

import static com.example.qride.helper.APIHelper.RESET_PASSWORD;

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

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.R;
import com.example.qride.sqlite.UserDAO;
import com.android.volley.RequestQueue;

import org.json.JSONObject;

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
            Intent intent = new Intent(QuenPassResetActivity.this, LoginTaiKhoanActivity.class);
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

        if (password.isEmpty()) {
            edtPassword.setError(getString(R.string.seterror_edtpassword));
            return;
        }

        if (passwordNhapLai.isEmpty()) {
            edtPasswordNhapLai.setError(getString(R.string.seterror_edtpassword));
            return;
        }

        if (!isValidPassword(password)) {
            edtPassword.setError(getString(R.string.seterror_edtpassword_invalid));
            return;
        }

        if (!password.equals(passwordNhapLai)) {
            edtPasswordNhapLai.setError(getString(R.string.seterror_edtpassword_invalid));
            return;
        }

        if (phone == null) {
            Toast.makeText(this, "Thiếu số điện thoại", Toast.LENGTH_SHORT).show();
            return;
        }

        // ================= CALL API =================
        String url = RESET_PASSWORD;
        RequestQueue queue = Volley.newRequestQueue(this);
        JSONObject body = new JSONObject();
        try {
            body.put("phone", phone);
            body.put("newPassword", password);
        } catch (Exception e) {
            e.printStackTrace();
        }
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                body,
                response -> {
                    Toast.makeText(this, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(this, LoginTaiKhoanActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                },
                error -> {
                    if (error.networkResponse != null) {
                        Toast.makeText(this, "Lỗi: " + error.networkResponse.statusCode, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Không kết nối server", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        queue.add(request);

    }

    private boolean isValidPassword(String password) {
        String regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+]).{8,20}$";
        return password.matches(regex);
    }
}
