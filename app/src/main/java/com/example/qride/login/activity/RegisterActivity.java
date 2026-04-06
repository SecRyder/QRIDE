package com.example.qride.login.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.qride.R;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class RegisterActivity extends AppCompatActivity {
    private ImageView imgBackLoginActivity, imgFlag, imgHideShowPass, imgHideShowPassNhapLai;
    private TextView tvTieuDe, tvSoDienThoai, tvCountry, tvPassword, tvQuenPass, tvAccount, tvDangNhap, tvPasswordNhapLai, tvRuleLength, tvRuleCase, tvRuleSpecial;
    private EditText edtPhone, edtPassword, edtPasswordNhapLai;
    private Button btnDangKy;

    private boolean isPasswordVisible = false;
    private boolean isPasswordNhapLaiVisible = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        imgBackLoginActivity = findViewById(R.id.imgBackLoginActivity);
        imgFlag = findViewById(R.id.imgFlag);
        imgHideShowPass = findViewById(R.id.imgHideShowPass);
        imgHideShowPassNhapLai = findViewById(R.id.imgHideShowPassNhapLai);
        tvTieuDe = findViewById(R.id.tvTieuDe);
        tvSoDienThoai = findViewById(R.id.tvSoDienThoai);
        tvCountry = findViewById(R.id.tvCountry);
        tvPassword = findViewById(R.id.tvPassword);
        tvQuenPass = findViewById(R.id.tvQuenPass);
        tvAccount = findViewById(R.id.tvAccount);
        tvDangNhap = findViewById(R.id.tvDangNhap);
        tvPasswordNhapLai = findViewById(R.id.tvPasswordNhapLai);
        tvRuleLength = findViewById(R.id.tvRuleLength);
        tvRuleCase = findViewById(R.id.tvRuleCase);
        tvRuleSpecial = findViewById(R.id.tvRuleSpecial);
        edtPhone = findViewById(R.id.edtPhone);
        edtPassword = findViewById(R.id.edtPassword);
        edtPasswordNhapLai = findViewById(R.id.edtPasswordNhapLai);
        btnDangKy = findViewById(R.id.btnDangKy);

        backLoginActivity();
        switchLoginTaiKhoanActivity();
        setupRegister(); // ham xu ly dang ky
        togglePasswordVisibility();
        togglePasswordNhapLaiVisibility();
    }

    // Ham quay lai LoginActivity
    private void backLoginActivity() {
        imgBackLoginActivity.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }

    // Ham chuyen den LoginTaiKhoanActivity khi nhan tvDangNhap
    private void switchLoginTaiKhoanActivity() {
        tvDangNhap.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginTaiKhoanActivity.class);
            startActivity(intent);
        });
    }

    // Ham xu ly nut Dang ky
//    private void setupRegister() {
//        btnDangKy.setOnClickListener(v -> {
//            String phone = edtPhone.getText().toString().trim();
//            String password = edtPassword.getText().toString();
//            String passwordNhapLai = edtPasswordNhapLai.getText().toString();
//
//            // Kiem tra so dien thoai
//            if (phone.isEmpty()) {
//                edtPhone.setError("Vui lòng nhập số điện thoại");
//                return;
//            }
//            if (!isValidPhone(phone)) {
//                edtPhone.setError("Số điện thoại không hợp lệ");
//            }
//
//            // Kiem tra mat khau
//            if (!isValidPassword(password)) {
//                edtPassword.setError("Mật khẩu không hợp lệ");
//                return;
//            }
//
//            // Kiem tra nhap lai mat khau
//            if (!password.equals(passwordNhapLai)) {
//                edtPasswordNhapLai.setError("Mật khẩu nhập lại không khớp");
//                return;
//            }
//            // Neu hop le chuyen den RegisterOTPActivity
//            Intent intent = new Intent(RegisterActivity.this, RegisterOTPActivity.class);
//            intent.putExtra("phone", phone);
//            startActivity(intent);
//        });
//    }

    private void setupRegister() {
        btnDangKy.setOnClickListener(v -> {
            String phone = edtPhone.getText().toString().trim();
            String password = edtPassword.getText().toString();
            String passwordNhapLai = edtPasswordNhapLai.getText().toString();

            // Kiểm tra hợp lệ
            if (phone.isEmpty()) {
                edtPhone.setError(getString(R.string.seterror_edtphone));
                return;
            }
            if (!isValidPhone(phone)) {
                edtPhone.setError(getString(R.string.seterror_edtphone_invalid));
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

            // Nếu hợp lệ → gọi Firebase gửi OTP
            FirebaseAuth auth = FirebaseAuth.getInstance();

            PhoneAuthOptions options =
                    PhoneAuthOptions.newBuilder(auth)
                            .setPhoneNumber("+84" + phone)       // số điện thoại
                            .setTimeout(60L, TimeUnit.SECONDS)
                            .setActivity(RegisterActivity.this)
                            .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                @Override
                                public void onVerificationCompleted(PhoneAuthCredential credential) {
                                    auth.signInWithCredential(credential)
                                            .addOnCompleteListener(task -> {
                                                if (task.isSuccessful()) {
                                                    // Đăng ky thành công
                                                }
                                            });
                                }

                                @Override
                                public void onVerificationFailed(FirebaseException e) {
                                    Log.e("RegisterActivity", "Verification failed", e);
                                    Toast.makeText(RegisterActivity.this, "Lỗi gửi OTP: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }

                                @Override
                                public void onCodeAutoRetrievalTimeOut(@NonNull String s) {
                                    Log.e("RegisterOTPActivity", "Auto retrieval timeout: " + s);
                                }

                                @Override
                                public void onCodeSent(String verificationId,
                                                       PhoneAuthProvider.ForceResendingToken token) {
                                    Intent intent = new Intent(RegisterActivity.this, RegisterOTPActivity.class);
                                    intent.putExtra("verificationId", verificationId);
                                    intent.putExtra("resendToken", token);
                                    intent.putExtra("phone", phone);
                                    intent.putExtra("password", password); // thêm password để lưu vào DB
                                    startActivity(intent);
                                }

                            })
                            .build();

            PhoneAuthProvider.verifyPhoneNumber(options);
        });
    }


    // Ham kiem tra mat khau hop le
    private boolean isValidPassword(String password) {
        String regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+]).{8,20}$";
        return password.matches(regex);
    }

    // Ham kiem tra so dien thoai
    private boolean isValidPhone(String phone) {
        String regex = "^[0-9]{9,9}$";
        return phone.matches(regex);
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

    // Ham cho phep hien thi passwordNhapLai
    private void togglePasswordNhapLaiVisibility() {
        imgHideShowPassNhapLai.setOnClickListener(v -> {
            if (isPasswordNhapLaiVisible) {
                edtPasswordNhapLai.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                imgHideShowPassNhapLai.setImageResource(R.drawable.show);
                isPasswordNhapLaiVisible = false;
            } else {
                edtPasswordNhapLai.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                imgHideShowPassNhapLai.setImageResource(R.drawable.hide);
                isPasswordNhapLaiVisible = true;
            }
            edtPasswordNhapLai.setSelection(edtPasswordNhapLai.getText().length());
        });
    }
}
