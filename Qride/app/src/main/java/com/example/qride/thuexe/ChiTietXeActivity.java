package com.example.qride.thuexe;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.MainActivity;
import com.example.qride.R;
import com.example.qride.helper.APIHelper;
import com.example.qride.profile.SupportCenterActivity;
import com.example.qride.sqlite.UserDAO;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ChiTietXeActivity extends AppCompatActivity {

    private Button btnThueXe;
    private ImageView btnBack;
    private TextView tvPlate, tvPin, tvLocation, tvStatus;

    private FrameLayout btnSupport; // robot

    private int vehicleId = -1;
    private boolean isLoading = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chitietxe);

        // Fullscreen
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        initViews();
        loadData();
        setupEvents();
    }

    private void initViews() {
        btnThueXe = findViewById(R.id.btnThueXe);
        btnBack = findViewById(R.id.btnBack);
        tvPlate = findViewById(R.id.tvPlate);
        btnSupport = findViewById(R.id.btnSupport);
        tvPin = findViewById(R.id.tvPin);
        tvLocation = findViewById(R.id.tvLocation);
        tvStatus = findViewById(R.id.tvStatus);
    }

    private void loadData() {
        Intent intent = getIntent();
        vehicleId = intent.getIntExtra("vehicleId", -1);
        String plateInput = intent.getStringExtra("plateInput");
        if (vehicleId != -1) {
            loadVehicleById(vehicleId);
        } else if (plateInput != null) {
            loadVehicleByPlate(plateInput);
        } else {
            Toast.makeText(this, "Không có dữ liệu xe", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupEvents() {
        btnBack.setOnClickListener(v -> finish());
        btnSupport.setOnClickListener(v -> {
            Intent intent = new Intent(ChiTietXeActivity.this, SupportCenterActivity.class);
            startActivity(intent);
        });

        btnThueXe.setOnClickListener(v -> {
            if (vehicleId == -1) {
                Toast.makeText(this, "Không xác định được xe", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isLoading) return;
            showPasswordBottomSheet();
        });
    }

    // ================= LOAD VEHICLE =================
    private void loadVehicleById(int id) {
        String url = APIHelper.BASE_URL + "vehicle/" + id;
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                this::showVehicleDetail,
                error -> Toast.makeText(this, "Lỗi tải xe", Toast.LENGTH_SHORT).show()
        );
        queue.add(request);
    }

    private void loadVehicleByPlate(String plate) {
        String url = APIHelper.BASE_URL + "vehicle-by-plate/" + plate;
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    showVehicleDetail(response);
                    vehicleId = response.optInt("id", -1);
                },
                error -> Toast.makeText(this, "Không tìm thấy xe", Toast.LENGTH_SHORT).show()
        );
        queue.add(request);
    }

    // ================= SHOW UI =================
    private void showVehicleDetail(JSONObject res) {
        try {
            String plate = res.getString("plate");
            String pin = res.getString("pin");
            String stationName = res.getString("station_name");
            String stationAddress = res.getString("station_address");
            String status = res.getString("current_status");

            tvPlate.setText(plate);
            tvPin.setText(pin + "%");
            tvLocation.setText(stationName + " - " + stationAddress);
            tvStatus.setText("Trạng thái: " + status);

            if (status.equalsIgnoreCase("renting")) {
                btnThueXe.setEnabled(false);
                btnThueXe.setText("Xe đang được thuê");
            } else {
                btnThueXe.setEnabled(true);
                btnThueXe.setText("Thuê xe");
            }
            checkActiveVoucher();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkActiveVoucher() {
        String url = APIHelper.ACTIVE_VOUCHER;
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    if (response != null && !response.isNull("discount_text")) {
                        String title = response.optString("title_display", "Ưu đãi");
                        String discountText = response.optString("discount_text", "");
                        Toast.makeText(ChiTietXeActivity.this,
                                "🎁 Ưu đãi \"" + title + " (" + discountText + ")\" sẽ tự động áp dụng khi kết thúc chuyến đi!",
                                Toast.LENGTH_LONG).show();
                    }
                },
                error -> android.util.Log.e("VOUCHER", "Error checking active voucher", error)
        ) {
            @Override
            public Map<String, String> getHeaders() {
                SharedPreferences pref = getSharedPreferences("login_check", MODE_PRIVATE);
                String token = pref.getString("token", null);
                Map<String, String> headers = new HashMap<>();
                if (token != null) {
                    headers.put("Authorization", "Bearer " + token);
                }
                return headers;
            }
        };
        queue.add(request);
    }

    // ================= RENT =================
    private void rentVehicle() {
        isLoading = true;
        btnThueXe.setEnabled(false);
        String url = APIHelper.BASE_URL + "rent";
        JSONObject json = new JSONObject();
        try {
            SharedPreferences pref = getSharedPreferences("login_check", MODE_PRIVATE);
            String phone = pref.getString("phone", null);

            if (phone == null) {
                Toast.makeText(this, "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
                isLoading = false;
                btnThueXe.setEnabled(true);
                return;
            }

            json.put("vehicleId", vehicleId);
            json.put("phone", phone);

        } catch (Exception e) {
            e.printStackTrace();
        }

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                json,
                response -> {
                    isLoading = false;
                    if ("SUCCESS".equals(response.optString("message"))) {
                        SharedPreferences pref = getSharedPreferences("ride_state", MODE_PRIVATE);
                        pref.edit()
                                .putBoolean("isRiding", true)
                                .putInt("vehicleId", vehicleId)
                                .putLong("startTime", System.currentTimeMillis())
                                .apply();
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.putExtra("isRiding", true);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        String msg = response.optString("message");

                        if ("NOT_ENOUGH_MONEY".equals(msg)) {
                            int balance = response.optInt("balance");
                            int need = response.optInt("need");

                            Toast.makeText(this,
                                    "Ví không đủ tiền (" + balance + "đ / cần " + need + "đ)",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                        }
                        btnThueXe.setEnabled(true);
                    }
                },
                error -> {
                    isLoading = false;
                    btnThueXe.setEnabled(true);
                    if (error.networkResponse != null) {
                        String data = new String(error.networkResponse.data);
                        System.out.println("SERVER ERROR: " + data);
                    }
                    error.printStackTrace();
                    Toast.makeText(this, "Lỗi server", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                SharedPreferences pref = getSharedPreferences("login_check", MODE_PRIVATE);
                String token = pref.getString("token", null);
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                if (token != null) {
                    headers.put("Authorization", "Bearer " + token);
                }
                return headers;
            }
        };
        queue.add(request);
    }

    private void showPasswordBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_password_confirm, null);
        EditText edtPassword = view.findViewById(R.id.edtPassword);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnConfirm = view.findViewById(R.id.btnConfirm);
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String password = edtPassword.getText().toString().trim();
            if (password.isEmpty()) {
                edtPassword.setError("Nhập mật khẩu");
                return;
            }
            SharedPreferences pref = getSharedPreferences("login_check", MODE_PRIVATE);
            String phone = pref.getString("phone", null);
            if (phone == null) {
                Toast.makeText(this, "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
                return;
            }
            verifyPasswordAPI(phone, password, dialog);
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void verifyPasswordAPI(String phone, String password, BottomSheetDialog dialog) {
        String url = APIHelper.BASE_URL + "login";
        RequestQueue queue = Volley.newRequestQueue(this);
        JSONObject body = new JSONObject();
        try {
            body.put("phone", phone);
            body.put("password", password);
        } catch (Exception e) {
            e.printStackTrace();
        }
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                body,
                response -> {
                    if ("Login success".equals(response.optString("message"))) {
                        String token = response.optString("token");
                        SharedPreferences pref = getSharedPreferences("login_check", MODE_PRIVATE);
                        pref.edit().putString("token", token).apply();
                        dialog.dismiss();
                        rentVehicle();
                    } else {
                        Toast.makeText(this, "Sai mật khẩu", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    // Xem chi tiết lỗi từ Server trả về trong Logcat
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        String errorData = new String(error.networkResponse.data);
                        android.util.Log.e("VERIFY_PASS", "Server Response: " + errorData);
                    }
                    Toast.makeText(this, "Lỗi xác thực (401)", Toast.LENGTH_SHORT).show();
                }
        );
        queue.add(request);
    }
}