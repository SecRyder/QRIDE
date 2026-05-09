package com.example.qride.thanhtoan;

import static com.example.qride.helper.APIHelper.LOAD_WALLET;
import static com.example.qride.helper.APIHelper.WALLET_WITHDRAW;
import static com.example.qride.helper.APIHelper.getToken;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.R;
import com.example.qride.helper.APIHelper;

import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RutTienActivity extends AppCompatActivity {

    private TextView tvAmount;
    private Button btnContinue;

    private String currentInput = "";
    private long currentBalance = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rut_tien);
        // Fullscreen
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        tvAmount = findViewById(R.id.tvAmount);
        btnContinue = findViewById(R.id.btnContinue);

        setupKeypad();
        setupQuickAmount();
        setupBack();

        loadWallet(); // lấy số dư để check
    }

    // KEYPAD
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
        ImageButton btnDelete = findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(v -> {
            if (!currentInput.isEmpty()) {
                currentInput = currentInput.substring(0, currentInput.length() - 1);
                updateUI();
            }
        });
    }

    // QUICK BUTTON
    private void setupQuickAmount() {
        findViewById(R.id.btn30k).setOnClickListener(v -> setAmount(30000));
        findViewById(R.id.btn50k).setOnClickListener(v -> setAmount(50000));
        findViewById(R.id.btn100k).setOnClickListener(v -> setAmount(100000));
    }

    private void setAmount(int amount) {
        currentInput = String.valueOf(amount);
        updateUI();
    }

    // UPDATE UI
    private void updateUI() {
        if (currentInput.isEmpty()) {
            tvAmount.setText("0 đ");
            btnContinue.setEnabled(false);
            return;
        }
        long value = Long.parseLong(currentInput);
        NumberFormat format = NumberFormat.getInstance(new Locale("vi", "VN"));
        tvAmount.setText(format.format(value) + " đ");
        // validate:
        if (value < 10000) {
            btnContinue.setEnabled(false);
        } else if (value > currentBalance) {
            btnContinue.setEnabled(false);
            Toast.makeText(this, "Số dư không đủ", Toast.LENGTH_SHORT).show();
        } else {
            btnContinue.setEnabled(true);
        }

        btnContinue.setOnClickListener(v -> callWithdrawAPI(value));
    }

    // == CALL API ==
    private void callWithdrawAPI(long amount) {
        String url = WALLET_WITHDRAW;
        String token = getToken(this);
        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }
        JSONObject body = new JSONObject();
        try {
            body.put("amount", amount);
        } catch (Exception e) {}
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                body,

                response -> {
                    Toast.makeText(this, "Rút tiền thành công", Toast.LENGTH_SHORT).show();
                    finish();
                },
                error -> {
                    if (error.networkResponse != null) {
                        int code = error.networkResponse.statusCode;
                        Toast.makeText(this, "Lỗi server: " + code, Toast.LENGTH_SHORT).show();
                    } else {
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

    // LOAD WALLET
    private void loadWallet() {
        String url = LOAD_WALLET;
        String token = getToken(this);
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,

                response -> {
                    currentBalance = response.optLong("balance", 0);
                },

                error -> {
                    Toast.makeText(this, "Không load được ví", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };

        queue.add(request);
    }

    // BACK
    private void setupBack() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}
