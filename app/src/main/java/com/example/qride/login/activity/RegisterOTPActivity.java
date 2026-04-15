package com.example.qride.login.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.qride.R;
import com.example.qride.profile.ChangePhoneActivity;
import com.example.qride.profile.ResetPasswordActivity;
import com.example.qride.sqlite.UserDAO;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class RegisterOTPActivity extends AppCompatActivity {

    private ImageView imgBackRegisterActivity;
    private TextView tvHuongDan, tvResend;
    private EditText otp1, otp2, otp3, otp4, otp5, otp6;
    private Button btnConfirmOtp;

    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private final String DEMO_OTP = "210404";

    private String mode;
    private String phone, password, name, cccd, address, gender, birthday;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register_otp);

        initView();
        getDataFromIntent();
        setupUI();
        setupEvents();
    }

    private void initView() {
        imgBackRegisterActivity = findViewById(R.id.imgBackRegisterActivity);
        tvHuongDan = findViewById(R.id.tvHuongDan);
        tvResend = findViewById(R.id.tvResend);
        otp1 = findViewById(R.id.otp1);
        otp2 = findViewById(R.id.otp2);
        otp3 = findViewById(R.id.otp3);
        otp4 = findViewById(R.id.otp4);
        otp5 = findViewById(R.id.otp5);
        otp6 = findViewById(R.id.otp6);
        btnConfirmOtp = findViewById(R.id.btnConfirmOtp);
    }

    private void getDataFromIntent() {
        verificationId = getIntent().getStringExtra("verificationId");
        resendToken = (PhoneAuthProvider.ForceResendingToken) getIntent().getSerializableExtra("resendToken");
        mode = getIntent().getStringExtra("mode");
        phone = getIntent().getStringExtra("phone");
        password = getIntent().getStringExtra("password");
        name = getIntent().getStringExtra("name");
        cccd = getIntent().getStringExtra("cccd");
        address = getIntent().getStringExtra("address");
        gender = getIntent().getStringExtra("gender");
        birthday = getIntent().getStringExtra("birthday");
    }

    private void setupUI() {
        String fullPhone = "+84" + phone;
        tvHuongDan.setText(getString(R.string.otp_instruction, fullPhone));
        startResendCountdown();
        simulateOtpReceive();
    }

    private void setupEvents() {
        imgBackRegisterActivity.setOnClickListener(v -> finish());

        btnConfirmOtp.setOnClickListener(v -> {
            String code = getOtpCode();

            if (code.length() != 6) {
                Toast.makeText(this, getString(R.string.otp), Toast.LENGTH_SHORT).show();
                return;
            }

            // Ưu tiên kiểm tra mã Demo để tránh lỗi Firebase Billing
            if (code.equals(DEMO_OTP)) {
                handleFinalAction();
            } else {
                if (verificationId == null) {
                    Toast.makeText(this, getString(R.string.faile_authen), Toast.LENGTH_SHORT).show();
                    return;
                }

                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
                FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                handleFinalAction();
                            } else {
                                Toast.makeText(this, "Mã xác thực không chính xác hoặc lỗi!", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        tvResend.setOnClickListener(v -> {
            if (!tvResend.isEnabled()) return;
            if (resendToken == null || phone == null) {
                Toast.makeText(this, getString(R.string.otp_resend_fail), Toast.LENGTH_SHORT).show();
                return;
            }
            PhoneAuthOptions options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                    .setPhoneNumber("+84" + phone)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(this)
                    .setForceResendingToken(resendToken)
                    .setCallbacks(callbacks)
                    .build();
            PhoneAuthProvider.verifyPhoneNumber(options);
            Toast.makeText(this, getString(R.string.otp_resend_sending), Toast.LENGTH_SHORT).show();
            startResendCountdown();
        });
    }

    private void handleFinalAction() {
        Toast.makeText(this, getString(R.string.success_authen), Toast.LENGTH_SHORT).show();

        if ("FORGOT_PASS".equals(mode)) {
            Intent intent = new Intent(this, ResetPasswordActivity.class);
            intent.putExtra("phone", phone);
            startActivity(intent);
            finish();

        } else if ("VERIFY_OLD_PHONE".equals(mode)) {
            Intent intent = new Intent(RegisterOTPActivity.this, ChangePhoneActivity.class);
            intent.putExtra("current_phone", phone);
            startActivity(intent);
            finish();

        } else if ("CHANGE_PHONE".equals(mode)) {
            String sdtMoi = phone;
            String sdtCu = getIntent().getStringExtra("old_phone");

            // --- BƯỚC 1: LÀM SẠCH TRIỆT ĐỂ (Cực kỳ quan trọng) ---
            // Xóa sạch mã vùng +84, khoảng trắng và số 0 ở đầu để đồng bộ với DB (987654321)
            if (sdtCu != null) {
                sdtCu = sdtCu.replace("+84", "").trim();
                if (sdtCu.startsWith("0")) sdtCu = sdtCu.substring(1);
            }
            if (sdtMoi != null) {
                sdtMoi = sdtMoi.replace("+84", "").trim();
                if (sdtMoi.startsWith("0")) sdtMoi = sdtMoi.substring(1);
            }

            Log.d("SQL_DEBUG", "Đang thử Update DB - Cũ: [" + sdtCu + "] -> Mới: [" + sdtMoi + "]");

            UserDAO userDAO = new UserDAO(this);
            boolean isUpdated = userDAO.updatePhoneNumber(sdtCu, sdtMoi);

            // --- BƯỚC 2: KHÔNG CHẤP NHẬN FAKE SUCCESS NỮA ---
            // Bắt buộc SQLite phải báo update thành công (isUpdated = true) thì mới lưu Session và hiện Dialog
            if (isUpdated) {
                getSharedPreferences("UserSession", MODE_PRIVATE)
                        .edit()
                        .putString("phone", sdtMoi)
                        .apply();

                setResult(RESULT_OK); // Tín hiệu hiện bảng xe đạp
                finish();
            } else {
                // Nếu nhảy vào đây, chắc chắn DB không tìm thấy số cũ
                Toast.makeText(this, "Lỗi DB: Không tìm thấy số [" + sdtCu + "] để cập nhật!", Toast.LENGTH_LONG).show();
                Log.e("SQL_DEBUG", "UPDATE THẤT BẠI DO KHÔNG TÌM THẤY SỐ CŨ!");
            }

        } else {
            // Luồng đăng ký tài khoản
            UserDAO userDAO = new UserDAO(RegisterOTPActivity.this);
            long result = userDAO.insertUser(phone, password, name, cccd, address, gender, birthday);
            if (result != -1) {
                Toast.makeText(this, getString(R.string.db_save_success), Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginTaiKhoanActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Lỗi lưu Database!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getOtpCode() {
        return otp1.getText().toString().trim() + otp2.getText().toString().trim() +
                otp3.getText().toString().trim() + otp4.getText().toString().trim() +
                otp5.getText().toString().trim() + otp6.getText().toString().trim();
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onVerificationCompleted(PhoneAuthCredential credential) {}
                @Override
                public void onVerificationFailed(FirebaseException e) {
                    Log.e("OTP_Debug", "Firebase Error: " + e.getMessage());
                }
                @Override
                public void onCodeSent(String newVerificationId, PhoneAuthProvider.ForceResendingToken token) {
                    verificationId = newVerificationId;
                    resendToken = token;
                }
            };

    private void startResendCountdown() {
        tvResend.setEnabled(false);
        new CountDownTimer(30000, 1000) {
            public void onTick(long millisUntilFinished) {
                tvResend.setText(getString(R.string.otp_resend_countdown, millisUntilFinished / 1000));
            }
            public void onFinish() {
                tvResend.setText(getString(R.string.guima_otp_finish));
                tvResend.setEnabled(true);
            }
        }.start();
    }

    private void simulateOtpReceive() {
        new android.os.Handler().postDelayed(() -> {
            Toast.makeText(this, getString(R.string.otp_demo_message, DEMO_OTP), Toast.LENGTH_LONG).show();
            fillOtp(DEMO_OTP);
        }, 2000);
    }

    private void fillOtp(String otp) {
        if (otp != null && otp.length() == 6) {
            otp1.setText(String.valueOf(otp.charAt(0)));
            otp2.setText(String.valueOf(otp.charAt(1)));
            otp3.setText(String.valueOf(otp.charAt(2)));
            otp4.setText(String.valueOf(otp.charAt(3)));
            otp5.setText(String.valueOf(otp.charAt(4)));
            otp6.setText(String.valueOf(otp.charAt(5)));
            otp6.requestFocus();
            otp6.setSelection(1);
        }
    }
}