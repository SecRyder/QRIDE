package com.example.qride.uudai;

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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.R;
import com.example.qride.helper.APIHelper;
import com.example.qride.sqlite.NotificationDAO;
import com.example.qride.sqlite.UserDAO;
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

/**
 * Màn hình thanh toán gói VIP/Hội viên qua MoMo QR Code.
 *
 * Luồng:
 *  1. Nhấn "Thanh toán" → gọi POST /api/payment/vip/momo
 *  2. Server trả về { payUrl, qrCodeUrl }
 *  3. Hiển thị Dialog chứa QR bitmap (render từ qrCodeUrl bằng ZXing)
 *  4. User dùng app MoMo quét QR hoặc nhấn "Mở ứng dụng MoMo"
 *  5. MoMo gọi webhook IPN → server kích hoạt VIP tự động
 */
public class VipCheckoutActivity extends AppCompatActivity {

    private int voucherId;
    private String voucherTitle;
    private String voucherDiscount;
    private int voucherPrice;

    private Button btnConfirmPayment;
    private Button btnPayWallet;
    private NotificationDAO notificationDAO;

    // QR Dialog state
    private Dialog qrDialog;
    private CountDownTimer qrCountdown;
    private String currentPayUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vip_checkout);

        voucherId       = getIntent().getIntExtra("VOUCHER_ID", -1);
        voucherTitle    = getIntent().getStringExtra("VOUCHER_TITLE");
        voucherDiscount = getIntent().getStringExtra("VOUCHER_DISCOUNT");
        voucherPrice    = getIntent().getIntExtra("VOUCHER_PRICE", 0);

        TextView tvVoucherName    = findViewById(R.id.tvVoucherName);
        TextView tvVoucherBenefit = findViewById(R.id.tvVoucherBenefit);
        TextView tvVoucherPrice   = findViewById(R.id.tvVoucherPrice);
        btnConfirmPayment         = findViewById(R.id.btnConfirmPayment);
        btnPayWallet              = findViewById(R.id.btnPayWallet);
        notificationDAO           = new NotificationDAO(this);

        if (voucherTitle != null)    tvVoucherName.setText(voucherTitle);
        if (voucherDiscount != null) tvVoucherBenefit.setText(voucherDiscount);

        if (voucherPrice > 0) {
            NumberFormat nf = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
            tvVoucherPrice.setText(nf.format(voucherPrice) + "đ");
        } else {
            tvVoucherPrice.setText("Miễn phí");
        }

        btnConfirmPayment.setOnClickListener(v -> {
            v.setEnabled(false);
            createMomoPayment();
        });
        btnPayWallet.setOnClickListener(v -> {
            v.setEnabled(false);
            buyVipWithWallet();
        });

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Xử lý deep link callback khi MoMo trả về app
        handleDeepLink(getIntent());
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
        if (qrCountdown != null) qrCountdown.cancel();
        if (qrDialog != null && qrDialog.isShowing()) qrDialog.dismiss();
    }

    // ─────────────────────────────────────────────────────────────
    // BƯỚC 1: Gọi server tạo đơn MoMo, nhận payUrl + qrCodeUrl
    // ─────────────────────────────────────────────────────────────
    private void createMomoPayment() {
        JSONObject body = new JSONObject();
        try {
            body.put("voucherId", voucherId);
            body.put("amount", voucherPrice);
        } catch (Exception ignored) {}

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                APIHelper.PAYMENT_VIP_MOMO,
                body,
                response -> {
                    try {
                        String payUrl    = response.optString("payUrl", "");
                        String qrCodeUrl = response.optString("qrCodeUrl", "");

                        if (qrCodeUrl.isEmpty() && payUrl.isEmpty()) {
                            btnConfirmPayment.setEnabled(true);
                            Toast.makeText(this, "Không nhận được thông tin thanh toán", Toast.LENGTH_LONG).show();
                            return;
                        }

                        currentPayUrl = payUrl;

                        if (!qrCodeUrl.isEmpty()) {
                            // Có QR string → render thành Bitmap và hiện dialog
                            showQrDialog(qrCodeUrl, payUrl);
                        } else {
                            // Fallback: không có QR → mở MoMo browser
                            openMomoBrowser(payUrl);
                        }

                    } catch (Exception e) {
                        btnConfirmPayment.setEnabled(true);
                        Toast.makeText(this, "Lỗi xử lý phản hồi", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    btnConfirmPayment.setEnabled(true);
                    String msg = "Lỗi kết nối server";
                    if (error.networkResponse != null && error.networkResponse.statusCode == 400) {
                        msg = "Thông tin gói không hợp lệ";
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Authorization", "Bearer " + APIHelper.getToken(VipCheckoutActivity.this));
                return h;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

    private void buyVipWithWallet() {
        JSONObject body = new JSONObject();
        try {
            body.put("voucherId", voucherId);
        } catch (Exception ignored) {}

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                APIHelper.BUY_VOUCHER_WITH_WALLET,
                body,
                response -> {
                    btnPayWallet.setEnabled(true);
                    if ("SUCCESS".equals(response.optString("message"))) {
                        addVipNotification();
                        Toast.makeText(this, "Đăng ký gói VIP thành công!", Toast.LENGTH_LONG).show();
                        Intent mainIntent = new Intent();
                        mainIntent.setClassName(this, "com.example.qride.MainActivity");
                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(mainIntent);
                        finish();
                    } else {
                        Toast.makeText(this, response.optString("message", "Thanh toán không thành công"), Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    btnPayWallet.setEnabled(true);
                    String msg = "Lỗi thanh toán bằng ví";
                    if (error.networkResponse != null) {
                        if (error.networkResponse.statusCode == 402) {
                            msg = "Số dư ví không đủ";
                        } else if (error.networkResponse.statusCode == 404) {
                            msg = "Không tìm thấy ví hoặc gói VIP";
                        } else if (error.networkResponse.statusCode == 409) {
                            msg = "Bạn đang sử dụng gói VIP này";
                        }
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Content-Type", "application/json");
                h.put("Authorization", "Bearer " + APIHelper.getToken(VipCheckoutActivity.this));
                return h;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

    private void addVipNotification() {
        int userId = new UserDAO(this).getUserId();
        if (userId > 0) {
            notificationDAO.addNotification(
                    userId,
                    "Nâng cấp VIP thành công",
                    "Bạn đã đăng ký gói hội viên VIP thành công.",
                    "PROMOTION"
            );
        }
    }

    // ─────────────────────────────────────────────────────────────
    // BƯỚC 2: Render QR từ string (ZXing) rồi hiện Dialog
    // ─────────────────────────────────────────────────────────────
    private void showQrDialog(String qrCodeString, String payUrl) {
        qrDialog = new Dialog(this);
        qrDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        qrDialog.setContentView(R.layout.dialog_momo_qr);
        qrDialog.setCancelable(false);

        // Resize dialog chiều rộng
        Window w = qrDialog.getWindow();
        if (w != null) {
            w.setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.92f),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
            w.setBackgroundDrawableResource(android.R.color.transparent);
        }

        ImageView  ivQr         = qrDialog.findViewById(R.id.ivMomoQr);
        ProgressBar pbLoading   = qrDialog.findViewById(R.id.pbQrLoading);
        TextView   tvAmount     = qrDialog.findViewById(R.id.tvQrAmount);
        TextView   tvExpiry     = qrDialog.findViewById(R.id.tvQrExpiry);
        Button     btnOpenMomo  = qrDialog.findViewById(R.id.btnOpenMomo);
        Button     btnClose     = qrDialog.findViewById(R.id.btnCloseDialog);

        // Hiển thị số tiền
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        tvAmount.setText(nf.format(voucherPrice) + "đ");

        // Hiện loading, ẩn QR trong khi render
        pbLoading.setVisibility(View.VISIBLE);
        ivQr.setVisibility(View.GONE);

        // Render QR bitmap trên background thread
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

        // Countdown 15 phút
        startQrCountdown(tvExpiry);

        // Nút mở app MoMo
        btnOpenMomo.setOnClickListener(v -> openMomoBrowser(payUrl));

        // Nút đóng
        btnClose.setOnClickListener(v -> {
            if (qrCountdown != null) qrCountdown.cancel();
            qrDialog.dismiss();
            btnConfirmPayment.setEnabled(true);
        });

        qrDialog.show();
    }

    // ─────────────────────────────────────────────────────────────
    // BƯỚC 3: Tạo QR Bitmap từ chuỗi EMVCo (MoMo qrCodeUrl)
    // ─────────────────────────────────────────────────────────────
    private Bitmap generateQrBitmap(String content, int sizePx) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);

            int width  = matrix.getWidth();
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

    // ─────────────────────────────────────────────────────────────
    // Countdown 15 phút hiển thị thời gian còn lại
    // ─────────────────────────────────────────────────────────────
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
                tvExpiry.setText("Mã QR đã hết hạn. Vui lòng thử lại.");
                tvExpiry.setTextColor(0xFFE53935);
                if (qrDialog != null && qrDialog.isShowing()) {
                    qrDialog.findViewById(R.id.ivMomoQr).setAlpha(0.3f);
                }
                btnConfirmPayment.setEnabled(true);
            }
        }.start();
    }

    // ─────────────────────────────────────────────────────────────
    // Fallback: mở MoMo qua browser
    // ─────────────────────────────────────────────────────────────
    private void openMomoBrowser(String payUrl) {
        if (payUrl == null || payUrl.isEmpty()) return;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(payUrl));
        startActivity(intent);
    }

    // ─────────────────────────────────────────────────────────────
    // Xử lý deep link callback từ MoMo (redirectUrl)
    // ─────────────────────────────────────────────────────────────
    private void handleDeepLink(Intent intent) {
        Uri data = intent.getData();
        if (data == null) return;

        String resultCode = data.getQueryParameter("resultCode");
        if ("0".equals(resultCode)) {
            // Thành công – webhook IPN đã xử lý kích hoạt VIP ở server
            if (qrCountdown != null) qrCountdown.cancel();
            if (qrDialog != null && qrDialog.isShowing()) qrDialog.dismiss();

            Toast.makeText(this,
                    "🎉 Đăng ký gói VIP thành công! Ưu đãi sẽ có hiệu lực ngay.",
                    Toast.LENGTH_LONG).show();
            addVipNotification();

            Intent mainIntent = new Intent();
            mainIntent.setClassName(this, "com.example.qride.MainActivity");
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(mainIntent);
            finish();
        } else if (resultCode != null) {
            // Thất bại
            if (qrDialog != null && qrDialog.isShowing()) qrDialog.dismiss();
            btnConfirmPayment.setEnabled(true);
            Toast.makeText(this, "Thanh toán MoMo không thành công. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
        }
    }
}
