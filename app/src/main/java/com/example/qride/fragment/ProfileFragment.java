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
            Toast.makeText(requireContext(), getString(R.string.menu_history), Toast.LENGTH_SHORT).show();
        });

        setupMenuItem(view, R.id.menuLanguage, getString(R.string.menu_language), v -> {
            Intent intent = new Intent(requireActivity(), ChangeLanguageActivity.class);
            startActivity(intent);
        });

        setupMenuItem(view, R.id.menuDarkMode, getString(R.string.menu_dark_mode), null);
        setupMenuItem(view, R.id.menuNotify, getString(R.string.menu_notify), null);
        setupMenuItem(view, R.id.menuInvite, getString(R.string.menu_invite), null);
        setupMenuItem(view, R.id.menuSupport, getString(R.string.menu_support), null);
        setupMenuItem(view, R.id.menuAbout, getString(R.string.menu_about), null);
        setupMenuItem(view, R.id.menuTerms, getString(R.string.menu_terms), null);

        View btnLogout = view.findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("login_check", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear();
                editor.apply();

                Toast.makeText(requireContext(), getString(R.string.logout_success), Toast.LENGTH_SHORT).show();

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
    }

    private void loadUserProfile() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("login_check", Context.MODE_PRIVATE);
        String currentPhone = sharedPreferences.getString("phone", "");

        if (!currentPhone.isEmpty()) {
            tvProfilePhone.setText("+84" + currentPhone);

            Cursor cursor = userDAO.getUserInfo(currentPhone);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIdx = cursor.getColumnIndex("name");
                if (nameIdx != -1) {
                    String name = cursor.getString(nameIdx);
                    if (name != null && !name.isEmpty()) {
                        tvProfileName.setText(name);
                    } else {
                        tvProfileName.setText(getString(R.string.profile_no_name));
                    }
                }
                cursor.close();
            }
        } else {
            tvProfileName.setText(getString(R.string.profile_guest));
            tvProfilePhone.setText(getString(R.string.profile_not_logged_in));
        }
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
