package com.example.qride.Onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.qride.R;
import com.example.qride.login.LoginActivity;

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

        viewPager = findViewById(R.id.viewPager);
        layoutIndicators = findViewById(R.id.layoutIndicators);
        btnContinue = findViewById(R.id.btnContinue);
        btnBack = findViewById(R.id.btnBack);

        // Mặc định lúc mới vào trang 1 thì ẩn nút Back đi
        btnBack.setVisibility(android.view.View.GONE);

        //  Tạo danh sách 4 trang
        List<OnboardingItem> items = new ArrayList<>();
        items.add(new OnboardingItem(R.drawable.kinhlup, "Tìm xe", "Tìm xe và định vị xe ở trạm gần bạn chỉ với một lần chạm."));
        items.add(new OnboardingItem(R.drawable.qr, "Quét mã QR", "Quét mã QR được dán trên xe để mở khóa và bắt đầu chuyến đi."));
        items.add(new OnboardingItem(R.drawable.batdau, "Bắt đầu hành trình", "Sau khi mở khóa, hãy thoải mái tận hưởng chuyến đi của bạn!"));
        items.add(new OnboardingItem(R.drawable.map, "Kết thúc hành trình", "Tìm trạm xe gần nhất, bấm Kết thúc và xác nhận trả xe trên ứng dụng."));

        adapter = new OnboardingAdapter(items);
        viewPager.setAdapter(adapter);

        // 2. Tạo dấu chấm (Indicators)
        setupIndicators(items.size());
        setCurrentIndicator(0);

        // 3. Xử lý khi vuốt trang
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setCurrentIndicator(position);
                // Nếu trang cuối, đổi chữ nút Tiếp tục thành "Bắt đầu và page 1 ko có nut back"
                if (position == 0) {
                    btnBack.setVisibility(android.view.View.INVISIBLE); // Trang 1 thì ẩn
                } else {
                    btnBack.setVisibility(android.view.View.VISIBLE); // Trang khác thì hiện
                }
                if (position == items.size() - 1) {
                    btnContinue.setText("Bắt đầu");
                } else {
                    btnContinue.setText("Tiếp tục");
                }
            }
        });

        // 4. Xử lý nút Tiếp tục
        btnContinue.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() + 1 < adapter.getItemCount()) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            } else {
                // Đã hết 4 trang -> Sang màn hình Login
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                finish();
            }
        });

        // 5. Nút Bỏ qua
        findViewById(R.id.tvSkip).setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            finish();
        });

        // 6. Nút Back
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (viewPager.getCurrentItem() > 0) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
            }
        });
    }

    private void setupIndicators(int count) {
        ImageView[] indicators = new ImageView[count];
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(20, 20);
        params.setMargins(8, 0, 8, 0);
        for (int i = 0; i < count; i++) {
            indicators[i] = new ImageView(getApplicationContext());
            indicators[i].setImageResource(R.drawable.indicator_inactive); // Cần tạo file drawable tròn xám
            indicators[i].setLayoutParams(params);
            layoutIndicators.addView(indicators[i]);
        }
    }

    private void setCurrentIndicator(int index) {
        int childCount = layoutIndicators.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ImageView imageView = (ImageView) layoutIndicators.getChildAt(i);
            if (i == index) {
                imageView.setImageResource(R.drawable.indicator_active); // Cần tạo file drawable tròn xanh
            } else {
                imageView.setImageResource(R.drawable.indicator_inactive);
            }
        }
    }
}
