package com.example.qride.thanhtoan;

import static com.example.qride.helper.APIHelper.WALLET_TOPUP;
import static com.example.qride.helper.APIHelper.getToken;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.R;

import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class NapTienActivity extends AppCompatActivity {
    TextView tvAmount;
    Button btnContinue;
    String currentInput = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nap_tien);
        // Fullscreen
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
        handleDeepLink(getIntent());
        tvAmount = findViewById(R.id.tvAmount);
        btnContinue = findViewById  (R.id.btnContinue);
        setupKeypad();
        setupQuickButtons();
        btnContinue.setOnClickListener(v -> {
            if (currentInput.isEmpty()) {
                Toast.makeText(this, "Nhập số tiền", Toast.LENGTH_SHORT).show();
                return;
            }
            int amount = Integer.parseInt(currentInput);
            callTopupAPI(amount);
        });
        setupBack();
    }

    private void setupKeypad() {
        GridLayout grid = findViewById(R.id.keypad);
        for (int i = 0; i < grid.getChildCount(); i++) {
            View v = grid.getChildAt(i);
            if (v instanceof Button) {
                Button btn = (Button) v;
                btn.setOnClickListener(view -> {
                    String number = btn.getText().toString();
                    currentInput += number;
                    updateUI();
                });
            }
        }
        findViewById(R.id.btnDelete).setOnClickListener(v -> {
            if (!currentInput.isEmpty()) {
                currentInput = currentInput.substring(0, currentInput.length() - 1);
                updateUI();
            }
        });
    }

    private void setupQuickButtons() {
        findViewById(R.id.btn30k).setOnClickListener(v -> setAmount(30000));
        findViewById(R.id.btn50k).setOnClickListener(v -> setAmount(50000));
        findViewById(R.id.btn100k).setOnClickListener(v -> setAmount(100000));
    }

    private void setAmount(int amount) {
        currentInput = String.valueOf(amount);
        updateUI();
    }

    private void updateUI() {
        if (currentInput.isEmpty()) {
            tvAmount.setText("0 đ");
            btnContinue.setEnabled(false);
            return;
        }
        long value = Long.parseLong(currentInput);
        NumberFormat format = NumberFormat.getInstance(new Locale("vi", "VN"));
        tvAmount.setText(format.format(value) + " đ");

        btnContinue.setEnabled(value >= 10000);
    }

    // Fake luong xu ly
//    private void callTopupAPI(int amount) {
//        String url = WALLET_TOPUP;
//        String token = getToken(this);
//        JSONObject body = new JSONObject();
//        try {
//            body.put("amount", amount);
//        } catch (Exception e) {}
//        JsonObjectRequest request = new JsonObjectRequest(
//                Request.Method.POST,
//                url,
//                body,
//                response -> {
//                    Toast.makeText(this, "Nạp thành công", Toast.LENGTH_SHORT).show();
//                    finish();
//                },
//                error -> Toast.makeText(this, "Lỗi nạp tiền", Toast.LENGTH_SHORT).show()
//        ){
//            @Override
//            public Map<String, String> getHeaders() {
//                Map<String, String> headers = new HashMap<>();
//                headers.put("Authorization", "Bearer " + token);
//                return headers;
//            }
//        };
//        Volley.newRequestQueue(this).add(request);
//    }

    // Luong goi momo that
    private void callTopupAPI(int amount) {
        String url = WALLET_TOPUP;
        String token = getToken(this);

        JSONObject body = new JSONObject();
        try {
            body.put("amount", amount);
        } catch (Exception e) {}

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                body,
                response -> {
                    try {
                        String payUrl = response.getString("payUrl");
                        Toast.makeText(this, "Đang chuyển sang MoMo...", Toast.LENGTH_SHORT).show();
                        // MỞ MOMO
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(payUrl));
                        startActivity(intent);

                    } catch (Exception e) {
                        Toast.makeText(this, "Lỗi mở MoMo", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Lỗi kết nối server", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

    private void handleDeepLink(Intent intent) {
        Uri data = intent.getData();
        if (data != null) {
            String resultCode = data.getQueryParameter("resultCode");

            if ("0".equals(resultCode)) {
                Toast.makeText(this, "Nạp tiền thành công", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Thanh toán thất bại", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLink(intent);
    }

    // BACK
    private void setupBack() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}
