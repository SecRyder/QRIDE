package com.example.qride.profile;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
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

public class InviteFriendsActivity extends AppCompatActivity {

    private TextView tvInviteCode;
    private EditText edtReferralCode;
    private String myReferralCode = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_friends);

        tvInviteCode = findViewById(R.id.tvInviteCode);
        edtReferralCode = findViewById(R.id.edtReferralCode);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        findViewById(R.id.btnCopy).setOnClickListener(v -> copyCodeToClipboard());
        
        findViewById(R.id.btnInvite).setOnClickListener(v -> shareInviteCode());

        findViewById(R.id.btnSubmitCode).setOnClickListener(v -> submitReferralCode());

        // Lấy mã giới thiệu thực từ Server
        fetchMyProfile();
    }

    private void fetchMyProfile() {
        String url = APIHelper.USER;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    myReferralCode = response.optString("referral_code", "KHONG_CO_MA");
                    tvInviteCode.setText(myReferralCode);
                },
                error -> tvInviteCode.setText("Lỗi tải mã")
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", "Bearer " + APIHelper.getToken(InviteFriendsActivity.this));
                return params;
            }
        };
        Volley.newRequestQueue(this).add(request);
    }

    private void submitReferralCode() {
        String code = edtReferralCode.getText().toString().trim().toUpperCase();
        if (code.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập mã", Toast.LENGTH_SHORT).show();
            return;
        }

        if (code.equals(myReferralCode)) {
            Toast.makeText(this, "Bạn không thể nhập mã của chính mình", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject body = new JSONObject();
        try { body.put("code", code); } catch (Exception ignored) {}

        String url = APIHelper.BASE_URL + "referral/submit";
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body,
                response -> {
                    Toast.makeText(this, response.optString("message", "Thành công"), Toast.LENGTH_LONG).show();
                    edtReferralCode.setText("");
                },
                error -> {
                    String msg = "Lỗi kết nối";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            JSONObject errObj = new JSONObject(new String(error.networkResponse.data));
                            msg = errObj.optString("message", msg);
                        } catch (Exception ignored) {}
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", "Bearer " + APIHelper.getToken(InviteFriendsActivity.this));
                return params;
            }
        };
        Volley.newRequestQueue(this).add(request);
    }

    private void copyCodeToClipboard() {
        if (myReferralCode.isEmpty()) return;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("InviteCode", myReferralCode);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Đã copy: " + myReferralCode, Toast.LENGTH_SHORT).show();
    }

    private void shareInviteCode() {
        if (myReferralCode.isEmpty()) return;
        String shareBody = "Cùng mình trải nghiệm QRIDE và nhận ưu đãi! Mã mời của mình: " + myReferralCode;
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(intent, "Chia sẻ mã qua"));
    }
}