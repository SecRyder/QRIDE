package com.example.qride.uudai;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.R;
import com.example.qride.helper.APIHelper;

import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Màn hình thanh toán gói VIP/Hội viên qua MoMo.
 * Hiển thị thông tin gói và giá tiền rõ ràng trước khi thanh toán.
 */
public class VipCheckoutActivity extends AppCompatActivity {

    private int voucherId;
    private String voucherTitle;
    private String voucherDiscount;
    private int voucherPrice;

    private Button btnConfirmPayment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vip_checkout);

        // Lấy dữ liệu từ Intent
        voucherId       = getIntent().getIntExtra("VOUCHER_ID", -1);
        voucherTitle    = getIntent().getStringExtra("VOUCHER_TITLE");
        voucherDiscount = getIntent().getStringExtra("VOUCHER_DISCOUNT");
        voucherPrice    = getIntent().getIntExtra("VOUCHER_PRICE", 0);

        // Ánh xạ Views
        TextView tvVoucherName    = findViewById(R.id.tvVoucherName);
        TextView tvVoucherBenefit = findViewById(R.id.tvVoucherBenefit);
        TextView tvVoucherPrice   = findViewById(R.id.tvVoucherPrice);
        btnConfirmPayment         = findViewById(R.id.btnConfirmPayment);

        // Hiển thị tên gói
        if (voucherTitle != null) tvVoucherName.setText(voucherTitle);

        // Hiển thị quyền lợi (discount text)
        if (voucherDiscount != null && !voucherDiscount.isEmpty()) {
            tvVoucherBenefit.setText(voucherDiscount);
            tvVoucherBenefit.setVisibility(View.VISIBLE);
        } else {
            tvVoucherBenefit.setVisibility(View.GONE);
        }

        // Hiển thị giá tiền (format VND)
        if (voucherPrice > 0) {
            NumberFormat nf = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
            tvVoucherPrice.setText(nf.format(voucherPrice) + "đ");
        } else {
            tvVoucherPrice.setText("Liên hệ");
        }

        // Nút thanh toán
        btnConfirmPayment.setOnClickListener(v -> {
            v.setEnabled(false);
            payWithMoMo();
        });

        // Nút back
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    private void payWithMoMo() {
        // Tính giá thanh toán thực tế
        int amount;
        if (voucherPrice > 0) {
            amount = voucherPrice;
        } else {
            // Fallback: thử parse từ discount text nếu price = 0
            try {
                String clean = voucherDiscount != null
                        ? voucherDiscount.replaceAll("[^0-9]", "") : "";
                amount = clean.isEmpty() ? 50000 : Math.max(1000, Integer.parseInt(clean));
            } catch (Exception e) {
                amount = 50000;
            }
        }

        Toast.makeText(this, getString(R.string.vip_connecting_momo), Toast.LENGTH_SHORT).show();

        JSONObject body = new JSONObject();
        try {
            body.put("voucherId", voucherId);
            body.put("amount", amount);
        } catch (Exception ignored) {}

        String url = APIHelper.PAYMENT_VIP_MOMO;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body,
                response -> {
                    String payUrl = response.optString("payUrl", "");
                    if (!payUrl.isEmpty()) {
                        // Mở MoMo app hoặc trình duyệt
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(payUrl));
                        startActivity(intent);
                        finish();
                    } else {
                        btnConfirmPayment.setEnabled(true);
                        Toast.makeText(this,
                                getString(R.string.vip_error_no_link),
                                Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    btnConfirmPayment.setEnabled(true);
                    Toast.makeText(this,
                            getString(R.string.vip_error_connect),
                            Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + APIHelper.getToken(VipCheckoutActivity.this));
                return headers;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }
}
