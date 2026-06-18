package com.example.qride.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.qride.R;

public class SupportCenterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support_center);
// Fullscreen
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );


        // 1. Xử lý nút Gọi điện
        findViewById(R.id.btnCall).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:1900123456"));
            startActivity(intent);
        });

        // 2. Xử lý nút Chat
        findViewById(R.id.btnChat).setOnClickListener(v -> {
            String url = "https://m.me/your_fanpage";
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        });

        // 3. Setup mục "Thắc mắc đi xe" (Có 2 cấp)
        setupExpandableLevel2();

        // 4. Setup các mục khác (Chỉ 1 cấp)

        setupSimpleExpand(R.id.faqPayment, getString(R.string.faq_payment), getString(R.string.faq_payment_content));
        setupSimpleExpand(R.id.faqOther, getString(R.string.faq_other), getString(R.string.faq_other_content));
        // Nút Back
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void setupExpandableLevel2() {
        View faqBike = findViewById(R.id.faqBike);
        TextView tvTitle = faqBike.findViewById(R.id.tvTitle);
        tvTitle.setText(getString(R.string.faq_bike));

        LinearLayout layoutContent = faqBike.findViewById(R.id.layoutContent);
        View layoutHeader = faqBike.findViewById(R.id.layoutHeader);
        ImageView imgArrow = faqBike.findViewById(R.id.imgArrow);

        // Đóng mở Cấp 1
        layoutHeader.setOnClickListener(v -> {
            boolean isVisible = layoutContent.getVisibility() == View.VISIBLE;
            layoutContent.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            imgArrow.setRotation(isVisible ? 0 : 180);

            // XỬ LÝ ĐỔI MÀU THANH CHA Ở ĐÂY
            // Nếu đang mở (chuẩn bị ẩn) -> Về lại xanh. Nếu đang đóng (chuẩn bị mở) -> Sang cam
            layoutHeader.setBackgroundResource(isVisible ? R.drawable.btn_outline_green : R.drawable.btn_solid_green);
        });

        // Add mục Cấp 2 vào bên trong
        addSubFaq(layoutContent,
                getString(R.string.how_to_rent_title),
                getString(R.string.how_to_rent_content));
        addSubFaq(layoutContent,
                getString(R.string.working_time_title),
                getString(R.string.working_time_content));
    }

    private void addSubFaq(LinearLayout parent, String question, String answer) {
        View subView = getLayoutInflater().inflate(R.layout.item_sub_faq, null);
        TextView tvSubTitle = subView.findViewById(R.id.tvSubTitle);
        TextView tvAnswer = subView.findViewById(R.id.tvAnswer);
        ImageView imgSubArrow = subView.findViewById(R.id.imgSubArrow);
        View layoutSubHeader = subView.findViewById(R.id.layoutSubHeader);

        tvSubTitle.setText(question);
        tvAnswer.setText(answer);

        // Đóng mở Cấp 2
        layoutSubHeader.setOnClickListener(v -> {
            boolean isVisible = tvAnswer.getVisibility() == View.VISIBLE;
            tvAnswer.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            imgSubArrow.setRotation(isVisible ? 0 : 180);

        });

        parent.addView(subView);
    }

    private void setupSimpleExpand(int id, String title, String contentText) {
        View itemView = findViewById(id);
        ((TextView)itemView.findViewById(R.id.tvTitle)).setText(title);

        LinearLayout layoutContent = itemView.findViewById(R.id.layoutContent);
        TextView tv = new TextView(this);
        tv.setText(contentText);
        tv.setPadding(50, 10, 0, 10);
        layoutContent.addView(tv);

        View layoutHeader = itemView.findViewById(R.id.layoutHeader);
        ImageView imgArrow = itemView.findViewById(R.id.imgArrow);

        layoutHeader.setOnClickListener(v -> {
            boolean isVisible = layoutContent.getVisibility() == View.VISIBLE;
            layoutContent.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            imgArrow.setRotation(isVisible ? 0 : 180);

            // XỬ LÝ ĐỔI MÀU TƯƠNG TỰ CHO CÁC MỤC KHÁC
            layoutHeader.setBackgroundResource(isVisible ? R.drawable.btn_outline_green : R.drawable.btn_solid_green);
        });
    }
}