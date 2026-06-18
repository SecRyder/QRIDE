package com.example.qride.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.R;
import com.example.qride.adapter.RideHistoryAdapter;
import com.example.qride.helper.APIHelper;

import org.json.JSONArray;

import java.util.HashMap;
import java.util.Map;

public class RideHistoryActivity extends AppCompatActivity {

    private RecyclerView rvRides;
    private TextView tvEmpty;
    private RideHistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_history);
// Fullscreen
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        rvRides = findViewById(R.id.rvRides);
        tvEmpty = findViewById(R.id.tvEmpty);

        rvRides.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RideHistoryAdapter(new JSONArray());
        rvRides.setAdapter(adapter);

        loadRideHistory();
    }

    private void loadRideHistory() {
        String token = APIHelper.getToken(this);
        if (token == null || token.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText(getString(R.string.profile_not_logged_in));
            return;
        }

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                APIHelper.RIDES,
                null,
                response -> {
                    if (response.length() == 0) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        rvRides.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        rvRides.setVisibility(View.VISIBLE);
                        adapter.updateData(response);
                    }
                },
                error -> {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText(getString(R.string.error_server));
                    Toast.makeText(this, getString(R.string.error_server), Toast.LENGTH_SHORT).show();
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
}
