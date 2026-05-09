package com.example.qride.login.activity;

import static com.example.qride.helper.APIHelper.CHECK_PHONE;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.R;
import com.example.qride.sqlite.UserDAO;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

public class QuenPassActivity extends AppCompatActivity {
    private ImageView imgBackLoginActivity, imgFlag;
    private TextView tvTieuDe, tvSoDienThoai, tvCountry;
    private EditText edtPhone;
    private Button btnGuiOTP;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_quenpass);
        imgBackLoginActivity = findViewById(R.id.imgBackLoginActivity);
        imgFlag = findViewById(R.id.imgFlag);
        tvTieuDe = findViewById(R.id.tvTieuDe);
        tvSoDienThoai = findViewById(R.id.tvSoDienThoai);
        tvCountry = findViewById(R.id.tvCountry);
        edtPhone = findViewById(R.id.edtPhone);
        btnGuiOTP = findViewById(R.id.btnGuiOTP);
        imgBackLoginActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(QuenPassActivity.this, LoginTaiKhoanActivity.class);
                startActivity(intent);
            }
        });

        btnGuiOTP.setOnClickListener(v -> handleSendOtp());
    }

    private void handleSendOtp() {
        String phone = edtPhone.getText().toString().trim();
        if (phone.isEmpty()) {
            edtPhone.setError(getString(R.string.seterror_edtphone));
            return;
        }
        if (!isValidPhone(phone)) {
            edtPhone.setError(getString(R.string.seterror_edtphone_invalid));
            return;
        }
        // ================= CALL SERVER CHECK PHONE =================
        String url = CHECK_PHONE;
        RequestQueue queue = Volley.newRequestQueue(this);
        JSONObject body = new JSONObject();
        try {
            body.put("phone", phone);
        } catch (Exception e) {
            e.printStackTrace();
        }
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                body,
                response -> {
                    try {
                        boolean exists = response.getBoolean("exists");

                        if (!exists) {
                            edtPhone.setError("Số điện thoại chưa đăng ký");
                            return;
                        }
                        // ================= SEND OTP =================
                        sendOTP(phone);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Lỗi server", Toast.LENGTH_SHORT).show()
        );
        queue.add(request);
    }

    private void sendOTP(String phone) {
        String phoneFirebase = "+84" + phone.substring(1);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(phoneFirebase)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                            @Override
                            public void onVerificationCompleted(PhoneAuthCredential credential) { }

                            @Override
                            public void onVerificationFailed(FirebaseException e) {
                                Toast.makeText(QuenPassActivity.this,
                                        "Lỗi OTP: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onCodeSent(String verificationId,
                                                   PhoneAuthProvider.ForceResendingToken token) {

                                Intent intent = new Intent(QuenPassActivity.this, QuenPassOTPActivity.class);
                                intent.putExtra("verificationId", verificationId);
                                intent.putExtra("resendToken", token);
                                intent.putExtra("phone", phone);
                                startActivity(intent);
                            }
                        })
                        .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private boolean isValidPhone(String phone) {
        String regex = "^0[0-9]{9}$"; // 10 số, bắt đầu bằng 0
        return phone.matches(regex);
    }
}
