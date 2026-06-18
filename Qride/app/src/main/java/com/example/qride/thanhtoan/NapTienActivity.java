package com.example.qride.thanhtoan;

import static com.example.qride.helper.APIHelper.WALLET_TOPUP;
import static com.example.qride.helper.APIHelper.PAYMENT_STATUS;
import static com.example.qride.helper.APIHelper.getToken;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NapTienActivity extends AppCompatActivity {
    TextView tvAmount;
    Button btnContinue;
    String currentInput = "";

    // QR Dialog state
    private Dialog qrDialog;
    private CountDownTimer qrCountdown;
    private String currentPayUrl;
    private int currentTopupAmount = 0;

    // Polling state
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;
    private String currentOrderId;
    private boolean paymentConfirmed = false;
    private static final long POLL_INTERVAL_MS = 3000; // 3 giây

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
        btnContinue = findViewById(R.id.btnContinue);
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

    // ── BƯỚC 1: Gọi API tạo đơn nạp tiền MoMo ──
    private void callTopupAPI(int amount) {
        btnContinue.setEnabled(false);
        String token = getToken(this);

        JSONObject body = new JSONObject();
        try {
            body.put("amount", amount);
        } catch (Exception e) {}

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                WALLET_TOPUP,
                body,
                response -> {
                    try {
                        String payUrl    = response.optString("payUrl", "");
                        String qrCodeUrl = response.optString("qrCodeUrl", "");
                        String orderId   = response.optString("orderId", "");

                        if (qrCodeUrl.isEmpty() && payUrl.isEmpty()) {
                            btnContinue.setEnabled(true);
                            Toast.makeText(this, "Không nhận được thông tin thanh toán", Toast.LENGTH_LONG).show();
                            return;
                        }

                        currentPayUrl      = payUrl;
                        currentTopupAmount = amount;
                        currentOrderId     = orderId;
                        paymentConfirmed   = false;

                        if (!qrCodeUrl.isEmpty()) {
                            showQrDialog(qrCodeUrl, payUrl, amount);
                            if (!orderId.isEmpty()) {
                                startPolling(orderId, token);
                            }
                        } else {
                            openMomoBrowser(payUrl);
                        }

                    } catch (Exception e) {
                        btnContinue.setEnabled(true);
                        Toast.makeText(this, "Lỗi xử lý phản hồi", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    btnContinue.setEnabled(true);
                    Toast.makeText(this, "Lỗi kết nối server", Toast.LENGTH_SHORT).show();
                }
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

    // ── BƯỚC 2: Polling mỗi 3 giây để kiểm tra trạng thái payment ──
    private void startPolling(String orderId, String token) {
        stopPolling();
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (paymentConfirmed || isFinishing() || isDestroyed()) return;

                String statusUrl = PAYMENT_STATUS + orderId;

                JsonObjectRequest pollReq = new JsonObjectRequest(
                        Request.Method.GET,
                        statusUrl,
                        null,
                        resp -> {
                            String status = resp.optString("status", "");
                            if ("success".equals(status) && !paymentConfirmed) {
                                paymentConfirmed = true;
                                stopPolling();
                                onPaymentSuccess();
                            } else if ("failed".equals(status)) {
                                paymentConfirmed = true;
                                stopPolling();
                                onPaymentFailed();
                            } else {
                                // Còn pending → poll tiếp
                                if (!paymentConfirmed && !isFinishing()) {
                                    pollHandler.postDelayed(this, POLL_INTERVAL_MS);
                                }
                            }
                        },
                        error -> {
                            // Lỗi mạng → thử lại sau 3 giây
                            if (!paymentConfirmed && !isFinishing()) {
                                pollHandler.postDelayed(this, POLL_INTERVAL_MS);
                            }
                        }
                ) {
                    @Override
                    public Map<String, String> getHeaders() {
                        Map<String, String> h = new HashMap<>();
                        h.put("Authorization", "Bearer " + token);
                        return h;
                    }
                };

                Volley.newRequestQueue(NapTienActivity.this).add(pollReq);
            }
        };
        pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }

    private void stopPolling() {
        if (pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
            pollRunnable = null;
        }
    }

    // ── Xử lý khi thanh toán thành công (IPN đã cập nhật DB) ──
    private void onPaymentSuccess() {
        runOnUiThread(() -> {
            if (qrCountdown != null) qrCountdown.cancel();
            if (qrDialog != null && qrDialog.isShowing()) qrDialog.dismiss();
            Toast.makeText(this, "🎉 Nạp tiền thành công! Ví đã được cộng tiền.", Toast.LENGTH_LONG).show();
            finish();
        });
    }

    private void onPaymentFailed() {
        runOnUiThread(() -> {
            if (qrDialog != null && qrDialog.isShowing()) qrDialog.dismiss();
            if (qrCountdown != null) qrCountdown.cancel();
            Toast.makeText(this, "Thanh toán thất bại. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
            btnContinue.setEnabled(true);
        });
    }

    // ── BƯỚC 3: Hiện Dialog QR ──
    private void showQrDialog(String qrCodeString, String payUrl, int amount) {
        qrDialog = new Dialog(this);
        qrDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        qrDialog.setContentView(R.layout.dialog_momo_qr);
        qrDialog.setCancelable(false);

        Window w = qrDialog.getWindow();
        if (w != null) {
            w.setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.92f),
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            w.setBackgroundDrawableResource(android.R.color.transparent);
        }

        ImageView ivQr         = qrDialog.findViewById(R.id.ivMomoQr);
        ProgressBar pbLoading  = qrDialog.findViewById(R.id.pbQrLoading);
        TextView tvAmountText  = qrDialog.findViewById(R.id.tvQrAmount);
        TextView tvExpiry      = qrDialog.findViewById(R.id.tvQrExpiry);
        Button btnOpenMomo     = qrDialog.findViewById(R.id.btnOpenMomo);
        Button btnClose        = qrDialog.findViewById(R.id.btnCloseDialog);

        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        tvAmountText.setText(nf.format(amount) + "đ");

        pbLoading.setVisibility(View.VISIBLE);
        ivQr.setVisibility(View.GONE);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            Bitmap qrBitmap = generateQrBitmap(qrCodeString, 600);
            mainHandler.post(() -> {
                pbLoading.setVisibility(View.GONE);
                if (qrBitmap != null) {
                    ivQr.setImageBitmap(qrBitmap);
                    ivQr.setVisibility(View.VISIBLE);
                } else {
                    tvExpiry.setText("Không thể tạo mã QR");
                }
            });
        });
        executor.shutdown();

        startQrCountdown(tvExpiry);

        btnOpenMomo.setOnClickListener(v -> openMomoBrowser(payUrl));

        btnClose.setOnClickListener(v -> {
            stopPolling();
            if (qrCountdown != null) qrCountdown.cancel();
            qrDialog.dismiss();
            btnContinue.setEnabled(true);
        });

        qrDialog.show();
    }

    // ── Tạo QR bitmap từ chuỗi EMVCo (ZXing) ──
    private Bitmap generateQrBitmap(String content, int sizePx) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);

            int width = matrix.getWidth();
            int height = matrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            return bmp;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ── Countdown 15 phút ──
    private void startQrCountdown(TextView tvExpiry) {
        if (qrCountdown != null) qrCountdown.cancel();

        qrCountdown = new CountDownTimer(15 * 60 * 1000L, 1000) {
            @Override
            public void onTick(long ms) {
                long minutes = ms / 60000;
                long seconds = (ms % 60000) / 1000;
                tvExpiry.setText(String.format(Locale.getDefault(),
                        "Mã QR có hiệu lực trong %02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                stopPolling();
                tvExpiry.setText("Mã QR đã hết hạn. Vui lòng thử lại.");
                tvExpiry.setTextColor(0xFFE53935);
                if (qrDialog != null && qrDialog.isShowing()) {
                    qrDialog.findViewById(R.id.ivMomoQr).setAlpha(0.3f);
                }
                btnContinue.setEnabled(true);
            }
        }.start();
    }

    // ── Fallback: mở MoMo qua browser ──
    private void openMomoBrowser(String payUrl) {
        if (payUrl == null || payUrl.isEmpty()) return;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(payUrl));
        startActivity(intent);
    }

    // ── Deep link callback từ MoMo redirectUrl ──
    private void handleDeepLink(Intent intent) {
        Uri data = intent.getData();
        if (data != null) {
            String resultCode = data.getQueryParameter("resultCode");

            stopPolling();
            if (qrDialog != null && qrDialog.isShowing()) qrDialog.dismiss();
            if (qrCountdown != null) qrCountdown.cancel();

            if ("0".equals(resultCode) && !paymentConfirmed) {
                paymentConfirmed = true;
                Toast.makeText(this, "🎉 Nạp tiền thành công! Ví đã được cộng tiền.", Toast.LENGTH_LONG).show();
                finish();
            } else if (resultCode != null && !"0".equals(resultCode)) {
                Toast.makeText(this, "Thanh toán thất bại", Toast.LENGTH_LONG).show();
                btnContinue.setEnabled(true);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLink(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPolling();
        if (qrCountdown != null) qrCountdown.cancel();
        if (qrDialog != null && qrDialog.isShowing()) qrDialog.dismiss();
    }

    // BACK
    private void setupBack() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}
