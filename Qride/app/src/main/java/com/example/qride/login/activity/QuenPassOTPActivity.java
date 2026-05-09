package com.example.qride.login.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.qride.R;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class QuenPassOTPActivity extends AppCompatActivity {
    private ImageView imgBackRegisterActivity;
    private TextView tvHuongDan, tvResend;
    private EditText otp1, otp2, otp3, otp4, otp5, otp6;
    private Button btnConfirmOtp;

    private String verificationId;
    private String phone;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private final String DEMO_OTP = "210404";

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
        resendToken = (PhoneAuthProvider.ForceResendingToken)
                getIntent().getSerializableExtra("resendToken");
        phone = getIntent().getStringExtra("phone");
    }

    private void setupUI() {
        if (phone == null) {
            Toast.makeText(this, "Thiếu dữ liệu", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String fullPhone = "+84" + phone.substring(1);
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

            // ===== DEMO OTP =====
            if (code.equals(DEMO_OTP)) {
                goToReset();
                return;
            }

            if (verificationId == null) {
                Toast.makeText(this, getString(R.string.faile_authen), Toast.LENGTH_SHORT).show();
                return;
            }

            PhoneAuthCredential credential =
                    PhoneAuthProvider.getCredential(verificationId, code);

            FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            goToReset();
                        } else {
                            Toast.makeText(this, getString(R.string.otp_fail), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        tvResend.setOnClickListener(v -> {
            if (!tvResend.isEnabled()) return;
            if (resendToken == null || phone == null) {
                Toast.makeText(this, getString(R.string.otp_resend_fail), Toast.LENGTH_SHORT).show();
                return;
            }
            PhoneAuthOptions options =
                    PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                            .setPhoneNumber("+84" + phone.substring(1))
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
    private void goToReset() {
        Toast.makeText(this, getString(R.string.success_authen), Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, QuenPassResetActivity.class);
        intent.putExtra("phone", phone);
        startActivity(intent);
        finish();
    }
    private String getOtpCode() {
        return otp1.getText().toString().trim() +
                otp2.getText().toString().trim() +
                otp3.getText().toString().trim() +
                otp4.getText().toString().trim() +
                otp5.getText().toString().trim() +
                otp6.getText().toString().trim();
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onVerificationCompleted(PhoneAuthCredential credential) {
                    FirebaseAuth.getInstance().signInWithCredential(credential);
                }

                @Override
                public void onVerificationFailed(FirebaseException e) {
                    Toast.makeText(QuenPassOTPActivity.this,
                            getString(R.string.otp_send_fail, e.getMessage()),
                            Toast.LENGTH_LONG).show();
                }

                @Override
                public void onCodeSent(String newVerificationId,
                                       PhoneAuthProvider.ForceResendingToken token) {
                    verificationId = newVerificationId;
                    resendToken = token;
                    Toast.makeText(QuenPassOTPActivity.this,
                            getString(R.string.otp_resend_success),
                            Toast.LENGTH_SHORT).show();
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

            // Hiển thị OTP bằng Toast (10 giây)
            Toast toast = Toast.makeText(
                    QuenPassOTPActivity.this,
                    getString(R.string.otp_demo_message, DEMO_OTP),
                    Toast.LENGTH_LONG
            );
            toast.show();

            // Giữ Toast hiển thị ~10s (show lại nhiều lần)
            new android.os.Handler().postDelayed(toast::show, 3500);
            new android.os.Handler().postDelayed(toast::show, 7000);

            // Auto điền OTP vào các ô
            fillOtp(DEMO_OTP);

        }, 2000);
    }

    private void fillOtp(String otp) {
        if (otp.length() == 6) {
            otp1.setText(String.valueOf(otp.charAt(0)));
            otp2.setText(String.valueOf(otp.charAt(1)));
            otp3.setText(String.valueOf(otp.charAt(2)));
            otp4.setText(String.valueOf(otp.charAt(3)));
            otp5.setText(String.valueOf(otp.charAt(4)));
            otp6.setText(String.valueOf(otp.charAt(5)));
        }
    }
}
