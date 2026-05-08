package com.example.qride.login.activity;

import static com.example.qride.helper.APIHelper.REGISTER;

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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.R;
import com.example.qride.profile.ChangePhoneActivity;
import com.example.qride.profile.ResetPasswordActivity;
import com.example.qride.sqlite.UserDAO;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import org.json.JSONObject;

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
        if (mode == null) mode = "REGISTER";
        if (phone == null) phone = "";
        if (password == null) password = "";
        if (name == null) name = "";
        if (cccd == null) cccd = "";
        if (address == null) address = "";
        if (gender == null) gender = "Khác";
        if (birthday == null) birthday = "";
    }

    private void setupUI() {
        String fullPhone = phone.length() >= 10 ? "+84" + phone.substring(1) : phone;
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

    private void handleChangePhoneAPI() {
        String url = com.example.qride.helper.APIHelper.CHANGE_PHONE; // Đảm bảo APIHelper đã có CHANGE_PHONE
        RequestQueue queue = Volley.newRequestQueue(this);
        JSONObject body = new JSONObject();

        try {
            // Lấy SĐT cũ được truyền từ ChangePhoneActivity qua Intent
            String oldPhone = getIntent().getStringExtra("old_phone");
            // SĐT mới chính là biến 'phone' đã có sẵn trong class
            String newPhone = phone;

            body.put("oldPhone", oldPhone);
            body.put("newPhone", newPhone);
        } catch (Exception e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body,
                response -> {
                    // Khi Server báo cập nhật Database thành công
                    Toast.makeText(this, "Cập nhật số điện thoại thành công!", Toast.LENGTH_SHORT).show();

                    // Trả kết quả về cho ChangePhoneActivity để nó hiện Dialog xác nhận
                    setResult(RESULT_OK);
                    finish();
                },
                error -> {
                    // Mặc định là lỗi 401 chung
                    String message = "Lỗi xác thực (401)";

                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        try {
                            // Cố gắng đọc nội dung chi tiết từ Server gửi về (ví dụ: {"message": "Token expired"})
                            String body2 = new String(error.networkResponse.data, "UTF-8");
                            JSONObject res = new JSONObject(body2);
                            if (res.has("message")) {
                                message = res.getString("message");
                            } else {
                                message = "Lỗi " + statusCode + ": " + body2;
                            }
                        } catch (Exception e) {
                            message = "Lỗi " + statusCode + " (Không thể đọc nội dung)";
                        }
                    } else {
                        message = "Không kết nối được Server";
                    }

                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    Log.e("API_DEBUG_ERROR", message);
                }
        ) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();

                com.example.qride.sqlite.UserDAO dao = new com.example.qride.sqlite.UserDAO(RegisterOTPActivity.this);

                String token = dao.getToken();

                android.util.Log.d("TOKEN_DEBUG", "Token lay tu SQLite: [" + token + "]");

                if (token != null && !token.isEmpty()) {
                    token = token.replace("\"", "").trim();
                    headers.put("Authorization", "Bearer " + token);
                }

                return headers;
            }
        };
        queue.add(request);
    }

    private void handleFinalAction() {
        Toast.makeText(this, getString(R.string.success_authen), Toast.LENGTH_SHORT).show();
        if ("REGISTER".equals(mode)) {

            String url = REGISTER;

            RequestQueue queue = Volley.newRequestQueue(this);
            JSONObject body = new JSONObject();

            try {
                body.put("phone", phone);
                body.put("password", password);
                body.put("name", name);
                body.put("cccd", cccd);
                body.put("address", address);
                body.put("gender", gender);
                body.put("birthday", convertDateFormat(birthday));
            } catch (Exception e) {
                e.printStackTrace();
            }
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    body,
                    response -> {

                        FirebaseAuth.getInstance().signOut();

                        Toast.makeText(this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();

                        startActivity(new Intent(this, LoginTaiKhoanActivity.class));
                        finish();
                    },
                    error -> {if (error.networkResponse != null) {
                        int code = error.networkResponse.statusCode;
                        String data = new String(error.networkResponse.data);

                        if (data.contains("PHONE_EXISTS")) {
                            Toast.makeText(this, "SĐT đã tồn tại", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Lỗi server: " + code, Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(this, "Không kết nối server", Toast.LENGTH_SHORT).show();
                    }
                    }
            );

            queue.add(request);
        }
        else if ("CHANGE_PHONE".equals(mode)) {
            handleChangePhoneAPI();
        }
    }

    private String convertDateFormat(String inputDate) {
        try {
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("dd/MM/yyyy");
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");

            java.util.Date date = inputFormat.parse(inputDate);
            return outputFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return inputDate; // fallback
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
                public void onVerificationCompleted(PhoneAuthCredential credential) {
                }

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