package com.example.qride.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.qride.R;
import com.example.qride.profile.ChangeLanguageActivity;
import com.example.qride.profile.SecurityActivity;
import com.example.qride.profile.UserInfoActivity;
import com.example.qride.login.activity.LoginTaiKhoanActivity;
import com.example.qride.sqlite.UserDAO;

public class ProfileFragment extends Fragment {

    // Khai báo biến
    private TextView tvProfileName, tvProfilePhone;
    private UserDAO userDAO;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_profile, container, false);

        // Ánh xạ Tên và Số điện thoại
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfilePhone = view.findViewById(R.id.tvProfilePhone);

        userDAO = new UserDAO(requireContext());

        // 1. Xử lý nút bấm vào Avatar để mở màn hình chỉnh sửa
        CardView cardAvatar = view.findViewById(R.id.cardAvatar);
        if (cardAvatar != null) {
            cardAvatar.setOnClickListener(v -> {
                Intent intent = new Intent(requireActivity(), UserInfoActivity.class);
                startActivity(intent);
            });
        }

        // ==========================================================
        // 2. KHÔI PHỤC LẠI CÁC NÚT MENU (INCLUDE)
        // ==========================================================
        setupMenuItem(view, R.id.menuInfo, "Thông tin cá nhân", v -> {
            Intent intent = new Intent(requireActivity(), UserInfoActivity.class);
            startActivity(intent);
        });

        setupMenuItem(view, R.id.menuSecurity, "Bảo mật & Mật khẩu", v -> {
            Intent intent = new Intent(requireActivity(), SecurityActivity.class);
            startActivity(intent);
        });

        setupMenuItem(view, R.id.menuHistory, "Lịch sử chuyến đi", v -> {
            Toast.makeText(requireContext(), "Mở Lịch sử", Toast.LENGTH_SHORT).show();
        });

        setupMenuItem(view, R.id.menuLanguage, "Ngôn ngữ", v -> {
            Intent intent = new Intent(requireActivity(), ChangeLanguageActivity.class);
            startActivity(intent);
        });

        setupMenuItem(view, R.id.menuDarkMode, "Giao diện tối (Dark Mode)", null);
        setupMenuItem(view, R.id.menuNotify, "Cài đặt thông báo", null);
        setupMenuItem(view, R.id.menuInvite, "Mời bạn bè", null);
        setupMenuItem(view, R.id.menuSupport, "Trung tâm trợ giúp", null);
        setupMenuItem(view, R.id.menuAbout, "Về Q-Ride", null);
        setupMenuItem(view, R.id.menuTerms, "Điều khoản & Chính sách", null);

        // ==========================================================
        // 3. XỬ LÝ NÚT ĐĂNG XUẤT
        // ==========================================================
        View btnLogout = view.findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("login_check", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear();
                editor.apply();

                Toast.makeText(requireContext(), "Đã đăng xuất thành công!", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(requireActivity(), LoginTaiKhoanActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        }

        return view;
    }

    // Dùng onResume để tự động cập nhật lại giao diện mỗi khi quay lại trang này
    @Override
    public void onResume() {
        super.onResume();
        loadUserProfile();
    }

    // Hàm lấy dữ liệu từ DB và gán lên UI
    private void loadUserProfile() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("login_check", Context.MODE_PRIVATE);
        String currentPhone = sharedPreferences.getString("phone", "");

        if (!currentPhone.isEmpty()) {
            // Hiển thị số điện thoại
            tvProfilePhone.setText("+84" + currentPhone);

            // Truy vấn Database để lấy Tên
            Cursor cursor = userDAO.getUserInfo(currentPhone);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIdx = cursor.getColumnIndex("name");
                if (nameIdx != -1) {
                    String name = cursor.getString(nameIdx);
                    if (name != null && !name.isEmpty()) {
                        tvProfileName.setText(name);
                    } else {
                        tvProfileName.setText("Chưa cập nhật tên");
                    }
                }
                cursor.close();
            }
        } else {
            // Trường hợp lỗi rỗng phone (Phòng hờ)
            tvProfileName.setText("Khách");
            tvProfilePhone.setText("Chưa đăng nhập");
        }
    }

    // Hàm tiện ích đặt tên Menu
    private void setupMenuItem(View parentView, int viewId, String title, View.OnClickListener listener) {
        View includedView = parentView.findViewById(viewId);
        if (includedView != null) {
            TextView tvTitle = includedView.findViewById(R.id.tvMenuTitle);
            if (tvTitle != null) {
                tvTitle.setText(title);
            }
            if (listener != null) {
                includedView.setOnClickListener(listener);
            } else {
                includedView.setOnClickListener(v ->
                        Toast.makeText(requireContext(), "Đang phát triển: " + title, Toast.LENGTH_SHORT).show()
                );
            }
        }
    }
}