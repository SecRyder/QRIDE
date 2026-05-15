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

        setupMenuItem(view, R.id.menuInfo, getString(R.string.menu_info), v -> {
            Intent intent = new Intent(requireActivity(), UserInfoActivity.class);
            startActivity(intent);
        });

        setupMenuItem(view, R.id.menuSecurity, getString(R.string.menu_security), v -> {
            Intent intent = new Intent(requireActivity(), SecurityActivity.class);
            startActivity(intent);
        });

        setupMenuItem(view, R.id.menuHistory, getString(R.string.menu_history), v -> {
            Intent intent = new Intent(requireActivity(), com.example.qride.profile.RideHistoryActivity.class);
            startActivity(intent);
        });

        setupMenuItem(view, R.id.menuLanguage, getString(R.string.menu_language), v -> {
            Intent intent = new Intent(requireActivity(), ChangeLanguageActivity.class);
            startActivity(intent);
        });

        setupMenuItem(view, R.id.menuDarkMode, getString(R.string.menu_dark_mode), v -> {
            Intent intent = new Intent(requireActivity(), com.example.qride.profile.DarkModeActivity.class);
            startActivity(intent);
        });
        setupMenuItem(view, R.id.menuNotify, getString(R.string.menu_notify), v -> {
            Intent intent = new Intent(requireActivity(), com.example.qride.profile.NotificationSettingsActivity.class);
            startActivity(intent);
        });
        setupMenuItem(view, R.id.menuInvite, getString(R.string.menu_invite), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(requireActivity(), com.example.qride.profile.InviteFriendsActivity.class);
                startActivity(intent);
            }
        });
        setupMenuItem(view, R.id.menuSupport, getString(R.string.menu_support), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(requireActivity(), com.example.qride.profile.SupportCenterActivity.class);
                startActivity(intent);
            }
        });
        setupMenuItem(view, R.id.menuAbout, getString(R.string.menu_about), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(requireActivity(), com.example.qride.profile.AboutQRideActivity.class);
                startActivity(intent);
            }
        });
        setupMenuItem(view, R.id.menuTerms, getString(R.string.menu_terms), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(requireActivity(), com.example.qride.profile.TermsActivity.class);
                startActivity(intent);
            }
        });

        View btnLogout = view.findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                // 1. Xóa session SQLite
                userDAO.clearSession();

                // 2. Xử lý SharedPreferences
                SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("login_check", Context.MODE_PRIVATE);
                boolean isRemembered = sharedPreferences.getBoolean("remember", false);
                SharedPreferences.Editor editor = sharedPreferences.edit();

                if (!isRemembered) {
                    // Nếu trước đó KHÔNG tích ghi nhớ -> Xóa hết để quay về Login là trống trơn
                    editor.clear();
                } else {
                    // Nếu CÓ tích ghi nhớ -> Không clear hết mà chỉ chuyển trạng thái
                    // (Giữ lại phone và password trong máy để màn Login tự lấy ra điền vào ô)
                }
                editor.apply();

                Toast.makeText(requireContext(), getString(R.string.logout_success), Toast.LENGTH_SHORT).show();

                // 3. Chuyển về màn hình Login
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
                        Toast.makeText(requireContext(), getString(R.string.msg_developing) + ": " + title, Toast.LENGTH_SHORT).show()
                );
            }
        }
    }
}
