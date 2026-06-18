package com.example.qride.thuexe;

import static com.example.qride.helper.APIHelper.getToken;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.MainActivity;
import com.example.qride.R;
import com.example.qride.helper.APIHelper;
import com.example.qride.sqlite.NotificationDAO;
import com.example.qride.sqlite.UserDAO;
import com.example.qride.thanhtoan.TransactionDetailActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class EndRideActivity extends AppCompatActivity {
    private TextView tvPlate, tvPin, tvLocation, tvTotal;
    private Button btnConfirm;
    private ImageView btnBack;

    private int vehicleId;
    private double lat, lng;
    private int discountApplied = 0;  // Lưu discount để sử dụng lúc return

    private RequestQueue queue;
    private NotificationDAO notificationDAO;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_endride);
        // Fullscreen
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
        queue = Volley.newRequestQueue(this);
        notificationDAO = new NotificationDAO(this);

        initViews();
        getIntentData();
        loadVehicleInfo();
        setupEvents();
    }

    private void initViews() {
        tvPlate = findViewById(R.id.tvPlate);
        tvPin = findViewById(R.id.tvPin);
        tvLocation = findViewById(R.id.tvLocation);
        tvTotal = findViewById(R.id.tvTotal);

        btnConfirm = findViewById(R.id.btnConfirm);
        btnBack = findViewById(R.id.btnBack);
    }

    private void getIntentData() {
        Intent intent = getIntent();

        vehicleId = intent.getIntExtra("vehicleId", -1);
        lat = intent.getDoubleExtra("lat", 0);
        lng = intent.getDoubleExtra("lng", 0);

        String time = intent.getStringExtra("time");
        String distance = intent.getStringExtra("distance");
        String price = intent.getStringExtra("price");
        String stationName = intent.getStringExtra("stationName");

        if (stationName != null) {
            tvLocation.setText(stationName);
        } else {
            tvLocation.setText("Lat: " + lat + " | Lng: " + lng);
        }

        TextView tvTime = findViewById(R.id.tvTime);
        TextView tvDistance = findViewById(R.id.tvDistance);

        if (tvTime != null && time != null) {
            tvTime.setText(time);
        }

        if (tvDistance != null && distance != null) {
            tvDistance.setText(distance);
        }

        if (price != null) {
            try {
                int total = Integer.parseInt(price.replace("đ", "").replace(".", "").trim());

                TextView tvUnlock = findViewById(R.id.tvUnlockFee);
                TextView tvRental = findViewById(R.id.tvRentalFee);
                TextView tvDiscount = findViewById(R.id.tvDiscount);

                int unlockFee = 5000;
                int rentalFee = Math.max(total - unlockFee, 0);

                if (tvUnlock != null) tvUnlock.setText(unlockFee + "đ");
                if (tvRental != null) tvRental.setText(rentalFee + "đ");
                if (tvDiscount != null) tvDiscount.setText("0đ");

                tvTotal.setText(total + "đ");

                checkActiveVoucherAndCalculate(total);

            } catch (Exception e) {
                tvTotal.setText(price);
            }
        }
    }

    private void loadVehicleInfo() {
        if (vehicleId == -1) return;

        String url = APIHelper.BASE_URL + "vehicle/" + vehicleId;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        tvPlate.setText(response.optString("plate", "N/A"));
                        tvPin.setText(response.optInt("pin", 0) + "%");
                    } catch (Exception e) {
                        Log.e("VEHICLE", "Parse error", e);
                    }
                },
                error -> {
                    Log.e("VEHICLE", "API error", error);
                    Toast.makeText(this, "Lỗi load xe", Toast.LENGTH_SHORT).show();
                }
        );

        queue.add(request);
    }

    private void checkActiveVoucherAndCalculate(int total) {
        String url = APIHelper.ACTIVE_VOUCHER;
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        Log.d("VOUCHER_API", "Full Response: " + response.toString());
                        
                        // Kiểm tra xem có voucher hay không
                        boolean hasVoucher = false;
                        int discountValue = 0;
                        String discountType = "PERCENT";
                        String title = "Ưu đãi";
                        
                        // Kiểm tra nhiều field có thể
                        if (!response.isNull("discount_value")) {
                            discountValue = response.optInt("discount_value", 0);
                            hasVoucher = true;
                        }
                        
                        if (hasVoucher && discountValue > 0) {
                            discountType = response.optString("discount_type", "PERCENT");
                            title = response.optString("title_display", response.optString("title", "Ưu đãi"));

                            int calculatedDiscount = 0;
                            if ("PERCENT".equals(discountType)) {
                                calculatedDiscount = (total * discountValue) / 100;
                                Log.d("VOUCHER", "PERCENT: " + total + " × " + discountValue + "% = " + calculatedDiscount + "đ");
                            } else if ("CASH".equals(discountType)) {
                                calculatedDiscount = discountValue;
                                Log.d("VOUCHER", "CASH: -" + discountValue + "đ");
                            }
                            
                            // Không được vượt quá tổng tiền
                            calculatedDiscount = Math.min(calculatedDiscount, total);
                            discountApplied = calculatedDiscount;

                            TextView tvDiscount = findViewById(R.id.tvDiscount);
                            if (tvDiscount != null) {
                                tvDiscount.setText("-" + calculatedDiscount + "đ (" + title + ")");
                                Log.d("VOUCHER", "Updated UI: -" + calculatedDiscount + "đ (" + title + ")");
                            }

                            int finalTotal = Math.max(total - calculatedDiscount, 0);
                            tvTotal.setText(finalTotal + "đ");
                            
                            Log.d("VOUCHER", "✓ Applied: " + calculatedDiscount + "đ, Final Total: " + finalTotal + "đ");
                        } else {
                            // Không có ưu đãi
                            TextView tvDiscount = findViewById(R.id.tvDiscount);
                            if (tvDiscount != null) {
                                tvDiscount.setText("0đ");
                            }
                            discountApplied = 0;
                            Log.d("VOUCHER", "✗ No voucher found");
                        }
                    } catch (Exception e) {
                        Log.e("VOUCHER", "Parse error: " + e.getMessage(), e);
                        TextView tvDiscount = findViewById(R.id.tvDiscount);
                        if (tvDiscount != null) {
                            tvDiscount.setText("0đ");
                        }
                        discountApplied = 0;
                    }
                },
                error -> {
                    Log.e("VOUCHER", "Error loading active voucher: " + error.getMessage(), error);
                    if (error.networkResponse != null) {
                        String body = new String(error.networkResponse.data);
                        Log.e("VOUCHER_ERR_BODY", body);
                    }
                    // Nếu lỗi, vẫn hiển thị không có discount
                    TextView tvDiscount = findViewById(R.id.tvDiscount);
                    if (tvDiscount != null) {
                        tvDiscount.setText("0đ");
                    }
                    discountApplied = 0;
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                String token = APIHelper.getToken(EndRideActivity.this);
                if (token != null) {
                    headers.put("Authorization", "Bearer " + token);
                }
                Log.d("VOUCHER_REQ", "Request URL: " + APIHelper.ACTIVE_VOUCHER);
                return headers;
            }
        };
        queue.add(request);
    }

    private void setupEvents() {
        btnBack.setOnClickListener(v -> finish());
        btnConfirm.setOnClickListener(v -> callReturnAPI());
    }

    private void callReturnAPI() {

        FusedLocationProviderClient fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(this, "Chưa có quyền GPS", Toast.LENGTH_SHORT).show();
            return;
        }

        btnConfirm.setEnabled(false);

        fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
        ).addOnSuccessListener(location -> {

            if (location == null) {
                btnConfirm.setEnabled(true);

                new AlertDialog.Builder(this)
                        .setTitle("Không lấy được vị trí")
                        .setMessage("Vui lòng bật GPS và thử lại")
                        .setPositiveButton("OK", null)
                        .show();

                return;
            }

            String url = APIHelper.BASE_URL + "return";

            JSONObject json = new JSONObject();
            try {
                json.put("vehicleId", vehicleId);
                json.put("lat", location.getLatitude());
                json.put("lng", location.getLongitude());
                // Gửi discount được áp dụng để server verify
                json.put("discount_preview", discountApplied);

            } catch (Exception e) {
                Log.e("RETURN", "JSON error", e);
            }

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    json,
                    response -> {
                        btnConfirm.setEnabled(true);

                        Log.d("RETURN_API", response.toString());

                        String message = response.optString("message");

                        switch (message) {

                            case "NOT_IN_STATION":
                                int dist = response.optInt("distance", 0);
                                new AlertDialog.Builder(this)
                                        .setTitle("Không thể trả xe")
                                        .setMessage("Bạn đang cách trạm " + dist + "m\nHãy di chuyển đến trạm gần nhất để trả xe.")
                                        .setPositiveButton("OK", null)
                                        .show();
                                break;

                            case "NO_RENTAL":
                                Toast.makeText(this,
                                        "Không có chuyến đi đang thuê",
                                        Toast.LENGTH_LONG).show();
                                break;
                            case "NOT_ENOUGH_MONEY":
                                int balance = response.optInt("balance", 0);
                                int need = response.optInt("need", 0);

                                new AlertDialog.Builder(this)
                                        .setTitle("Không đủ tiền")
                                        .setMessage("Số dư: " + balance + "đ\nCần: " + need + "đ")
                                        .setPositiveButton("OK", null)
                                        .show();
                                break;

                            case "SUCCESS":
                                int finalPrice = response.optInt("total_price", 0);
                                int discount = response.optInt("discount_applied", 0);
                                int minutes = response.optInt("minutes", 0);
                                int rentalId = response.optInt("rental_id", -1);
                                String discountTitle = response.optString("discount_title", "");

                                int userId = new UserDAO(this).getUserId();
                                if (userId > 0) {
                                    String msg = "Chuyến đi đã hoàn tất. ";
                                    if (discount > 0) {
                                        msg += "Ưu đãi: -" + discount + "đ. ";
                                    }
                                    msg += "Chi phí: " + finalPrice + "đ.";
                                    notificationDAO.addNotification(
                                            userId,
                                            "Trả xe thành công",
                                            msg,
                                            "RIDE"
                                    );
                                }

                                tvTotal.setText(finalPrice + "đ");
                                TextView tvDiscount = findViewById(R.id.tvDiscount);
                                if (tvDiscount != null && discount > 0) {
                                    String discountDisplay = "-" + discount + "đ";
                                    if (!discountTitle.isEmpty()) {
                                        discountDisplay += " (" + discountTitle + ")";
                                    }
                                    tvDiscount.setText(discountDisplay);
                                } else if (tvDiscount != null) {
                                    tvDiscount.setText("0đ");
                                }
                                
                                Log.d("END_RIDE", "Success: Price=" + finalPrice + "đ, Discount=" + discount + "đ, Time=" + minutes + "m");

                                Toast.makeText(this,
                                        "Trả xe thành công (" + minutes + " phút)",
                                        Toast.LENGTH_LONG).show();

                                Intent intent = new Intent(this, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                                intent.putExtra("rideEnded", true);
                                // Dung cho danh gia
                                intent.putExtra("rental_id",rentalId);
                                // Dung cho xem chi tiet
                                intent.putExtra("transaction_id", response.optInt("transaction_id", -1));

                                startActivity(intent);
                                finish();
                                break;

                            default:
                                Toast.makeText(this,
                                        "Phản hồi không xác định",
                                        Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        btnConfirm.setEnabled(true);

                        if (error.networkResponse != null) {
                            String body = new String(error.networkResponse.data);
                            Log.e("RETURN_API_ERROR", body);
                        }

                        Toast.makeText(this, "Lỗi trả xe", Toast.LENGTH_SHORT).show();
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + getToken(EndRideActivity.this));
                    return headers;
                }
            };
            queue.add(request);
        });
    }
}
