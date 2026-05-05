package com.example.qride.thuexe;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.R;
import com.example.qride.helper.APIHelper;

public class QRNhapMaActivity extends AppCompatActivity {
    private EditText edtNhapMaQR;
    private Button btnXemChiTiet;
    private ImageView btnBack;
    double currentLat = 0;
    double currentLng = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nhapmaqr);

        // Fullscreen giống app scan
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
        initViews();
        setupEvents();
        currentLat = getIntent().getDoubleExtra("lat", 0);
        currentLng = getIntent().getDoubleExtra("lng", 0);
    }

    private void initViews() {
        edtNhapMaQR = findViewById(R.id.edtNhapMaQR);
        btnXemChiTiet = findViewById(R.id.btnXemChiTiet);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupEvents() {
        // Nút back → quay lại scan (không tạo activity mới)
        btnBack.setOnClickListener(v -> finish());

        // Click nút xem chi tiết
        btnXemChiTiet.setOnClickListener(v -> handleSubmit());

        // Nhấn Enter trên bàn phím cũng submit
        edtNhapMaQR.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                handleSubmit();
                return true;
            }
            return false;
        });
    }

    // ================= HANDLE INPUT =================
    private void handleSubmit() {
        String plate = edtNhapMaQR.getText().toString().trim();
        if (plate.isEmpty()) {
            edtNhapMaQR.setError("Vui lòng nhập biển số!");
            edtNhapMaQR.requestFocus();
            return;
        }
        plate = plate.toUpperCase();
        btnXemChiTiet.setEnabled(false);
        String url = APIHelper.BASE_URL + "vehicle-by-plate/" + plate;
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        Intent intent = new Intent(this, ChiTietXeActivity.class);
                        int vehicleId = response.optInt("id", -1);
                        if (vehicleId == -1) {
                            edtNhapMaQR.setError("Dữ liệu lỗi");
                            btnXemChiTiet.setEnabled(true);
                            return;
                        }
                        intent.putExtra("vehicleId", vehicleId);
                        intent.putExtra("plate", response.getString("plate"));
                        intent.putExtra("pin", response.getString("pin"));
                        intent.putExtra("stationName", response.getString("station_name"));
                        intent.putExtra("stationAddress", response.getString("station_address"));
                        String status = response.getString("current_status");
                        if (!status.equals("available")) {
                            edtNhapMaQR.setError("Xe không khả dụng");
                            btnXemChiTiet.setEnabled(true);
                            return;
                        }
                        intent.putExtra("lat", currentLat);
                        intent.putExtra("lng", currentLng);
                        startActivity(intent);
                        finish();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    btnXemChiTiet.setEnabled(true);
                    if (error.networkResponse != null && error.networkResponse.statusCode == 404) {
                        edtNhapMaQR.setError("Xe không tồn tại");
                    } else {
                        edtNhapMaQR.setError("Lỗi server");
                    }
                    edtNhapMaQR.requestFocus();
                }
        );
        queue.add(request);
    }
}