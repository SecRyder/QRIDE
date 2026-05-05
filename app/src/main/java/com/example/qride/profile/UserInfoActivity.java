package com.example.qride.profile;

import static com.example.qride.helper.APIHelper.UPDATE;
import static com.example.qride.helper.APIHelper.USER;
import static com.example.qride.helper.APIHelper.getToken;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.R;
import com.example.qride.sqlite.UserDAO;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

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
            String birthdayRaw = etBirthday.getText().toString().trim();
            String birthday = convertToISODate(birthdayRaw);
            String gender = "Khác";
            int selectedId = rgGender.getCheckedRadioButtonId();
            if (selectedId == R.id.rbMale) gender = "Nam";
            else if (selectedId == R.id.rbFemale) gender = "Nữ";
            if (name.isEmpty() || cccd.isEmpty()) {
                Toast.makeText(this, "Nhập đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }
            updateUserAPI(name, cccd, address, gender, birthday);
        });
    }

    private String convertToISODate(String input) {
        try {
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("dd/MM/yyyy");
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
            java.util.Date date = inputFormat.parse(input);
            return outputFormat.format(date);
        } catch (Exception e) {
            return "";
        }
    }

    private void updateUserAPI(String name, String cccd, String address, String gender, String birthday) {

        String url = UPDATE;
        String token = getToken(this);
        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "Token lỗi, vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(this);

        JSONObject body = new JSONObject();
        try {
            body.put("name", name);
            body.put("cccd", cccd);
            body.put("address", address);
            body.put("gender", gender);
            if (!birthday.isEmpty()) {
                body.put("birthday", birthday);
            }
        } catch (Exception e) {
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                body,
                response -> {
                    if ("SUCCESS".equals(response.optString("message"))) {
                        Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Thất bại", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Lỗi server", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        queue.add(request);
    }

    private void loadUserData() {
        String url = USER;
        String token = getToken(this);
        if (token == null || token.isEmpty()) {
            Log.d("TOKEN_DEBUG", "Token = " + token);
            Toast.makeText(this, "Token lỗi, vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            return;
        }
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    etName.setText(response.optString("name"));
                    etCccd.setText(response.optString("cccd"));
                    etAddress.setText(response.optString("address"));
                    etBirthday.setText(formatDateFromAPI(response.optString("birthday")));

                    String gender = response.optString("gender");
                    if ("Nam".equals(gender)) rgGender.check(R.id.rbMale);
                    else if ("Nữ".equals(gender)) rgGender.check(R.id.rbFemale);
                    else rgGender.check(R.id.rbOther);
                },
                error -> {
                    if (error.networkResponse != null) {
                        int code = error.networkResponse.statusCode;
                        String data = new String(error.networkResponse.data);
                        Log.e("API_ERROR", code + " | " + data);
                        Toast.makeText(this, "Lỗi: " + code, Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("API_ERROR", error.toString());
                        Toast.makeText(this, "Không kết nối server", Toast.LENGTH_SHORT).show();
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        queue.add(request);
    }

    private String formatDateFromAPI(String isoDate) {
        try {
            java.text.SimpleDateFormat input =
                    new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            java.text.SimpleDateFormat output =
                    new java.text.SimpleDateFormat("dd/MM/yyyy");

            java.util.Date date = input.parse(isoDate);
            return output.format(date);
        } catch (Exception e) {
            return "";
        }
    }
}