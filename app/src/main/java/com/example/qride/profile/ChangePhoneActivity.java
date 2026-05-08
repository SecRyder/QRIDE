package com.example.qride.profile;

import static com.example.qride.helper.APIHelper.CHECK_PHONE;

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
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.R;
import com.example.qride.login.activity.RegisterOTPActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputLayout;

public class ChangePhoneActivity extends AppCompatActivity {

    private EditText etPhoneNumber;
    private TextInputLayout tilPhoneNumber;
    private Button btnSendOTP;
    private String oldPhoneNumber; // SĐT hiện tại của user
    private String newPhoneNumber; // SĐT mới chuẩn bị đổi
    private static final int REQUEST_CODE_OTP = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_phone);

        // LẤY SĐT CŨ TRỰC TIẾP TỪ SHAREDPREFERENCES ĐỂ ĐẢM BẢO KHÔNG BỊ NULL
        SharedPreferences pref = getSharedPreferences("login_check", MODE_PRIVATE);
        oldPhoneNumber = pref.getString("phone", "");

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
                // Thường SĐT là 10 số
                if (s.toString().trim().length() == 10) {
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

        if (phoneInput.isEmpty() || phoneInput.length() < 10) {
            tilPhoneNumber.setError("Vui lòng nhập số điện thoại hợp lệ (10 số)");
            return;
        }

        // Kiểm tra xem có trùng với số đang dùng không
        if (phoneInput.equals(oldPhoneNumber)) {
            tilPhoneNumber.setError("Đây là số điện thoại hiện tại của bạn!");
            return;
        }

        newPhoneNumber = phoneInput;
        checkPhoneFromServer(phoneInput);
    }

    private void checkPhoneFromServer(String phone) {
        // FIX LỖI 404: Thêm dấu "/" ở giữa
        String url = CHECK_PHONE + "/" + phone;
        Log.d("API_DEBUG", "Checking phone: " + url);

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        boolean exists = response.getBoolean("exists");
                        // Logic đổi SĐT: Nếu số mới ĐÃ CÓ người dùng khác -> Không cho đổi
                        if (exists) {
                            tilPhoneNumber.setError("Số điện thoại này đã được sử dụng bởi người khác!");
                        } else {
                            showBottomSheetOTP(phone);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    tilPhoneNumber.setError("Lỗi server hoặc mạng không ổn định");
                    Log.e("API_ERROR", error.toString());
                }
        );
        queue.add(request);
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

        View.OnClickListener selectListener = v -> {
            btnConfirm.setEnabled(true);
            btnConfirm.setBackgroundResource(R.drawable.btn_solid_green);
            btnConfirm.setTextColor(ContextCompat.getColor(this, R.color.white));

            if (v.getId() == R.id.optionZalo || v.getId() == R.id.rbZalo) {
                rbZalo.setChecked(true);
                rbSMS.setChecked(false);
            } else {
                rbSMS.setChecked(true);
                rbZalo.setChecked(false);
            }
        };

        view.findViewById(R.id.optionZalo).setOnClickListener(selectListener);
        view.findViewById(R.id.optionSMS).setOnClickListener(selectListener);
        btnCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();

            // CHUYỂN SANG MÀN HÌNH OTP (Chế độ Demo giống các bài trước)
            Intent intent = new Intent(ChangePhoneActivity.this, RegisterOTPActivity.class);
            intent.putExtra("mode", "CHANGE_PHONE");
            intent.putExtra("phone", phoneNo);
            intent.putExtra("old_phone", oldPhoneNumber);
            intent.putExtra("verificationId", "demo_mode");

            // Dùng startActivityForResult để nhận biết khi nào user đổi thành công ở màn hình kia
            startActivityForResult(intent, REQUEST_CODE_OTP);
        });

        bottomSheetDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OTP && resultCode == RESULT_OK) {
            // Khi RegisterOTPActivity gọi setResult(RESULT_OK), ta hiện dialog thành công ở đây
            showSuccessDialog("Thành công!", "Số điện thoại của bạn đã được cập nhật thành công.");
        }
    }

    private void showSuccessDialog(String title, String message) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_success_change);
        dialog.setCancelable(false);

        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvDialogMessage);

        tvTitle.setText(title);
        tvMessage.setText(message);

        // Cập nhật lại SĐT mới vào SharedPreferences để các màn hình khác nhận diện đúng
        SharedPreferences sharedPreferences = getSharedPreferences("login_check", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("phone", newPhoneNumber);
        editor.apply();

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
            // Quay về SecurityActivity và xóa các task trung gian
            Intent intent = new Intent(ChangePhoneActivity.this, SecurityActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
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