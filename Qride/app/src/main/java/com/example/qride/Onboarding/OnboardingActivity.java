package com.example.qride.Onboarding;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.qride.R;
import com.example.qride.login.activity.LoginActivity;
import com.example.qride.login.activity.PermissionActivity;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private LinearLayout layoutIndicators;
    private Button btnContinue;
    private OnboardingAdapter adapter;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        initViews();
        setupOnboardingItems();
        setupListeners();
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        layoutIndicators = findViewById(R.id.layoutIndicators);
        btnContinue = findViewById(R.id.btnContinue);
        btnBack = findViewById(R.id.btnBack);
        btnBack.setVisibility(View.GONE);
    }

    private void setupOnboardingItems() {
        List<OnboardingItem> items = new ArrayList<>();
        items.add(new OnboardingItem(R.drawable.kinhlup, "Tìm xe", "Tìm xe và định vị xe ở trạm gần bạn chỉ với một lần chạm."));
        items.add(new OnboardingItem(R.drawable.qr, "Quét mã QR", "Quét mã QR được dán trên xe để mở khóa và bắt đầu chuyến đi."));
        items.add(new OnboardingItem(R.drawable.batdau, "Bắt đầu hành trình", "Sau khi mở khóa, hãy thoải mái tận hưởng chuyến đi của bạn!"));
        items.add(new OnboardingItem(R.drawable.map, "Kết thúc hành trình", "Tìm trạm xe gần nhất, bấm Kết thúc và xác nhận trả xe trên ứng dụng."));

        adapter = new OnboardingAdapter(items);
        viewPager.setAdapter(adapter);

        setupIndicators(items.size());
        setCurrentIndicator(0);
    }

    private void setupListeners() {
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setCurrentIndicator(position);
                
                // Ẩn/hiện nút Back
                btnBack.setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);
                
                // Đổi text nút Tiếp tục ở trang cuối
                if (position == adapter.getItemCount() - 1) {
                    btnContinue.setText("Bắt đầu");
                } else {
                    btnContinue.setText("Tiếp tục");
                }
            }
        });

        btnContinue.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() + 1 < adapter.getItemCount()) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            } else {
                navigateToNext();
            }
        });

        findViewById(R.id.tvSkip).setOnClickListener(v -> navigateToNext());

        btnBack.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() > 0) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
            }
        });
    }

    private void navigateToNext() {
        // Đánh dấu đã xem Onboarding
        setOnboardingFinished();

        if (areAllPermissionsGranted()) {
            // Nếu đã đủ quyền -> Vào thẳng Login
            startActivity(new Intent(this, LoginActivity.class));
        } else {
            // Nếu thiếu quyền -> Sang màn hình xin quyền
            startActivity(new Intent(this, PermissionActivity.class));
        }
        finish();
    }

    private void setOnboardingFinished() {
        SharedPreferences sharedPreferences = getSharedPreferences("Onboarding", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("Finished", true);
        editor.apply();
    }

    private boolean areAllPermissionsGranted() {
        // 1. Thông báo (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) return false;
        }

        // 2. Bluetooth
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        } else {
            // Android < 12 cần quyền BLUETOOTH cơ bản
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        // 3. Vị trí
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        return true;
    }

    private void setupIndicators(int count) {
        ImageView[] indicators = new ImageView[count];
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(20, 20);
        params.setMargins(8, 0, 8, 0);
        layoutIndicators.removeAllViews(); // Đảm bảo không bị trùng lặp
        for (int i = 0; i < count; i++) {
            indicators[i] = new ImageView(this);
            indicators[i].setImageResource(R.drawable.indicator_inactive);
            indicators[i].setLayoutParams(params);
            layoutIndicators.addView(indicators[i]);
        }
    }

    private void setCurrentIndicator(int index) {
        int childCount = layoutIndicators.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ImageView imageView = (ImageView) layoutIndicators.getChildAt(i);
            if (i == index) {
                imageView.setImageResource(R.drawable.indicator_active);
            } else {
                imageView.setImageResource(R.drawable.indicator_inactive);
            }
        }
    }
}
