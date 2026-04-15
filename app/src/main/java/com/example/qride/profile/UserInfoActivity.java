package com.example.qride.profile;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.qride.R;
import com.example.qride.sqlite.UserDAO;

public class UserInfoActivity extends AppCompatActivity {

    private EditText etName, etCccd, etAddress, etBirthday;
    private RadioGroup rgGender;
    private Button btnUpdate;
    private UserDAO userDAO;

    // Biến lưu số điện thoại đang đăng nhập
    private String currentUserPhone = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        // Ánh xạ giao diện
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        etName = findViewById(R.id.etName);
        etCccd = findViewById(R.id.etCccd);
        etAddress = findViewById(R.id.etAddress);
        etBirthday = findViewById(R.id.etBirthday);
        // giao diện mở lịch để chọn ngày tháng năm
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
        btnUpdate = findViewById(R.id.btnUpdate);

        userDAO = new UserDAO(this);

        // 1. LẤY SỐ ĐIỆN THOẠI ĐANG ĐĂNG NHẬP
        SharedPreferences sharedPreferences = getSharedPreferences("login_check", MODE_PRIVATE);
        currentUserPhone = sharedPreferences.getString("phone", "");

        if (currentUserPhone.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không tìm thấy tài khoản đăng nhập!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. LOAD DỮ LIỆU TỪ DATABASE LÊN GIAO DIỆN
        loadUserData();

        // 3. XỬ LÝ KHI BẤM NÚT "CẬP NHẬT"
        btnUpdate.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String cccd = etCccd.getText().toString().trim();
            String address = etAddress.getText().toString().trim();
            String birthday = etBirthday.getText().toString().trim();

            String gender = "Khác";
            int selectedId = rgGender.getCheckedRadioButtonId();
            if (selectedId == R.id.rbMale) {
                gender = "Nam";
            } else if (selectedId == R.id.rbFemale) {
                gender = "Nữ";
            }

            if (name.isEmpty() || cccd.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ Tên và CCCD!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Cập nhật vào DB
            boolean isSuccess = userDAO.updateUserInfo(currentUserPhone, name, cccd, address, gender, birthday);

            if (isSuccess) {
                Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                finish(); // Cập nhật xong thì tự động đóng màn hình này
            } else {
                Toast.makeText(this, "Cập nhật thất bại!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Hàm lấy thông tin user từ CSDL và điền lên UI
    private void loadUserData() {
        Cursor cursor = userDAO.getUserInfo(currentUserPhone);
        if (cursor != null && cursor.moveToFirst()) {

            // Lấy vị trí (index) của các cột trong Database
            int nameIdx = cursor.getColumnIndex("name");
            int cccdIdx = cursor.getColumnIndex("cccd");
            int addressIdx = cursor.getColumnIndex("address");
            int genderIdx = cursor.getColumnIndex("gender");
            int birthdayIdx = cursor.getColumnIndex("birthday");

            // Điền dữ liệu vào các ô EditText
            if (nameIdx != -1) etName.setText(cursor.getString(nameIdx));
            if (cccdIdx != -1) etCccd.setText(cursor.getString(cccdIdx));
            if (addressIdx != -1) etAddress.setText(cursor.getString(addressIdx));
            if (birthdayIdx != -1) etBirthday.setText(cursor.getString(birthdayIdx));

            // Chọn đúng giới tính trên RadioGroup
            if (genderIdx != -1) {
                String gender = cursor.getString(genderIdx);
                if ("Nam".equals(gender)) {
                    rgGender.check(R.id.rbMale);
                } else if ("Nữ".equals(gender)) {
                    rgGender.check(R.id.rbFemale);
                } else {
                    rgGender.check(R.id.rbOther);
                }
            }
            cursor.close();
        }
    }
}