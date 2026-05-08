package com.example.qride.profile;

import static com.example.qride.helper.APIHelper.CHECK_PHONE;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.R;
import com.example.qride.login.activity.QuenPassOTPActivity; // Dùng màn hình OTP quên pass bạn đã có
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText etPhoneNumber;
    private Button btnSendOTP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // 1. Ánh xạ View
        ImageView btnBack = findViewById(R.id.btnBack);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        btnSendOTP = findViewById(R.id.btnSendOTP);

        setButtonInactive();

        btnBack.setOnClickListener(v -> finish());

        // 2. Theo dõi nhập liệu để bật/tắt nút
        etPhoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String input = s.toString().trim();
                // SĐT hợp lệ thường có 10 số
                if (input.length() == 10) {
                    setButtonActive();
                } else {
                    setButtonInactive();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 3. Xử lý nút Gửi OTP
        btnSendOTP.setOnClickListener(v -> {
            String phoneInput = etPhoneNumber.getText().toString().trim();
            if (!isValidPhone(phoneInput)) {
                etPhoneNumber.setError("Số điện thoại không hợp lệ (10 số)");
                return;
            }
            checkPhoneFromServer(phoneInput);
        });
    }

    private void checkPhoneFromServer(String phone) {
        // Log để bạn kiểm tra link trong Logcat xem có đúng IP không
        String url = CHECK_PHONE + "/" + phone;
        Log.d("API_CHECK", "Connecting to: " + url);

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        boolean exists = response.getBoolean("exists");
                        if (exists) {
                            showBottomSheetOTP(phone);
                        } else {
                            etPhoneNumber.setError("Số điện thoại này chưa được đăng ký!");
                            Toast.makeText(this, "Số điện thoại không tồn tại", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Lỗi đọc dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    // Fix lỗi "Lỗi server" chung chung bằng cách hiện lỗi chi tiết
                    String errorMsg = "Không thể kết nối Server. ";
                    if (error.networkResponse != null) {
                        errorMsg += "Mã lỗi: " + error.networkResponse.statusCode;
                    } else {
                        errorMsg += "Vui lòng kiểm tra Wi-Fi/IP máy tính.";
                    }
                    Log.e("API_ERROR", error.toString());
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                }
        );
        queue.add(request);
    }

    private void showBottomSheetOTP(String phoneNumber) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_otp_selection, null);
        bottomSheetDialog.setContentView(view);

        RadioButton rbZalo = view.findViewById(R.id.rbZalo);
        RadioButton rbSMS = view.findViewById(R.id.rbSMS);
        Button btnConfirm = view.findViewById(R.id.btnConfirmOTP);

        // Thiết lập trạng thái ban đầu cho nút xác nhận trong BottomSheet
        btnConfirm.setEnabled(false);

        View.OnClickListener selectOption = v -> {
            if (v.getId() == R.id.optionZalo) rbZalo.setChecked(true);
            else rbSMS.setChecked(true);

            btnConfirm.setEnabled(true);
            btnConfirm.setBackgroundResource(R.drawable.btn_solid_green);
            btnConfirm.setTextColor(ContextCompat.getColor(this, R.color.white));
        };

        view.findViewById(R.id.optionZalo).setOnClickListener(selectOption);
        view.findViewById(R.id.optionSMS).setOnClickListener(selectOption);

        btnConfirm.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();

            // Chuyển sang màn hình QuenPassOTPActivity bạn đã viết
            Intent intent = new Intent(ChangePasswordActivity.this, QuenPassOTPActivity.class);
            intent.putExtra("phone", phoneNumber);
            intent.putExtra("verificationId", "demo_mode"); // Để khớp với logic demo trong QuenPassOTPActivity
            startActivity(intent);
        });

        bottomSheetDialog.show();
    }

    private boolean isValidPhone(String phone) {
        return phone.length() == 10 && phone.startsWith("0");
    }

    private void setButtonActive() {
        btnSendOTP.setBackgroundResource(R.drawable.btn_solid_green);
        btnSendOTP.setTextColor(ContextCompat.getColor(this, R.color.white));
        btnSendOTP.setEnabled(true);
    }

    private void setButtonInactive() {
        btnSendOTP.setBackgroundResource(R.drawable.btn_outline_green);
        btnSendOTP.setTextColor(ContextCompat.getColor(this, R.color.xanhTieuDe));
        btnSendOTP.setEnabled(false);
    }
}