package com.example.qride.thanhtoan;

import static com.example.qride.helper.APIHelper.WALLET_HISTORY;
import static com.example.qride.helper.APIHelper.getToken;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.R;
import com.example.qride.adapter.TransactionAdapter;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionHistoryActivity extends AppCompatActivity {
    RecyclerView rv;
    TransactionAdapter adapter;
    Button btnAll, btnPayment, btnTopup, btnWithdraw;
    List<Transaction> list = new ArrayList<>();
    List<Transaction> originalList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);
        // Fullscreen
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
        rv = findViewById(R.id.rvTransactions);
        btnAll = findViewById(R.id.btnAll);
        btnPayment = findViewById(R.id.btnPayment);
        btnTopup = findViewById(R.id.btnTopup);
        btnWithdraw = findViewById(R.id.btnWithdraw);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(this, list);
        rv.setAdapter(adapter);

        loadData();
        setupBack();

        btnAll.setOnClickListener(v -> {
            selectButton(btnAll);
            showAll();
        });

        btnPayment.setOnClickListener(v -> {
            selectButton(btnPayment);
            filter("payment");
        });

        btnTopup.setOnClickListener(v -> {
            selectButton(btnTopup);
            filter("topup");
        });

        btnWithdraw.setOnClickListener(v -> {
            selectButton(btnWithdraw);
            filter("withdraw");
        });
        selectButton(btnAll);
    }

    private void loadData() {
        String url = WALLET_HISTORY;
        String token = getToken(this);

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    list.clear();
                    originalList.clear();

                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject o = response.getJSONObject(i);

                            Transaction t = new Transaction(
                                    o.getInt("id"),
                                    o.getString("type"),
                                    o.getLong("amount"),
                                    o.optString("description", ""),
                                    o.getString("created_at"),
                                    o.isNull("rental_id") ? null : o.getInt("rental_id")
                            );

                            list.add(t);
                            originalList.add(t);

                        } catch (Exception e) {
                        }
                    }

                    adapter.notifyDataSetChanged();
                },
                error -> Toast.makeText(this, "Lỗi load data", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Authorization", "Bearer " + token);
                return h;
            }
        };

        queue.add(request);
    }

    // ===== FILTER ====
    private void filter(String type) {
        list.clear();
        for (Transaction t : originalList) {
            if (t.type.equals(type)) {
                list.add(t);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void setupBack() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    // Ham doi trang thai khi chon cac nut
    private void selectButton(Button selected) {
        btnAll.setSelected(false);
        btnPayment.setSelected(false);
        btnTopup.setSelected(false);
        btnWithdraw.setSelected(false);

        selected.setSelected(true);
    }

    private void showAll() {
        list.clear();
        list.addAll(originalList);
        adapter.notifyDataSetChanged();
    }
}
