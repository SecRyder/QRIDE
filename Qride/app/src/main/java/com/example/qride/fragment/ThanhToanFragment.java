package com.example.qride.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.R;
import com.example.qride.helper.APIHelper;
import com.example.qride.thanhtoan.NapTienActivity;
import com.example.qride.thanhtoan.RutTienActivity;
import com.example.qride.thanhtoan.TransactionHistoryActivity;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ThanhToanFragment extends Fragment {
    LinearLayout btnNapTien, btnRutTien, btnLichSu;
    private TextView tvBalance;
    private ImageView btnToggleBalance;
    private boolean isHidden = false;
    private long currentBalance = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_thanh_toan, container, false);

        btnNapTien = view.findViewById(R.id.btnNapTien);
        btnRutTien = view.findViewById(R.id.btnRutTien);
        btnLichSu = view.findViewById(R.id.btnLichSu);
        tvBalance = view.findViewById(R.id.tvBalance);
        btnToggleBalance = view.findViewById(R.id.btnToggleBalance);

        btnNapTien.setOnClickListener(v -> {
            if (getContext() != null)
                startActivity(new Intent(getContext(), NapTienActivity.class));
        });
        btnRutTien.setOnClickListener(v->{
            if (getContext() != null)
                startActivity(new Intent(getContext(), RutTienActivity.class));
        });
        btnLichSu.setOnClickListener(v->{
            if (getContext() != null)
                startActivity(new Intent(getContext(), TransactionHistoryActivity.class));
        });

        btnToggleBalance.setOnClickListener(v -> {
            isHidden = !isHidden;
            if (isHidden) {
                tvBalance.setText("••••••");
                btnToggleBalance.setImageResource(R.drawable.hide);
            } else {
                tvBalance.setText(formatBalance(currentBalance));
                btnToggleBalance.setImageResource(R.drawable.show);
            }
        });
        return view;
    }

    // Helper method để hiển thị Toast an toàn trong Fragment
    private void showSafeToast(String message) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void callTopupAPI(int amount) {
        String url = APIHelper.BASE_URL + "wallet/topup";
        String token = APIHelper.getToken(getContext());

        if (token == null || token.isEmpty()) {
            showSafeToast("Chưa đăng nhập");
            return;
        }

        JSONObject body = new JSONObject();
        try {
            body.put("amount", amount);
        } catch (Exception e) {
            e.printStackTrace();
        }

        RequestQueue queue = Volley.newRequestQueue(requireContext());
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, url, body,
                response -> {
                    if ("SUCCESS".equals(response.optString("message"))) {
                        int balance = response.optInt("balance");
                        showSafeToast("Nạp +" + amount + " VND");
                        tvBalance.setText(formatBalance(balance));
                        loadWallet();
                    } else {
                        showSafeToast(response.optString("message"));
                    }
                },
                error -> {
                    if (error.networkResponse != null) {
                        int code = error.networkResponse.statusCode;
                        showSafeToast("Lỗi server: " + code);
                    } else {
                        showSafeToast("Không kết nối server");
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

    private void loadWallet() {
        if (getContext() == null) return;

        String url = APIHelper.BASE_URL + "wallet";
        String token = APIHelper.getToken(getContext());

        if (token == null || token.isEmpty()) {
            // Không show toast ở đây vì loadWallet chạy liên tục trong onResume, có thể gây phiền
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(requireContext());
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    // Kiểm tra Fragment còn gắn vào Activity không trước khi cập nhật UI
                    if (!isAdded()) return;

                    long balance = response.optLong("balance", -1);
                    if (balance == -1) {
                        showSafeToast("Dữ liệu ví không hợp lệ");
                        return;
                    }

                    currentBalance = balance;
                    if (isHidden) {
                        tvBalance.setText("******");
                    } else {
                        tvBalance.setText(formatBalance(balance));
                    }
                },
                error -> {
                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        if (statusCode == 401) {
                            showSafeToast("Hết phiên đăng nhập");
                        } else {
                            showSafeToast("Lỗi server: " + statusCode);
                        }
                    } else {
                        showSafeToast("Không kết nối được server");
                    }
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

    private String formatBalance(long balance) {
        java.text.NumberFormat formatter = java.text.NumberFormat.getNumberInstance(new java.util.Locale("vi", "VN"));
        return formatter.format(balance) + " VND";
    }

    @Override
    public void onResume() {
        super.onResume();
        loadWallet();
    }
}