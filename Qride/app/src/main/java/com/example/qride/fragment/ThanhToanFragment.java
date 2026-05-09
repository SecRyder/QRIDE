package com.example.qride.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
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
        // return inflater.inflate(R.layout.fragment_thanh_toan, container, false);
        View view = inflater.inflate(R.layout.fragment_thanh_toan, container, false);

        btnNapTien = view.findViewById(R.id.btnNapTien);
        btnRutTien = view.findViewById(R.id.btnRutTien);
        btnLichSu = view.findViewById(R.id.btnLichSu);
        tvBalance = view.findViewById(R.id.tvBalance);
        btnToggleBalance = view.findViewById(R.id.btnToggleBalance);

//        btnNapTien.setOnClickListener(v -> {
//            showTopupDialog();
//        });
        btnNapTien.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), NapTienActivity.class));
        });
        btnRutTien.setOnClickListener(v->{
            startActivity(new Intent(getContext(), RutTienActivity.class));
        });
        btnLichSu.setOnClickListener(v->{
            startActivity(new Intent(getContext(), TransactionHistoryActivity.class));
        });
        btnToggleBalance.setOnClickListener(v -> {
            isHidden = !isHidden;

            if (isHidden) {
                tvBalance.setText("••••••");
                btnToggleBalance.setImageResource(R.drawable.hide);
            } else {
                tvBalance.setText(currentBalance + " VND");
                btnToggleBalance.setImageResource(R.drawable.show);
            }
        });
        return view;
    }

    private void showTopupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_topup, null);

        EditText edtAmount = view.findViewById(R.id.edtAmount);

        builder.setView(view)
                .setTitle("Nạp tiền")
                .setPositiveButton("Nạp", (dialog, which) -> {
                    String amountStr = edtAmount.getText().toString();
                    if (amountStr.isEmpty()) {
                        Toast.makeText(getContext(), "Nhập số tiền", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int amount;
                    try {
                        amount = Integer.parseInt(amountStr);
                        if (amount <= 0) throw new Exception();
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    callTopupAPI(amount);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void callTopupAPI(int amount) {
        String url = APIHelper.BASE_URL + "wallet/topup";

        String token = APIHelper.getToken(getContext());

        if (token == null || token.isEmpty()) {
            Toast.makeText(getContext(), "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject body = new JSONObject();
        try {
            body.put("amount", amount);
        } catch (Exception e) {
            e.printStackTrace();
        }

        RequestQueue queue = Volley.newRequestQueue(getContext());

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                body,

                response -> {
                    if ("SUCCESS".equals(response.optString("message"))) {

                        int balance = response.optInt("balance");

                        Toast.makeText(getContext(),
                                "Nạp +" + amount + " VND",
                                Toast.LENGTH_SHORT).show();

                        tvBalance.setText(balance + " VND");
                        loadWallet();

                    } else {
                        Toast.makeText(getContext(),
                                response.optString("message"),
                                Toast.LENGTH_SHORT).show();
                    }
                },

                error -> {
                    if (error.networkResponse != null) {
                        int code = error.networkResponse.statusCode;
                        String msg = new String(error.networkResponse.data);

                        android.util.Log.e("TOPUP_ERROR", msg);

                        Toast.makeText(getContext(),
                                "Lỗi server: " + code,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(),
                                "Không kết nối server",
                                Toast.LENGTH_SHORT).show();
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

        String url = APIHelper.BASE_URL + "wallet";

        // ===== LẤY TOKEN =====
        String token = APIHelper.getToken(getContext());

        if (token == null || token.isEmpty()) {
            Toast.makeText(getContext(), "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(getContext());

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,

                // ===== SUCCESS =====
                response -> {
                    try {
                        long balance = response.optLong("balance", -1);

                        if (balance == -1) {
                            Toast.makeText(getContext(), "Dữ liệu ví không hợp lệ", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        currentBalance = balance;
                        if (isHidden) {
                            tvBalance.setText("******");
                        } else {
                            tvBalance.setText(balance + " VND");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Lỗi xử lý dữ liệu ví", Toast.LENGTH_SHORT).show();
                    }
                },

                // ===== ERROR =====
                error -> {
                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;

                        try {
                            String body = new String(error.networkResponse.data);
                            android.util.Log.e("WALLET_ERROR", body);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (statusCode == 401) {
                            Toast.makeText(getContext(), "Hết phiên đăng nhập, vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Lỗi server: " + statusCode, Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(getContext(), "Không kết nối được server", Toast.LENGTH_SHORT).show();
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

    @Override
    public void onResume() {
        super.onResume();
        loadWallet();
    }

}
