package com.example.qride.profile;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.qride.R;
import com.example.qride.sqlite.UserDAO;
import com.google.android.material.textfield.TextInputLayout;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etOldPassword, etNewPassword, etConfirmPassword;
    private TextInputLayout tilOld, tilNew, tilConfirm;
    private Button btnSavePassword;
    private String phoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        // Lấy SĐT truyền từ màn hình OTP qua
        phoneNumber = getIntent().getStringExtra("phone");

        initView();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnSavePassword.setOnClickListener(v -> handleResetPassword());
    }

    private void initView() {
        // Ánh xạ các EditText bên trong
        etOldPassword = findViewById(R.id.etOldPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        // Ánh xạ các bộ khung TextInputLayout bọc ngoài
        tilOld = findViewById(R.id.tilOldPassword);
        tilNew = findViewById(R.id.tilNewPassword);
        tilConfirm = findViewById(R.id.tilConfirmPassword);

        btnSavePassword = findViewById(R.id.btnSavePassword);
    }

    private void handleResetPassword() {
        // 1. Xóa tất cả các thông báo lỗi cũ trên TextInputLayout
        tilOld.setError(null);
        tilNew.setError(null);
        tilConfirm.setError(null);

        String oldPass = etOldPassword.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();

        // 2. Kiểm tra Mật khẩu cũ
        if (oldPass.isEmpty()) {
            tilOld.setError("Vui lòng nhập mật khẩu hiện tại!");
            etOldPassword.requestFocus();
            return;
        }

        // 3. Kiểm tra Mật khẩu mới (Ràng buộc: 8-20 ký tự, hoa, thường, số, đặc biệt)
        if (newPass.isEmpty()) {
            tilNew.setError("Vui lòng nhập mật khẩu mới!");
            etNewPassword.requestFocus();
            return;
        }

        if (!isValidPassword(newPass)) {
            tilNew.setError("Mật khẩu 8-20 ký tự, gồm chữ hoa, thường, số và ký tự đặc biệt!");
            etNewPassword.requestFocus();
            return;
        }

        // 4. Kiểm tra Nhập lại mật khẩu mới
        if (confirmPass.isEmpty()) {
            tilConfirm.setError("Vui lòng xác nhận lại mật khẩu mới!");
            etConfirmPassword.requestFocus();
            return;
        }

        if (!newPass.equals(confirmPass)) {
            tilConfirm.setError("Mật khẩu xác nhận không khớp!");
            etConfirmPassword.requestFocus();
            return;
        }

        // 5. Kiểm tra mật khẩu mới không được trùng mật khẩu cũ
        if (newPass.equals(oldPass)) {
            tilNew.setError("Mật khẩu mới không được giống mật khẩu cũ!");
            etNewPassword.requestFocus();
            return;
        }

        // 6. Gọi Database để cập nhật
        UserDAO userDAO = new UserDAO(this);
        boolean isUpdated = userDAO.updatePassword(phoneNumber, oldPass, newPass);

        if (isUpdated) {
//            Toast.makeText(this, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
//            finish();
            showSuccessDialog("Thành công!", "Mật khẩu của bạn đã được\ncập nhật thành công.");
        } else {
            // Hiển thị lỗi mật khẩu cũ ngay trên khung TextInputLayout
            tilOld.setError("Mật khẩu cũ không chính xác!");
            etOldPassword.requestFocus();
        }
    }



    private boolean isValidPassword(String password) {
        String regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+]).{8,20}$";
        return password.matches(regex);
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

            // 1. Tạo Intent để chuyển hướng về SecurityActivity
            Intent intent = new Intent(ResetPasswordActivity.this, SecurityActivity.class);


            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            startActivity(intent);

            finish(); // Đóng màn hình thay đổi số điện thoại và quay về SecurityActivity
        });

        dialog.show();
    }


}