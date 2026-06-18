package com.example.qride.thanhtoan;

import static com.example.qride.helper.APIHelper.TRANSACTION_DETAIL;
import static com.example.qride.helper.APIHelper.getToken;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.R;
import com.example.qride.thuexe.QRScanActivity;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TransactionDetailActivity extends AppCompatActivity {

    ImageView btnBack, imgType;
     FrameLayout btnSupport;
    TextView tvTitle, tvAmount, tvDesc, tvStatus, tvCode, tvRental, tvTime, tvFee;
    TextView tvOriginalPrice, tvDiscount;
    android.widget.LinearLayout layoutPrice;
    Button btnAction;
    private static final String TAG = "TRANSACTION_DETAIL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_detail);
        // Fullscreen
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
        initView();
        int id = getIntent().getIntExtra("transaction_id", -1);
        loadTransaction(id);
        btnSupport.setOnClickListener(v -> {
            Intent intent = new Intent(TransactionDetailActivity.this, com.example.qride.profile.SupportCenterActivity.class);
            startActivity(intent);
        });
        btnBack.setOnClickListener(v -> finish());
    }

    private void initView() {
        btnBack = findViewById(R.id.btnBack);
        imgType = findViewById(R.id.imgType);
        btnSupport = findViewById(R.id.btnSupport);
        tvTitle = findViewById(R.id.tvTitle);
        tvAmount = findViewById(R.id.tvAmount);
        tvDesc = findViewById(R.id.tvDesc);
        tvStatus = findViewById(R.id.tvStatus);
        tvCode = findViewById(R.id.tvCode);
        tvRental = findViewById(R.id.tvRental);
        tvTime = findViewById(R.id.tvTime);
        tvFee = findViewById(R.id.tvFee);
        btnAction = findViewById(R.id.btnAction);
        
        // Price detail
        layoutPrice = findViewById(R.id.layoutPrice);
        tvOriginalPrice = findViewById(R.id.tvOriginalPrice);
        tvDiscount = findViewById(R.id.tvDiscount);
    }

    private void loadTransaction(int id) {
        String url = TRANSACTION_DETAIL + id;
        String token = getToken(this);
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        String type = response.getString("type");
                        long amount = response.getLong("amount");
                        String desc = response.optString("description", "");
                        String status = response.optString("payment_status", "success");
                        String code = response.optString("external_ref", "-");
                        if (code.isEmpty() || code.equals("null")) {
                            code = "#" + response.optString("id", "-");
                        }
                        // Thử nhiều field có thể cho thời gian
                        String time = response.optString("created_at", "");
                        if (time.isEmpty()) {
                            time = response.optString("transaction_date", "");
                        }
                        if (time.isEmpty()) {
                            time = response.optString("date", "");
                        }
                        String rentalId = response.optString("rental_id", "");
                        
                        Log.d(TAG, "API Response time field: " + time);
                        
                        // Lấy thêm discount info (nếu có)
                        long originalAmount = response.optLong("original_amount", amount);
                        long discountAmount = response.optLong("discount_amount", 0);
                        String discountTitle = response.optString("discount_title", "");

                        bindData(type, amount, desc, status, code, time, rentalId, 
                                 originalAmount, discountAmount, discountTitle);

                    } catch (Exception e) {
                        Log.e(TAG, "Parse error", e);
                        e.printStackTrace();
                    }
                },
                error -> {
                    Log.e(TAG, "API error", error);
                    Toast.makeText(this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Authorization", "Bearer " + token);
                return h;
            }
        };

        queue.add(request);
    }

    private void bindData(String type, long amount, String desc,
                          String status, String code,
                          String time, String rentalId,
                          long originalAmount, long discountAmount, String discountTitle) {

        // ===== TITLE + ICON =====
        switch (type) {
            case "topup":
                tvTitle.setText("Nạp tiền");
                imgType.setImageResource(R.drawable.naptien);
                tvAmount.setText("+ " + formatMoney(amount));
                btnAction.setText("Nạp thêm");
                btnAction.setOnClickListener(v -> {
                    Intent intent = new Intent(this, NapTienActivity.class);
                    startActivity(intent);
                });
                break;

            case "withdraw":
                tvTitle.setText("Rút tiền");
                imgType.setImageResource(R.drawable.rutien);
                tvAmount.setText("- " + formatMoney(amount));
                btnAction.setText("Rút tiếp");
                btnAction.setOnClickListener(v -> {
                    Intent intent = new Intent(this, RutTienActivity.class);
                    startActivity(intent);
                });
                break;

            case "payment":
                tvTitle.setText("Thuê xe");
                imgType.setImageResource(R.drawable.ic_bike_small);
                tvAmount.setText("- " + formatMoney(amount));
                btnAction.setText("Thuê xe mới");
                tvRental.setVisibility(View.VISIBLE);
                tvRental.setText("Mã chuyến: #" + rentalId);
                btnAction.setOnClickListener(v -> {
                    Intent intent = new Intent(this, QRScanActivity.class);
                    startActivity(intent);
                });
                
                // Hiển thị giá gốc và discount nếu có
                if (originalAmount > 0) {
                    layoutPrice.setVisibility(View.VISIBLE);
                    tvOriginalPrice.setText("Giá gốc: " + formatMoney(originalAmount));
                    
                    if (discountAmount > 0) {
                        String discountText = "Giảm giá: -" + formatMoney(discountAmount);
                        if (!discountTitle.isEmpty()) {
                            discountText += " (" + discountTitle + ")";
                        }
                        tvDiscount.setText(discountText);
                        tvDiscount.setVisibility(View.VISIBLE);
                        
                        Log.d(TAG, "Payment: Original=" + originalAmount + ", Discount=" + discountAmount + ", Final=" + amount);
                    } else {
                        tvDiscount.setVisibility(View.GONE);
                    }
                }
                break;
        }

        // ===== STATUS =====
        if ("success".equals(status)) {
            tvStatus.setText("Trạng thái: Thành công");
            tvStatus.setTextColor(Color.parseColor("#009688"));
        } else if ("pending".equals(status)) {
            tvStatus.setText("Trạng thái: Đang xử lý");
            tvStatus.setTextColor(Color.parseColor("#FF9800"));
        } else {
            tvStatus.setText("Trạng thái: Thất bại");
            tvStatus.setTextColor(Color.RED);
        }

        tvDesc.setText(desc);
        tvCode.setText("Mã giao dịch: " + code);
        String formattedTime = (time != null && !time.isEmpty()) ? formatDate(time) : "Không xác định";
        tvTime.setText("Thời gian: " + formattedTime);
        tvFee.setText("Phí dịch vụ: 0đ");
    }

    private String formatMoney(long money) {
        return String.format("%,dđ", money);
    }

    private String formatDate(String iso) {
        if (iso == null || iso.isEmpty() || iso.equals("null")) {
            Log.w(TAG, "Date is null or empty");
            return "Không xác định";
        }
        
        try {
            // Thử nhiều format khác nhau
            SimpleDateFormat[] formats = {
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"),
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"),
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"),
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
                new SimpleDateFormat("yyyy-MM-dd")
            };
            
            SimpleDateFormat output = new SimpleDateFormat("HH:mm - dd/MM/yyyy");
            Date date = null;
            
            for (SimpleDateFormat format : formats) {
                try {
                    date = format.parse(iso);
                    Log.d(TAG, "Successfully parsed date with format: " + format.toPattern());
                    break;
                } catch (Exception e) {
                    // Thử format tiếp theo
                }
            }
            
            if (date != null) {
                return output.format(date);
            } else {
                Log.e(TAG, "Could not parse date with any format: " + iso);
                return iso;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error formatting date: " + iso, e);
            return iso;
        }
    }
}
