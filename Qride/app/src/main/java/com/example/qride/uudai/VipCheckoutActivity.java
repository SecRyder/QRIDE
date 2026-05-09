package com.example.qride.uudai;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.R;
import com.example.qride.helper.APIHelper;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Màn hình thanh toán gói VIP/Hội viên qua MoMo.
 */
public class VipCheckoutActivity extends AppCompatActivity {

    private int voucherId;
    private String title;
    private String discount;
    private int voucherPrice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vip_checkout);

        // Lấy dữ liệu từ Intent
        voucherId = getIntent().getIntExtra("VOUCHER_ID", -1);
        title = getIntent().getStringExtra("VOUCHER_TITLE");
        discount = getIntent().getStringExtra("VOUCHER_DISCOUNT");
        voucherPrice = getIntent().getIntExtra("VOUCHER_PRICE", 0);

        // Ánh xạ View
        TextView tvVoucherName = findViewById(R.id.tvVoucherName);
        TextView tvVoucherPrice = findViewById(R.id.tvVoucherPrice);

        tvVoucherName.setText(title);
        tvVoucherPrice.setText(discount);

        findViewById(R.id.btnConfirmPayment).setOnClickListener(v -> {
            v.setEnabled(false); // Chống nhấn nhiều lần
            payWithMoMo();
        });
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void payWithMoMo() {
        int amount = 0;
        
        // Ưu tiên dùng giá tiền số (Price) nếu có
        if (voucherPrice > 0) {
            amount = voucherPrice;
        } else {
            // Nhảy vào logic cũ nếu Price = 0 (dùng cho các gói cũ chưa update DB)
            try {
                String cleanPrice = discount.replaceAll("[^0-9]", "");
                if (cleanPrice.isEmpty()) {
                    amount = 50000;
                } else {
                    amount = Integer.parseInt(cleanPrice);
                    if (amount < 1000) amount = 1000; 
                }
            } catch (Exception e) {
                amount = 50000;
            }
        }

        // Hiện Toast thông báo đang xử lý
        Toast.makeText(this, "Đang kết nối MoMo...", Toast.LENGTH_SHORT).show();

        JSONObject body = new JSONObject();
        try {
            body.put("voucherId", voucherId);
            body.put("amount", amount);
        } catch (Exception ignored) {}

        String url = APIHelper.BASE_URL + "payment/vip/momo";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body,
                response -> {
                    String payUrl = response.optString("payUrl");
                    if (!payUrl.isEmpty()) {
                        // Mở MoMo hoặc trình duyệt để thanh toán
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(payUrl));
                        startActivity(intent);
                        finish(); // Đóng màn hình checkout sau khi mở link
                    } else {
                        Toast.makeText(this, "Không lấy được link thanh toán", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    findViewById(R.id.btnConfirmPayment).setEnabled(true);
                    Toast.makeText(this, "Lỗi kết nối Server. Vui lòng thử lại!", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", "Bearer " + APIHelper.getToken(VipCheckoutActivity.this));
                return params;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }
}
