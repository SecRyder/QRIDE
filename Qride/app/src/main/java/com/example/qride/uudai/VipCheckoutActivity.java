package com.example.qride.uudai;

import android.content.Intent;
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
import com.example.qride.MainActivity;
import com.example.qride.R;
import com.example.qride.helper.APIHelper;
import com.example.qride.sqlite.NotificationDAO;
import com.example.qride.sqlite.UserDAO;

import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Màn hình thanh toán gói VIP/Hội viên.
 * Sau khi thanh toán thành công, hệ thống lưu thông báo và cập nhật trạng thái người dùng.
 */
public class VipCheckoutActivity extends AppCompatActivity {

    private int voucherId;
    private String voucherTitle;
    private String voucherDiscount;
    private int voucherPrice;

    private Button btnConfirmPayment;
    private NotificationDAO notificationDAO;
    private UserDAO userDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vip_checkout);

        notificationDAO = new NotificationDAO(this);
        userDAO = new UserDAO(this);

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

        if (voucherTitle != null) tvVoucherName.setText(voucherTitle);
        if (voucherDiscount != null) tvVoucherBenefit.setText(voucherDiscount);

        if (voucherPrice > 0) {
            NumberFormat nf = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
            tvVoucherPrice.setText(nf.format(voucherPrice) + "đ");
        } else {
            tvVoucherPrice.setText("0đ");
        }

        btnConfirmPayment.setOnClickListener(v -> {
            v.setEnabled(false);
            buyVipPackage();
        });

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    private void buyVipPackage() {
        JSONObject body = new JSONObject();
        try {
            body.put("voucherId", voucherId);
        } catch (Exception ignored) {}

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, APIHelper.BUY_VOUCHER, body,
                response -> {
                    // 1. Lưu thông báo vào SQLite
                    int userId = userDAO.getUserId();
                    String msg = "Chúc mừng! Bạn đã đăng ký thành công " + voucherTitle + ". Tận hưởng ưu đãi ngay!";
                    notificationDAO.addNotification(userId, "Gói Hội Viên", msg, "MEMBERSHIP");

                    // 2. Thông báo cho người dùng
                    Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_LONG).show();

                    // 3. Quay lại và cập nhật UI
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                },
                error -> {
                    btnConfirmPayment.setEnabled(true);
                    String errorMsg = "Lỗi thanh toán, vui lòng thử lại";
                    if (error.networkResponse != null && error.networkResponse.statusCode == 400) {
                        errorMsg = "Số dư ví không đủ để đăng ký gói này.";
                    }
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
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
