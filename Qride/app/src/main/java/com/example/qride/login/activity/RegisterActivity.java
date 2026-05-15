package com.example.qride.login.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
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
    private TextView tvTieuDe, tvSoDienThoai, tvCountry, tvPassword, tvQuenPass, tvAccount, tvDangNhap, tvPasswordNhapLai;

    private EditText edtPhone, edtPassword, edtPasswordNhapLai, etName, etCccd, etAddress, etBirthday;
    private RadioGroup rgGender;

    private Button btnDangKy;

    private boolean isPasswordVisible = false;
    private boolean isPasswordNhapLaiVisible = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        imgBackLoginActivity = findViewById(R.id.imgBackLoginActivity);
        imgHideShowPass = findViewById(R.id.imgHideShowPass);
        imgHideShowPassNhapLai = findViewById(R.id.imgHideShowPassNhapLai);
        tvDangNhap = findViewById(R.id.tvDangNhap);

        edtPhone = findViewById(R.id.edtPhone);
        edtPassword = findViewById(R.id.edtPassword);
        edtPasswordNhapLai = findViewById(R.id.edtPasswordNhapLai);

        etName = findViewById(R.id.etName);
        etCccd = findViewById(R.id.etCccd);
        etAddress = findViewById(R.id.etAddress);
        etBirthday = findViewById(R.id.etBirthday);

        // Giao diện mở lịch để chọn ngày tháng năm
        etBirthday.setOnClickListener(v -> {
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            int year = calendar.get(java.util.Calendar.YEAR);
            int month = calendar.get(java.util.Calendar.MONTH);
            int day = calendar.get(java.util.Calendar.DAY_OF_MONTH);

            android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                    this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        String formattedDate = String.format("%02d/%02d/%04d", selectedDay, (selectedMonth + 1), selectedYear);
                        etBirthday.setText(formattedDate);
                    },
                    year, month, day
            );
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            datePickerDialog.show();
        });

        rgGender = findViewById(R.id.rgGender);
        btnDangKy = findViewById(R.id.btnDangKy);

        backLoginActivity();
        switchLoginTaiKhoanActivity();
        setupRegister();
        togglePasswordVisibility();
        togglePasswordNhapLaiVisibility();
    }

    private void backLoginActivity() {
        imgBackLoginActivity.setOnClickListener(v -> finish());
    }

    private void switchLoginTaiKhoanActivity() {
        tvDangNhap.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginTaiKhoanActivity.class);
            startActivity(intent);
        });
    }

    private void setupRegister() {
        btnDangKy.setOnClickListener(v -> {
            String rawPhone = edtPhone.getText().toString().trim();
            String password = edtPassword.getText().toString();
            String passwordNhapLai = edtPasswordNhapLai.getText().toString();
            String name = etName.getText().toString().trim();
            String cccd = etCccd.getText().toString().trim();
            String address = etAddress.getText().toString().trim();
            String birthday = etBirthday.getText().toString().trim();

            // 1. Kiểm tra Họ tên
            if (name.isEmpty()) {
                etName.setError("Họ tên không được để trống");
                etName.requestFocus();
                return;
            }

            // 2. Kiểm tra Địa chỉ
            if (address.isEmpty()) {
                etAddress.setError("Địa chỉ không được để trống");
                etAddress.requestFocus();
                return;
            }

            // 3. Kiểm tra CCCD
            if (cccd.length() != 12) {
                etCccd.setError("CCCD phải đúng 12 số");
                etCccd.requestFocus();
                return;
            }

            // 4. Kiểm tra Ngày sinh
            if (birthday.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ngày sinh", Toast.LENGTH_SHORT).show();
                return;
            }

            // 5. Kiểm tra Số điện thoại (Regex chuẩn 10 số Việt Nam)
            if (!rawPhone.matches("^0\\d{9}$")) {
                edtPhone.setError("SĐT phải gồm 10 số và bắt đầu bằng số 0");
                edtPhone.requestFocus();
                return;
            }

            // 6. Kiểm tra Mật khẩu
            if (!isValidPassword(password)) {
                edtPassword.setError("Mật khẩu 8-20 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt");
                edtPassword.requestFocus();
                return;
            }

            // 7. Kiểm tra Nhập lại mật khẩu
            if (!password.equals(passwordNhapLai)) {
                edtPasswordNhapLai.setError("Mật khẩu nhập lại không khớp");
                edtPasswordNhapLai.requestFocus();
                return;
            }

            // Giới tính
            String tempGender = "Khác";
            int selectedId = rgGender.getCheckedRadioButtonId();
            if (selectedId == R.id.rbMale) tempGender = "Nam";
            else if (selectedId == R.id.rbFemale) tempGender = "Nữ";

            // CHUYỂN SANG MÀN HÌNH OTP
            Intent intent = new Intent(RegisterActivity.this, RegisterOTPActivity.class);
            intent.putExtra("mode", "REGISTER");
            intent.putExtra("phone", rawPhone);
            intent.putExtra("password", password);
            intent.putExtra("name", name);
            intent.putExtra("cccd", cccd);
            intent.putExtra("address", address);
            intent.putExtra("gender", tempGender);
            intent.putExtra("birthday", birthday);
            startActivity(intent);

            Toast.makeText(this, "Đang gửi mã xác thực ...", Toast.LENGTH_SHORT).show();
        });
    }


    // Biểu thức Regex kiểm tra Password (hoàn toàn khớp với yêu cầu của bạn)
    private boolean isValidPassword(String password) {
        String regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+]).{8,20}$";
        return password.matches(regex);
    }

    private void togglePasswordVisibility() {
        imgHideShowPass.setOnClickListener(v -> {
            if (isPasswordVisible) {
                edtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                imgHideShowPass.setImageResource(R.drawable.show);
                isPasswordVisible = false;
            } else {
                edtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                imgHideShowPass.setImageResource(R.drawable.hide);
                isPasswordVisible = true;
            }
            edtPassword.setSelection(edtPassword.getText().length());
        });
    }

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