package com.example.qride.profile;

import static com.example.qride.helper.APIHelper.RESET_PASSWORD;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.R;
import com.example.qride.sqlite.UserDAO;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etOldPassword, etNewPassword, etConfirmPassword;
    private TextInputLayout tilOld, tilNew, tilConfirm;
    private Button btnSavePassword;
    private String phoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        // Lấy SĐT truyền từ màn hình OTP qua
        phoneNumber = getIntent().getStringExtra("phone");

        initView();
        setupPasswordWatcher();
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnSavePassword.setOnClickListener(v -> handleResetPassword());
    }

    private void initView() {
        // Ánh xạ các EditText bên trong
        etOldPassword = findViewById(R.id.etOldPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        // Ánh xạ các bộ khung TextInputLayout bọc ngoài
        tilOld = findViewById(R.id.tilOldPassword);
        tilNew = findViewById(R.id.tilNewPassword);
        tilConfirm = findViewById(R.id.tilConfirmPassword);

        btnSavePassword = findViewById(R.id.btnSavePassword);
    }

    private void handleResetPassword() {
        tilOld.setError(null);
        tilNew.setError(null);
        tilConfirm.setError(null);
        String newPass = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();

        if (newPass.isEmpty()) {
            tilNew.setError("Nhập mật khẩu mới");
            return;
        }

        if (!isValidPassword(newPass)) {
            tilNew.setError("Mật khẩu không hợp lệ");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            tilConfirm.setError("Không khớp");
            return;
        }
        sendResetPasswordAPI(newPass);
    }

    private void sendResetPasswordAPI(String newPass) {
        String url = RESET_PASSWORD;
        RequestQueue queue = Volley.newRequestQueue(this);
        JSONObject body = new JSONObject();
        try {
            body.put("phone", phoneNumber);
            body.put("newPassword", newPass);
        } catch (Exception e) {
            e.printStackTrace();
        }
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                body,
                response -> {
                    if ("SUCCESS".equals(response.optString("message"))) {
                        showSuccessDialog("Thành công", "Đã đổi mật khẩu");
                    } else {
                        Toast.makeText(this, "Lỗi server", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(this, "Lỗi kết nối server", Toast.LENGTH_SHORT).show();
                }
        );

        queue.add(request);
    }

    private boolean isValidPassword(String password) {
        String regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+]).{8,20}$";
        return password.matches(regex);
    }

    // PHẦN MỚI: Hiển thị Dialog thành công
    private void showSuccessDialog(String title, String message) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_success_change);
        dialog.setCancelable(false);

        // ánh xạ
        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvDialogMessage);

        tvTitle.setText(title);
        tvMessage.setText(message);


        // Bo góc cho background dialog
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(lp);
        }

        Button btnDone = dialog.findViewById(R.id.btnDone);
        btnDone.setOnClickListener(v -> {
            dialog.dismiss();

            // 1. Tạo Intent để chuyển hướng về SecurityActivity
            Intent intent = new Intent(ResetPasswordActivity.this, SecurityActivity.class);


            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            startActivity(intent);

            finish(); // Đóng màn hình thay đổi số điện thoại và quay về SecurityActivity
        });

        dialog.show();
    }

    private void setButtonActive() {
        btnSavePassword.setBackgroundResource(R.drawable.btn_solid_green);
        btnSavePassword.setTextColor(ContextCompat.getColor(this, R.color.white));
        btnSavePassword.setEnabled(true);
    }

    private void setButtonInactive() {
        btnSavePassword.setBackgroundResource(R.drawable.btn_outline_green);
        btnSavePassword.setTextColor(ContextCompat.getColor(this, R.color.xanhTieuDe));
        btnSavePassword.setEnabled(false);
    }

    private void setupPasswordWatcher() {
        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int oldLen = etOldPassword.getText().toString().trim().length();
                int newLen = etNewPassword.getText().toString().trim().length();
                int confirmLen = etConfirmPassword.getText().toString().trim().length();
                // ĐIỀU KIỆN: Cả 3 ô đều phải >= 8 ký tự
                if (oldLen >= 8 && newLen >= 8 && confirmLen >= 8) {
                    setButtonActive();
                } else {
                    setButtonInactive();
                }
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        };
        etOldPassword.addTextChangedListener(watcher);
        etNewPassword.addTextChangedListener(watcher);
        etConfirmPassword.addTextChangedListener(watcher);
    }


}