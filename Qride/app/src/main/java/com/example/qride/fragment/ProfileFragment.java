package com.example.qride.fragment;

import static com.example.qride.helper.APIHelper.USER;
import static com.example.qride.helper.APIHelper.getToken;
import static com.example.qride.helper.APIHelper.USER_STATS;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.qride.R;
import com.example.qride.profile.ChangeLanguageActivity;
import com.example.qride.profile.SecurityActivity;
import com.example.qride.profile.UserInfoActivity;
import com.example.qride.login.activity.LoginTaiKhoanActivity;
import com.example.qride.sqlite.UserDAO;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private TextView tvProfileName, tvProfilePhone;
    private UserDAO userDAO;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_profile, container, false);

        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfilePhone = view.findViewById(R.id.tvProfilePhone);

        userDAO = new UserDAO(requireContext());

        CardView cardAvatar = view.findViewById(R.id.cardAvatar);
        if (cardAvatar != null) {
            cardAvatar.setOnClickListener(v -> {
                Intent intent = new Intent(requireActivity(), UserInfoActivity.class);
                startActivity(intent);
            });
        }

        // Nhóm 1: Dùng Lambda (viết ngắn gọn)
        setupMenuItem(view, R.id.menuInfo, getString(R.string.menu_info), R.drawable.ic_user, v -> {
            startActivity(new Intent(requireActivity(), UserInfoActivity.class));
        });

        setupMenuItem(view, R.id.menuSecurity, getString(R.string.menu_security), R.drawable.ic_security, v -> {
            startActivity(new Intent(requireActivity(), SecurityActivity.class));
        });

        setupMenuItem(view, R.id.menuHistory, getString(R.string.menu_history), R.drawable.ic_history, v -> {
            startActivity(new Intent(requireActivity(), com.example.qride.profile.RideHistoryActivity.class));
        });

        setupMenuItem(view, R.id.menuLanguage, getString(R.string.menu_language), R.drawable.ic_language, v -> {
            startActivity(new Intent(requireActivity(), ChangeLanguageActivity.class));
        });

        setupMenuItem(view, R.id.menuDarkMode, getString(R.string.menu_dark_mode), R.drawable.ic_night_mode, v -> {
            startActivity(new Intent(requireActivity(), com.example.qride.profile.DarkModeActivity.class));
        });

        setupMenuItem(view, R.id.menuNotify, getString(R.string.menu_notify), R.drawable.ic_bellll, v -> {
            startActivity(new Intent(requireActivity(), com.example.qride.profile.NotificationSettingsActivity.class));
        });

// Nhóm 2: Dùng Anonymous Class (Cần sửa lại thứ tự tham số cho khớp)
        setupMenuItem(view, R.id.menuInvite, getString(R.string.menu_invite), R.drawable.ic_add_friend, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(requireActivity(), com.example.qride.profile.InviteFriendsActivity.class));
            }
        });

        setupMenuItem(view, R.id.menuSupport, getString(R.string.menu_support), R.drawable.ic_costumer_service, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(requireActivity(), com.example.qride.profile.SupportCenterActivity.class));
            }
        });

        setupMenuItem(view, R.id.menuAbout, getString(R.string.menu_about), R.drawable.ic_about, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(requireActivity(), com.example.qride.profile.AboutQRideActivity.class));
            }
        });

        setupMenuItem(view, R.id.menuTerms, getString(R.string.menu_terms), R.drawable.ic_term, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(requireActivity(), com.example.qride.profile.TermsActivity.class));
            }
        });

        View btnLogout = view.findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                // 1. Xóa sạch session trong SQLite
                // Đảm bảo hàm clearSession() hoặc clearData() của bạn xóa sạch bảng user_session
                userDAO.clearSession();

                // 2. Xử lý SharedPreferences
                SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("login_check", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();

                boolean isRemembered = sharedPreferences.getBoolean("remember", false);
                String savedPhone = sharedPreferences.getString("phone", "");
                String savedPass = sharedPreferences.getString("password", "");

                editor.clear(); // Xóa sạch tất cả (bao gồm cả Token và trạng thái Login)

                if (isRemembered) {
                    // Nếu có ghi nhớ, ta nạp lại phone và pass vào để màn Login tự điền
                    editor.putBoolean("remember", true);
                    editor.putString("phone", savedPhone);
                    editor.putString("password", savedPass);
                }
                editor.apply();

                Toast.makeText(requireContext(), getString(R.string.logout_success), Toast.LENGTH_SHORT).show();

                // 3. Chuyển về màn hình Login và xóa sạch lịch sử các màn hình trước
                Intent intent = new Intent(requireActivity(), LoginTaiKhoanActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserProfile();
        loadUserStats();
    }

    private void loadUserStats() {
        String token = getToken(getContext());
        if (token == null || token.isEmpty()) return;

        com.android.volley.toolbox.JsonObjectRequest statsReq = new com.android.volley.toolbox.JsonObjectRequest(
                com.android.volley.Request.Method.GET,
                USER_STATS,
                null,
                response -> {
                    try {
                        if (!isAdded()) return;
                        android.view.View root = getView();
                        if (root == null) return;

                        android.widget.TextView tvTrips = root.findViewById(R.id.tvTrips);
                        android.widget.TextView tvKm = root.findViewById(R.id.tvKm);
                        android.widget.TextView tvHours = root.findViewById(R.id.tvHours);

                        if (tvTrips != null) tvTrips.setText(String.valueOf(response.optInt("trips", 0)));
                        if (tvKm != null) tvKm.setText(String.valueOf(response.optDouble("km", 0)));
                        if (tvHours != null) tvHours.setText(String.valueOf(response.optDouble("hours", 0)));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e("STATS", "Failed to load stats")
        ) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> h = new java.util.HashMap<>();
                h.put("Authorization", "Bearer " + token);
                return h;
            }
        };
        com.android.volley.toolbox.Volley.newRequestQueue(requireContext()).add(statsReq);
    }

    private void loadUserProfile() {
        String token = getToken(getContext());
        String phone = new UserDAO(requireContext()).getPhone();

        ImageView imgProfileAvatar = getView() != null ? getView().findViewById(R.id.imgProfileAvatar) : null;
        // Fallback sang SharedPreferences nếu SQLite trống
        if (phone == null || phone.isEmpty()) {
            SharedPreferences sp = requireActivity().getSharedPreferences("login_check", Context.MODE_PRIVATE);
            phone = sp.getString("phone", "");
        }
        
        if (phone == null || phone.isEmpty()) {
            tvProfileName.setText(getString(R.string.profile_guest));
            tvProfilePhone.setText(getString(R.string.profile_not_logged_in));
            return;
        }

        tvProfilePhone.setText("+84" + (phone.startsWith("0") ? phone.substring(1) : phone));
        String url = USER;
        RequestQueue queue =
                com.android.volley.toolbox.Volley.newRequestQueue(requireContext());
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        String name = response.optString("name", "");
                        if (!name.isEmpty() && !name.equals("null")) {
                            tvProfileName.setText(name);
                        } else {
                            tvProfileName.setText(getString(R.string.profile_no_name));
                        }

                        //  Load Avatar
                        String avatarBase64 = response.optString("avatar", "");
                        if (!avatarBase64.isEmpty() && !avatarBase64.equals("null") && imgProfileAvatar != null) {
                            byte[] decodedString = android.util.Base64.decode(avatarBase64, android.util.Base64.DEFAULT);
                            android.graphics.Bitmap decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            imgProfileAvatar.setImageBitmap(decodedByte);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    Log.e("API_ERROR", "Profile load failed");
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

    // Thêm tham số int iconResId vào cuối
    private void setupMenuItem(View parentView, int viewId, String title, int iconResId, View.OnClickListener listener) {
        View includedView = parentView.findViewById(viewId);
        if (includedView != null) {
            // Gán chữ
            TextView tvTitle = includedView.findViewById(R.id.tvMenuTitle);
            if (tvTitle != null) {
                tvTitle.setText(title);
            }

            // Gán Icon
            ImageView imgIcon = includedView.findViewById(R.id.imgMenuIcon);
            if (imgIcon != null) {
                imgIcon.setImageResource(iconResId);
            }

            // Gán sự kiện click
            if (listener != null) {
                includedView.setOnClickListener(listener);
            }
        }
    }
}
