//package com.example.qride.login.activity;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.text.InputType;
//import android.util.Log;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.ImageView;
//import android.widget.RadioGroup;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.activity.EdgeToEdge;
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.example.qride.R;
//import com.google.firebase.FirebaseException;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.PhoneAuthCredential;
//import com.google.firebase.auth.PhoneAuthOptions;
//import com.google.firebase.auth.PhoneAuthProvider;
//
//import java.util.concurrent.TimeUnit;
//
//public class RegisterActivity extends AppCompatActivity {
//    private ImageView imgBackLoginActivity, imgFlag, imgHideShowPass, imgHideShowPassNhapLai;
//    private TextView tvTieuDe, tvSoDienThoai, tvCountry, tvPassword, tvQuenPass, tvAccount, tvDangNhap, tvPasswordNhapLai;
//
//    // ĐÃ THÊM CÁC BIẾN CHO PROFILE
//    private EditText edtPhone, edtPassword, edtPasswordNhapLai, etName, etCccd, etAddress, etBirthday;
//    private RadioGroup rgGender;
//
//    private Button btnDangKy;
//
//    private boolean isPasswordVisible = false;
//    private boolean isPasswordNhapLaiVisible = false;
//
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_register);
//
//        imgBackLoginActivity = findViewById(R.id.imgBackLoginActivity);
//        imgHideShowPass = findViewById(R.id.imgHideShowPass);
//        imgHideShowPassNhapLai = findViewById(R.id.imgHideShowPassNhapLai);
//        tvDangNhap = findViewById(R.id.tvDangNhap);
//
//        edtPhone = findViewById(R.id.edtPhone);
//        edtPassword = findViewById(R.id.edtPassword);
//        edtPasswordNhapLai = findViewById(R.id.edtPasswordNhapLai);
//
//        // TODO: Ánh xạ các trường Profile (Đảm bảo trong XML activity_register có các ID này)
//        etName = findViewById(R.id.etName);
//        etCccd = findViewById(R.id.etCccd);
//        etAddress = findViewById(R.id.etAddress);
//        etBirthday = findViewById(R.id.etBirthday);
//        // giao diện mở lịch để chọn ngay tháng năm
//        etBirthday.setOnClickListener(v -> {
//            java.util.Calendar calendar = java.util.Calendar.getInstance();
//            int year = calendar.get(java.util.Calendar.YEAR);
//            int month = calendar.get(java.util.Calendar.MONTH);
//            int day = calendar.get(java.util.Calendar.DAY_OF_MONTH);
//
//            android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
//                    this,
//                    (view, selectedYear, selectedMonth, selectedDay) -> {
//                        String formattedDate = String.format("%02d/%02d/%04d", selectedDay, (selectedMonth + 1), selectedYear);
//                        etBirthday.setText(formattedDate);
//                    },
//                    year, month, day
//            );
//            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
//            datePickerDialog.show();
//        });
//
//        rgGender = findViewById(R.id.rgGender);
//
//        btnDangKy = findViewById(R.id.btnDangKy);
//
//        backLoginActivity();
//        switchLoginTaiKhoanActivity();
//        setupRegister();
//        togglePasswordVisibility();
//        togglePasswordNhapLaiVisibility();
//    }
//
//    private void backLoginActivity() {
//        imgBackLoginActivity.setOnClickListener(v -> finish());
//    }
//
//    private void switchLoginTaiKhoanActivity() {
//        tvDangNhap.setOnClickListener(v -> {
//            Intent intent = new Intent(RegisterActivity.this, LoginTaiKhoanActivity.class);
//            startActivity(intent);
//        });
//    }
//
//    private void setupRegister() {
//        btnDangKy.setOnClickListener(v -> {
//            String phone = edtPhone.getText().toString().trim();
//            String password = edtPassword.getText().toString();
//            String passwordNhapLai = edtPasswordNhapLai.getText().toString();
//
//            // Lấy thêm dữ liệu profile
//            String name = etName != null ? etName.getText().toString().trim() : "";
//            String cccd = etCccd != null ? etCccd.getText().toString().trim() : "";
//            String address = etAddress != null ? etAddress.getText().toString().trim() : "";
//            String birthday = etBirthday != null ? etBirthday.getText().toString().trim() : "";
//
//            // Dùng một biến tạm để lấy giá trị
//            String tempGender = "Khác";
//            if(rgGender != null) {
//                int selectedId = rgGender.getCheckedRadioButtonId();
//                if (selectedId == R.id.rbMale) tempGender = "Nam";
//                else if (selectedId == R.id.rbFemale) tempGender = "Nữ";
//            }
//
//            //  giá trị bằng một biến final
//            final String finalGender = tempGender;
//
//            // Kiểm tra hợp lệ cơ bản
//            if (phone.isEmpty() || name.isEmpty() || cccd.isEmpty()) {
//                Toast.makeText(this, "Vui lòng nhập đủ các thông tin bắt buộc!", Toast.LENGTH_SHORT).show();
//                return;
//            }
//            if (!isValidPhone(phone)) {
//                edtPhone.setError(getString(R.string.seterror_edtphone_invalid));
//                return;
//            }
//            if (!isValidPassword(password)) {
//                edtPassword.setError(getString(R.string.seterror_edtpassword_invalid));
//                return;
//            }
//            if (!password.equals(passwordNhapLai)) {
//                edtPasswordNhapLai.setError(getString(R.string.seterror_edtpassword_invalid));
//                return;
//            }
//
//            // Nếu hợp lệ → gọi Firebase gửi OTP
//            FirebaseAuth auth = FirebaseAuth.getInstance();
//            PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
//                    .setPhoneNumber("+84" + phone)
//                    .setTimeout(60L, TimeUnit.SECONDS)
//                    .setActivity(RegisterActivity.this)
//                    .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
//                        @Override
//                        public void onVerificationCompleted(PhoneAuthCredential credential) { }
//
//                        @Override
//                        public void onVerificationFailed(FirebaseException e) {
//                            Toast.makeText(RegisterActivity.this, "Lỗi gửi OTP: " + e.getMessage(), Toast.LENGTH_LONG).show();
//                        }
//
//                        @Override
//                        public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
//                            Intent intent = new Intent(RegisterActivity.this, RegisterOTPActivity.class);
//                            intent.putExtra("verificationId", verificationId);
//                            intent.putExtra("resendToken", token);
//                            // Truyền TOÀN BỘ thông tin sang màn hình OTP
//                            intent.putExtra("phone", phone);
//                            intent.putExtra("password", password);
//                            intent.putExtra("name", name);
//                            intent.putExtra("cccd", cccd);
//                            intent.putExtra("address", address);
//                            intent.putExtra("gender", finalGender);
//                            intent.putExtra("birthday", birthday);
//                            startActivity(intent);
//                        }
//                    })
//                    .build();
//
//            PhoneAuthProvider.verifyPhoneNumber(options);
//        });
//    }
//
//    private boolean isValidPassword(String password) {
//        String regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+]).{8,20}$";
//        return password.matches(regex);
//    }
//
//    private boolean isValidPhone(String phone) {
//        String regex = "^[0-9]{9,9}$";
//        return phone.matches(regex);
//    }
//
//    private void togglePasswordVisibility() {
//        imgHideShowPass.setOnClickListener(v -> {
//            if (isPasswordVisible) {
//                edtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
//                imgHideShowPass.setImageResource(R.drawable.show);
//                isPasswordVisible = false;
//            } else {
//                edtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
//                imgHideShowPass.setImageResource(R.drawable.hide);
//                isPasswordVisible = true; // SỬA LỖI LOGIC CŨ CỦA BẠN: Phải set thành true
//            }
//            edtPassword.setSelection(edtPassword.getText().length());
//        });
//    }
//
//    private void togglePasswordNhapLaiVisibility() {
//        imgHideShowPassNhapLai.setOnClickListener(v -> {
//            if (isPasswordNhapLaiVisible) {
//                edtPasswordNhapLai.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
//                imgHideShowPassNhapLai.setImageResource(R.drawable.show);
//                isPasswordNhapLaiVisible = false;
//            } else {
//                edtPasswordNhapLai.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
//                imgHideShowPassNhapLai.setImageResource(R.drawable.hide);
//                isPasswordNhapLaiVisible = true;
//            }
//            edtPasswordNhapLai.setSelection(edtPasswordNhapLai.getText().length());
//        });
//    }
//}

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

            String name = etName != null ? etName.getText().toString().trim() : "";
            String cccd = etCccd != null ? etCccd.getText().toString().trim() : "";
            String address = etAddress != null ? etAddress.getText().toString().trim() : "";
            String birthday = etBirthday != null ? etBirthday.getText().toString().trim() : "";

            String tempGender = "Khác";
            if(rgGender != null) {
                int selectedId = rgGender.getCheckedRadioButtonId();
                if (selectedId == R.id.rbMale) tempGender = "Nam";
                else if (selectedId == R.id.rbFemale) tempGender = "Nữ";
            }
            final String finalGender = tempGender;

            // ====================================================
            // KIỂM TRA ĐIỀU KIỆN (VALIDATION)
            // ====================================================

            // 1. Kiểm tra Họ và Tên (bắt buộc)
            if (name.isEmpty()) {
                etName.setError("Họ và tên không được để trống!");
                etName.requestFocus();
                return;
            }

            // 2. Kiểm tra CCCD (bắt buộc, đúng 12 số)
            if (cccd.isEmpty() || cccd.length() != 12) {
                etCccd.setError("CCCD bắt buộc phải bao gồm đúng 12 chữ số!");
                etCccd.requestFocus();
                return;
            }

            // 3. Kiểm tra Ngày sinh (bắt buộc)
            if (birthday.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ngày sinh!", Toast.LENGTH_SHORT).show();
                etBirthday.requestFocus();
                return;
            }

            // 4. Kiểm tra Số điện thoại
            if (rawPhone.isEmpty()) {
                edtPhone.setError("Vui lòng nhập số điện thoại!");
                edtPhone.requestFocus();
                return;
            }

            // Xử lý tự động loại bỏ số 0 ở đầu nếu người dùng nhập vào
            String phone = rawPhone;
            if (phone.startsWith("0")) {
                phone = phone.substring(1);
            }

            // Số điện thoại Việt Nam sau khi bỏ số 0 phải có đúng 9 số
            if (phone.length() != 9) {
                edtPhone.setError("Số điện thoại sau +84 phải có đúng 9 số!");
                edtPhone.requestFocus();
                return;
            }

            // 5. Kiểm tra Mật khẩu (8-20 ký tự, có số, chữ hoa, thường, đặc biệt)
            if (!isValidPassword(password)) {
                edtPassword.setError("Mật khẩu 8-20 ký tự, gồm chữ hoa, thường, số và ký tự đặc biệt!");
                edtPassword.requestFocus();
                return;
            }

            // 6. Kiểm tra Nhập lại Mật khẩu
            if (!password.equals(passwordNhapLai)) {
                edtPasswordNhapLai.setError("Mật khẩu nhập lại không khớp!");
                edtPasswordNhapLai.requestFocus();
                return;
            }

            // ====================================================
            // ĐÃ HỢP LỆ -> GỌI FIREBASE GỬI OTP
            // ====================================================

            // Biến phone truyền đi phải dùng biến cuối cùng đã gỡ số 0
            final String phoneToSend = phone;

            FirebaseAuth auth = FirebaseAuth.getInstance();
            PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber("+84" + phoneToSend)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(RegisterActivity.this)
                    .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        @Override
                        public void onVerificationCompleted(PhoneAuthCredential credential) { }

                        @Override
                        public void onVerificationFailed(FirebaseException e) {
                            Toast.makeText(RegisterActivity.this, "Lỗi gửi OTP: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                            Intent intent = new Intent(RegisterActivity.this, RegisterOTPActivity.class);
                            intent.putExtra("verificationId", verificationId);
                            intent.putExtra("resendToken", token);
                            // Truyền TOÀN BỘ thông tin sang màn hình OTP
                            intent.putExtra("phone", phoneToSend);
                            intent.putExtra("password", password);
                            intent.putExtra("name", name);
                            intent.putExtra("cccd", cccd);
                            intent.putExtra("address", address);
                            intent.putExtra("gender", finalGender);
                            intent.putExtra("birthday", birthday);
                            startActivity(intent);
                        }
                    })
                    .build();

            PhoneAuthProvider.verifyPhoneNumber(options);
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