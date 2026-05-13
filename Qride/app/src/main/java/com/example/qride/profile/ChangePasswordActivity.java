package com.example.qride.profile;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.R;
import com.example.qride.helper.APIHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ChangePasswordActivity extends AppCompatActivity {

    private TextInputEditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private TextInputLayout tilOldPassword, tilNewPassword, tilConfirmPassword;
    private Button btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        ImageView btnBack = findViewById(R.id.btnBack);
        tilOldPassword = findViewById(R.id.tilOldPassword);
        tilNewPassword = findViewById(R.id.tilNewPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etCurrentPassword = findViewById(R.id.etOldPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSubmit = findViewById(R.id.btnSavePassword);

        btnBack.setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> attemptChangePassword());

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkInputs();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etCurrentPassword.addTextChangedListener(watcher);
        etNewPassword.addTextChangedListener(watcher);
        etConfirmPassword.addTextChangedListener(watcher);

        checkInputs();
    }

    private void checkInputs() {
        boolean valid = etCurrentPassword.getText().length() >= 8 &&
                etNewPassword.getText().length() >= 8 &&
                etConfirmPassword.getText().length() >= 8;
        btnSubmit.setEnabled(valid);
        btnSubmit.setAlpha(valid ? 1.0f : 0.5f);
    }

    private void attemptChangePassword() {
        // Clear lỗi cũ trước khi validate
        tilOldPassword.setError(null);
        tilNewPassword.setError(null);
        tilConfirmPassword.setError(null);

        String current = etCurrentPassword.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();
        String confirm = etConfirmPassword.getText().toString().trim();

        // 1. Kiểm tra định dạng mật khẩu mới
        // - Ít nhất 1 chữ hoa, 1 chữ thường, 1 số, 1 ký tự đặc biệt
        // - Độ dài từ 8 đến 20
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_]).{8,20}$";

        if (!newPass.matches(passwordPattern)) {
            tilNewPassword.setError("Mật khẩu 8-20 ký tự, có hoa, thường, số và ký tự đặc biệt (!@#...)");
            etNewPassword.requestFocus();
            return;
        }

        // 2. Check mật khẩu mới không được giống mật khẩu cũ
        if (newPass.equals(current)) {
            tilNewPassword.setError("Mật khẩu mới không được trùng với mật khẩu cũ");
            etNewPassword.requestFocus();
            return;
        }

        // 3. Check xác nhận mật khẩu
        if (!newPass.equals(confirm)) {
            tilConfirmPassword.setError("Xác nhận mật khẩu không khớp");
            etConfirmPassword.requestFocus();
            return;
        }

        // Tiến hành gọi API
        sendRequest(current, newPass);
    }

    private void sendRequest(String current, String newPass) {
        String token = APIHelper.getToken(this);
        btnSubmit.setEnabled(false); // Disable nút để tránh bấm nhiều lần

        JSONObject body = new JSONObject();
        try {
            body.put("currentPassword", current);
            body.put("newPassword", newPass);
        } catch (Exception e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                APIHelper.CHANGE_PASSWORD,
                body,
                response -> {
                    Toast.makeText(this, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                },
                error -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setAlpha(1.0f);

                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            // Chuyển Byte Array từ Server trả về thành String JSON
                            String errorResponse = new String(error.networkResponse.data, "UTF-8");
                            JSONObject obj = new JSONObject(errorResponse);
                            String message = obj.optString("message");

                            if (error.networkResponse.statusCode == 401 || "WRONG_PASSWORD".equals(message)) {
                                tilOldPassword.setError("Mật khẩu hiện tại không đúng");
                                etCurrentPassword.requestFocus();
                            } else {
                                Toast.makeText(this, "Lỗi: " + message, Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(this, "Lỗi server không xác định", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Không thể kết nối máy chủ", Toast.LENGTH_SHORT).show();
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };
        Volley.newRequestQueue(this).add(request);
    }
}