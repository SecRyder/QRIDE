package com.example.qride.profile;

import static com.example.qride.helper.APIHelper.CHECK_PHONE;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.R;
import com.example.qride.sqlite.UserDAO;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

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

        etPhoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() > 0) {
                    setButtonActive();
                } else {
                    setButtonInactive();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 4. Bấm nút Gửi OTP ở màn hình chính
        btnSendOTP.setOnClickListener(v -> {
            String phoneInput = etPhoneNumber.getText().toString().trim();
            if (phoneInput.isEmpty()) {
                etPhoneNumber.setError("Vui lòng nhập số điện thoại");
                return;
            }
            checkPhoneFromServer(phoneInput);
        });
    }

    // Truyền phoneNumber vào hàm này để sử dụng
    private void showBottomSheetOTP(String phoneNumber) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_otp_selection, null);
        bottomSheetDialog.setContentView(view);

        RadioButton rbZalo = view.findViewById(R.id.rbZalo);
        RadioButton rbSMS = view.findViewById(R.id.rbSMS);
        Button btnConfirm = view.findViewById(R.id.btnConfirmOTP);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        btnConfirm.setEnabled(false);
        btnConfirm.setBackgroundResource(R.drawable.btn_outline_green);
        btnConfirm.setTextColor(ContextCompat.getColor(this, R.color.xanhTieuDe));

        Runnable activeConfirmButton = () -> {
            btnConfirm.setEnabled(true);
            btnConfirm.setBackgroundResource(R.drawable.btn_solid_green);
            btnConfirm.setTextColor(ContextCompat.getColor(this, R.color.white));
        };

        view.findViewById(R.id.optionZalo).setOnClickListener(v -> {
            rbZalo.setChecked(true);
            rbSMS.setChecked(false);
            activeConfirmButton.run();
        });

        view.findViewById(R.id.optionSMS).setOnClickListener(v -> {
            rbSMS.setChecked(true);
            rbZalo.setChecked(false);
            activeConfirmButton.run();
        });

        btnCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());

        // 6. Nút Xác nhận gửi trong BottomSheet
        btnConfirm.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Toast.makeText(this, "Đang xác thực ...", Toast.LENGTH_SHORT).show();
            // CHUYỂN THẲNG SANG OTP (KHÔNG DÙNG FIREBASE)
            Intent intent = new Intent(ChangePasswordActivity.this, com.example.qride.login.activity.RegisterOTPActivity.class);
            intent.putExtra("mode", "FORGOT_PASS");
            intent.putExtra("phone", phoneNumber);
            intent.putExtra("verificationId", "demo_mode");
            startActivity(intent);
        });

        bottomSheetDialog.show();
    }

    private void checkPhoneFromServer(String phone) {
        String url = CHECK_PHONE + phone;
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
                                    etPhoneNumber.setError("Số điện thoại chưa đăng ký!");
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        },
                        error -> {
                            Toast.makeText(this, "Lỗi server", Toast.LENGTH_SHORT).show();
                        }
                );
        queue.add(request);
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