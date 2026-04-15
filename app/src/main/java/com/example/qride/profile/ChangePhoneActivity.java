package com.example.qride.profile;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.qride.R;
import com.example.qride.login.activity.RegisterOTPActivity;
import com.example.qride.sqlite.UserDAO;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class ChangePhoneActivity extends AppCompatActivity {

    private EditText etPhoneNumber;
    private TextInputLayout tilPhoneNumber;
    private Button btnSendOTP;
    private String oldPhoneNumber;
    private String newPhoneNumber;
    // Hằng số để nhận diện kết quả trả về từ màn hình OTP
    private static final int REQUEST_CODE_OTP = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_phone);
        oldPhoneNumber = getIntent().getStringExtra("current_phone");
        initView();
        setupTextWatcher();
        btnSendOTP.setOnClickListener(v -> handleSendOTP());
    }

    private void initView() {
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        tilPhoneNumber = findViewById(R.id.tilPhoneNumber);
        btnSendOTP = findViewById(R.id.btnSendOTP);
        ImageView btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
        setButtonInactive();
    }

    private void setupTextWatcher() {
        etPhoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() >= 9) {
                    setButtonActive();
                } else {
                    setButtonInactive();
                }
                tilPhoneNumber.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void handleSendOTP() {
        String phoneInput = etPhoneNumber.getText().toString().trim();
        String phoneToVerify = phoneInput;

        if (phoneToVerify.startsWith("0")) {
            phoneToVerify = phoneToVerify.substring(1);
        }

        newPhoneNumber = phoneInput; // lưu số mới


        if (phoneToVerify.equals(oldPhoneNumber) || phoneInput.equals(oldPhoneNumber)) {
            tilPhoneNumber.setError("Đây là số điện thoại hiện tại của bạn!");
            return;
        }

        UserDAO userDAO = new UserDAO(this);
        if (userDAO.isPhoneExist(phoneInput) || userDAO.isPhoneExist(phoneToVerify)) {
            tilPhoneNumber.setError("Số điện thoại này đã được đăng ký bởi tài khoản khác!");
            return;
        }

        showBottomSheetOTP(phoneToVerify);
    }

    private void showBottomSheetOTP(String phoneNo) {
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

        View.OnClickListener selectListener = v -> {
            btnConfirm.setEnabled(true);
            btnConfirm.setBackgroundResource(R.drawable.btn_solid_green);
            btnConfirm.setTextColor(ContextCompat.getColor(this, R.color.white));
            if (v.getId() == R.id.optionZalo || v.getId() == R.id.rbZalo) {
                rbZalo.setChecked(true); rbSMS.setChecked(false);
            } else {
                rbSMS.setChecked(true); rbZalo.setChecked(false);
            }
        };

        view.findViewById(R.id.optionZalo).setOnClickListener(selectListener);
        view.findViewById(R.id.optionSMS).setOnClickListener(selectListener);
        btnCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            sendFirebaseOTP(phoneNo);
        });

        bottomSheetDialog.show();
    }

    private void sendFirebaseOTP(String phone) {
        // 1. Chuyển sang màn hình OTP luôn để dùng mã Demo
        Intent intent = new Intent(ChangePhoneActivity.this, RegisterOTPActivity.class);
        intent.putExtra("mode", "CHANGE_PHONE");
        intent.putExtra("phone", phone);
        intent.putExtra("old_phone", oldPhoneNumber);
        intent.putExtra("verificationId", "demo_mode"); // Truyền ID giả để RegisterOTP không báo null
        startActivityForResult(intent, REQUEST_CODE_OTP);

        // 2. Vẫn gọi Firebase để hệ thống hiểu là có gửi (Kệ lỗi Billing)
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                .setPhoneNumber("+84" + phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {}
                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Log.e("OTP_Debug", "Firebase báo lỗi nhưng vẫn cho dùng mã Demo");
                    }
                    @Override
                    public void onCodeSent(@NonNull String vId, @NonNull PhoneAuthProvider.ForceResendingToken token) {}
                }).build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    // PHẦN MỚI: Nhận tín hiệu từ RegisterOTPActivity trả về
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OTP && resultCode == RESULT_OK) {
            // Khi bên OTP báo thành công, hiện Dialog thông báo
            showSuccessDialog("Thành công!","Số điện thoại của bạn đã được \n cập nhật thành công. ");

        }
    }

    // PHẦN MỚI: Hiển thị Dialog thành công
    private void showSuccessDialog(String title, String message) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_success_change);
        dialog.setCancelable(false);

        // ánh xạ
        TextView tvTitle= dialog.findViewById(R.id.tvDialogTitle);
        TextView tvMessage= dialog.findViewById(R.id.tvDialogMessage);

        tvTitle.setText(title);
        tvMessage.setText(message);


        // update session trước
        SharedPreferences sharedPreferences = getSharedPreferences("login_check", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("phone", newPhoneNumber);
        editor.apply();


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

            Intent intent = new Intent(ChangePhoneActivity.this, SecurityActivity.class);

            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            startActivity(intent);

            finish(); // Đóng màn hình thay đổi số điện thoại và quay về SecurityActivity
        });

        dialog.show();
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