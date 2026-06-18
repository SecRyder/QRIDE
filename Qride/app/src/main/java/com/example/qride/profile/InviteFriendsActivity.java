package com.example.qride.profile;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.R;
import com.example.qride.helper.APIHelper;
import com.example.qride.login.activity.LoginTaiKhoanActivity;
import com.example.qride.sqlite.UserDAO;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class InviteFriendsActivity extends AppCompatActivity {

    private static final String TAG = "InviteFriends";

    private TextView tvInviteCode;
    private TextView tvCodeLabel;
    private ProgressBar progressLoading;
    private EditText edtReferralCode;
    private Button btnCopy;
    private Button btnInvite;
    private Button btnSubmitCode;

    private String myReferralCode = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_friends);
// Fullscreen
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
        // Ánh xạ View
        tvInviteCode   = findViewById(R.id.tvInviteCode);
        tvCodeLabel    = findViewById(R.id.tvCodeLabel);
        progressLoading = findViewById(R.id.progressLoadingCode);
        edtReferralCode = findViewById(R.id.edtReferralCode);
        btnCopy        = findViewById(R.id.btnCopy);
        btnInvite      = findViewById(R.id.btnInvite);
        btnSubmitCode  = findViewById(R.id.btnSubmitCode);

        // Disable nút cho đến khi mã được tải xong
        setCodeActionsEnabled(false);

        // Listeners
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnCopy.setOnClickListener(v   -> copyCodeToClipboard());
        btnInvite.setOnClickListener(v -> shareInviteCode());
        btnSubmitCode.setOnClickListener(v -> submitReferralCode());

        // Bắt đầu tải mã giới thiệu từ server
        fetchMyProfile();
    }

    // ----------------------------------------------------------------
    // Tải mã giới thiệu từ server
    // ----------------------------------------------------------------
    private void fetchMyProfile() {
        String token = APIHelper.getToken(this);
        if (token == null || token.isEmpty()) {
            redirectToLogin();
            return;
        }

        showLoading(true);

        String url = APIHelper.USER;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    myReferralCode = response.optString("referral_code", "");
                    showLoading(false);
                    if (!myReferralCode.isEmpty()) {
                        tvInviteCode.setText(myReferralCode);
                        setCodeActionsEnabled(true);
                    } else {
                        tvInviteCode.setText("--");
                        Toast.makeText(this, "Chưa có mã giới thiệu", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    showLoading(false);
                    Log.e(TAG, "Error fetching profile: ", error);

                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        if (statusCode == 401) {
                            // Token hết hạn → xóa session và về màn login
                            new UserDAO(this).clearSession();
                            Toast.makeText(this, "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại", Toast.LENGTH_LONG).show();
                            redirectToLogin();
                        } else {
                            tvInviteCode.setText("--");
                            Toast.makeText(this, "Lỗi kết nối (HTTP " + statusCode + ")", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        tvInviteCode.setText("--");
                        Toast.makeText(this, "Không kết nối được server. Kiểm tra wifi/3G.", Toast.LENGTH_LONG).show();
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", "Bearer " + token);
                return params;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

    // ----------------------------------------------------------------
    // Nhập mã của người khác
    // ----------------------------------------------------------------
    private void submitReferralCode() {
        String code = edtReferralCode.getText().toString().trim().toUpperCase();
        if (code.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập mã mời", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!myReferralCode.isEmpty() && code.equals(myReferralCode)) {
            Toast.makeText(this, "Bạn không thể nhập mã của chính mình", Toast.LENGTH_SHORT).show();
            return;
        }

        String token = APIHelper.getToken(this);
        if (token == null || token.isEmpty()) {
            redirectToLogin();
            return;
        }

        btnSubmitCode.setEnabled(false);
        btnSubmitCode.setText("Đang xử lý...");

        JSONObject body = new JSONObject();
        try { body.put("code", code); } catch (Exception ignored) {}

        String url = APIHelper.BASE_URL + "referral/submit";
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body,
                response -> {
                    btnSubmitCode.setEnabled(true);
                    btnSubmitCode.setText("Xác nhận mã");
                    Toast.makeText(this, response.optString("message", "Thành công!"), Toast.LENGTH_LONG).show();
                    edtReferralCode.setText("");
                },
                error -> {
                    btnSubmitCode.setEnabled(true);
                    btnSubmitCode.setText("Xác nhận mã");

                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        if (statusCode == 401) {
                            new UserDAO(this).clearSession();
                            redirectToLogin();
                            return;
                        }
                        String msg = "Lỗi kết nối";
                        try {
                            JSONObject errObj = new JSONObject(new String(error.networkResponse.data));
                            msg = errObj.optString("message", msg);
                        } catch (Exception ignored) {}
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Không kết nối được server", Toast.LENGTH_SHORT).show();
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", "Bearer " + token);
                return params;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

    // ----------------------------------------------------------------
    // Copy mã vào clipboard
    // ----------------------------------------------------------------
    private void copyCodeToClipboard() {
        if (myReferralCode.isEmpty()) {
            Toast.makeText(this, "Chưa có mã để copy", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("InviteCode", myReferralCode);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Đã copy mã: " + myReferralCode, Toast.LENGTH_SHORT).show();
    }

    // ----------------------------------------------------------------
    // Chia sẻ mã ra ngoài
    // ----------------------------------------------------------------
    private void shareInviteCode() {
        if (myReferralCode.isEmpty()) {
            Toast.makeText(this, "Chưa có mã để chia sẻ", Toast.LENGTH_SHORT).show();
            return;
        }
        String shareBody = "Cùng mình trải nghiệm QRIDE và nhận ưu đãi!\nMã mời của mình: " + myReferralCode
                + "\nTải app QRIDE ngay hôm nay!";
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(intent, "Chia sẻ mã qua"));
    }

    // ----------------------------------------------------------------
    // UI helpers
    // ----------------------------------------------------------------
    private void showLoading(boolean show) {
        if (progressLoading != null) {
            progressLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (tvInviteCode != null) {
            tvInviteCode.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
        }
    }

    private void setCodeActionsEnabled(boolean enabled) {
        if (btnCopy   != null) btnCopy.setEnabled(enabled);
        if (btnInvite != null) btnInvite.setEnabled(enabled);
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginTaiKhoanActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}