package com.example.qride.profile;

import android.content.SharedPreferences;
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

        // Fullscreen
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        // Nút quay lại
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Khởi tạo SharedPreferences để lưu cấu hình thông báo
        SharedPreferences notifyPref = getSharedPreferences("notification_settings", MODE_PRIVATE);

        View swSystemView = findViewById(R.id.swSystem);
        TextView tvSystemTitle = swSystemView.findViewById(R.id.tvSwitchTitle);
        tvSystemTitle.setText("Thông báo hệ thống");

        androidx.appcompat.widget.SwitchCompat switchSystem = swSystemView.findViewById(R.id.switchWidget);
        // Đọc trạng thái cũ (mặc định là true nếu muốn bật sẵn cho user khi mới cài app)
        switchSystem.setChecked(notifyPref.getBoolean("system_notify_status", true));
        // Lắng nghe và lưu khi gạt nút
        switchSystem.setOnCheckedChangeListener((buttonView, isChecked) -> {
            notifyPref.edit().putBoolean("system_notify_status", isChecked).apply();
        });



        View swTransactionView = findViewById(R.id.swTransaction);
        TextView tvTransactionTitle = swTransactionView.findViewById(R.id.tvSwitchTitle);
        tvTransactionTitle.setText("Thông báo giao dịch");

        androidx.appcompat.widget.SwitchCompat switchTransaction = swTransactionView.findViewById(R.id.switchWidget);
        // Đọc trạng thái cũ
        switchTransaction.setChecked(notifyPref.getBoolean("transaction_notify_status", true));
        // Lắng nghe và lưu khi gạt nút
        switchTransaction.setOnCheckedChangeListener((buttonView, isChecked) -> {
            notifyPref.edit().putBoolean("transaction_notify_status", isChecked).apply();
        });



        View swOtherView = findViewById(R.id.swOther);
        TextView tvOtherTitle = swOtherView.findViewById(R.id.tvSwitchTitle);
        tvOtherTitle.setText("Thông báo khác");

        androidx.appcompat.widget.SwitchCompat switchOther = swOtherView.findViewById(R.id.switchWidget);
        // Đọc trạng thái cũ
        switchOther.setChecked(notifyPref.getBoolean("other_notify_status", false));
        // Lắng nghe và lưu khi gạt nút
        switchOther.setOnCheckedChangeListener((buttonView, isChecked) -> {
            notifyPref.edit().putBoolean("other_notify_status", isChecked).apply();
        });

    }
}