package com.example.qride.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.qride.R;

public class NotificationSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);

        // Nút quay lại
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            finish();
        });

        // ========================================================
        // ĐOẠN ĐỔI CHỮ TIÊU ĐỀ CHO TỪNG SWITCH ITEM Ở ĐÂY:
        // ========================================================

        // 1. Thay đổi tiêu đề cho "Thông báo hệ thống"
        View swSystemView = findViewById(R.id.swSystem);
        TextView tvSystemTitle = swSystemView.findViewById(R.id.tvSwitchTitle);
        tvSystemTitle.setText("Thông báo hệ thống");

        // 2. Thay đổi tiêu đề cho "Thông báo giao dịch"
        View swTransactionView = findViewById(R.id.swTransaction);
        TextView tvTransactionTitle = swTransactionView.findViewById(R.id.tvSwitchTitle);
        tvTransactionTitle.setText("Thông báo giao dịch");

        // 3. Thay đổi tiêu đề cho "Thông báo khác"
        View swOtherView = findViewById(R.id.swOther);
        TextView tvOtherTitle = swOtherView.findViewById(R.id.tvSwitchTitle);
        tvOtherTitle.setText("Thông báo khác");

        // ========================================================
    }
}