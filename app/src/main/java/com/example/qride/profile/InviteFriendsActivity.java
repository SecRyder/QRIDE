package com.example.qride.profile;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.qride.R;

import java.util.Random;

public class InviteFriendsActivity extends AppCompatActivity {

    private TextView tvInviteCode;
    private View btnCopy;
    private View btnInvite;
    private String currentCode = ""; // Lưu mã hiện tại

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_friends);

        // 1. Ánh xạ các view
        initViews();

        // 2. Tạo mã mời ngẫu nhiên (Demo giao diện)
        // Trong thực tế, bạn sẽ gọi API lấy mã mời của User ở đây.
        currentCode = generateRandomInviteCode();
        tvInviteCode.setText(currentCode);

        // 3. Xử lý các sự kiện click
        setClickListeners();
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btnBack);
        tvInviteCode = findViewById(R.id.tvInviteCode);
        // Mình cho người dùng click vào cả ô chứa mã (background xanh nhạt) để copy
        btnCopy = (View) findViewById(R.id.tvInviteCode).getParent(); // Lấy LinearLayout cha
        btnInvite = findViewById(R.id.btnInvite);

        // Nút back xử lý luôn
        btnBack.setOnClickListener(v -> finish());
    }

    private void setClickListeners() {
        // Chức năng Copy mã
        btnCopy.setOnClickListener(v -> copyCodeToClipboard());

        // Chức năng Mời bạn bè (Share)
        btnInvite.setOnClickListener(v -> shareInviteCode());
    }

    /**
     * Copy mã mời hiện tại vào ClipBoard
     */
    private void copyCodeToClipboard() {
        if (currentCode.isEmpty()) return;

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("InviteCode", currentCode);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, "Đã copy mã mời: " + currentCode, Toast.LENGTH_SHORT).show();
    }

    /**
     * Mở Share Intent để chia sẻ thông tin mời
     */
    private void shareInviteCode() {
        if (currentCode.isEmpty()) return;

        // Nội dung tin nhắn mời (Tùy chỉnh nội dung bạn muốn)
        String shareBody = "Cùng mình trải nghiệm QRIDE - Dịch vụ thuê xe tiện lợi! " +
                "\nNhập mã mời của mình để nhận ưu đãi hấp dẫn nha: " + currentCode +
                "\n\nTải app tại đây: " + getAppLink();

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain"); // Định dạng gửi là văn bản thuần
        intent.putExtra(Intent.EXTRA_SUBJECT, "Mời bạn trải nghiệm QRIDE"); // Tiêu đề (tùy app share hỗ trợ)
        intent.putExtra(Intent.EXTRA_TEXT, shareBody); // Nội dung chính

        // Mở bộ chọn Share của Android (Cho phép chọn Zalo, Messenger, Email...)
        startActivity(Intent.createChooser(intent, "Mời bạn bè qua"));
    }

    /**
     * Demo hàm lấy link tải app (để trống hoặc điền link CHPlay thực tế)
     */
    private String getAppLink() {
        // Sau này bạn thay bằng link thực tế của app QRIDE trên Google Play
        return "https://play.google.com/store/apps/details?id=" + getPackageName();
    }

    /**
     * Hàm demo tạo mã mời ngẫu nhiên giống định dạng trong ảnh
     */
    private String generateRandomInviteCode() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder result = new StringBuilder();
        Random random = new Random();

        // Thêm "MBB" cố định lúc đầu cho giống demo
        result.append("MBB");

        // Tạo thêm 6 ký tự ngẫu nhiên
        for (int i = 0; i < 6; i++) {
            result.append(characters.charAt(random.nextInt(characters.length())));
        }

        // Thêm "V2" cố định lúc cuối
        result.append("V2");

        return result.toString();
    }
}