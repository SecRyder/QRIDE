package com.example.qride;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.qride.fragment.NotificationFragment;
import com.example.qride.fragment.ProfileFragment;
import com.example.qride.fragment.QuetQRFragment;
import com.example.qride.fragment.ThanhToanFragment;
import com.example.qride.fragment.TramXeFragment;
import com.example.qride.fragment.UuDaiFragment;
import com.example.qride.sqlite.NotificationDAO;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private EditText etSearch;
    private ImageButton btnNotification;
    private TextView tvNotificationBadge;
    private CardView cardStationInfo;
    private TextView tvStationName, tvStationAddress;
    private FloatingActionButton btnLocateMe;
    private LinearLayout layoutTopBar;
    
    private View navTramXe, navUuDai, navQuetQR, navThanhToan, navTaiKhoan;
    private NotificationDAO notifDAO;

    public static class BikeStation {
        public String name;
        public String address;
        public int bikeCount;
        public LatLng location;

        public BikeStation(String name, String address, int bikeCount, LatLng location) {
            this.name = name;
            this.address = address;
            this.bikeCount = bikeCount;
            this.location = location;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        notifDAO = new NotificationDAO(this);
        initViews();
        setupBottomNav();
        setupSearch();
        updateNotifBadge();

        // ✅ Lắng nghe sự kiện quay lại Fragment để hiện/ẩn Header tự động
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragment instanceof TramXeFragment) {
                showMapUI(true);
                updateBottomNavUI(navTramXe);
            } else if (currentFragment instanceof UuDaiFragment) {
                showMapUI(false);
                updateBottomNavUI(navUuDai);
            }
            // ... thêm các fragment khác nếu cần
        });

        if (savedInstanceState == null) {
            replaceFragment(new TramXeFragment(), "TRAM_XE");
            updateBottomNavUI(navTramXe);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Luu thoi gian dong app
        SharedPreferences prefLogin = getSharedPreferences("login_check", MODE_PRIVATE);
        prefLogin.edit().putLong("lastCloseTime", System.currentTimeMillis()).apply();
    }

    private void initViews() {
        layoutTopBar    = findViewById(R.id.layoutTopBar);
        etSearch        = findViewById(R.id.etSearch);
        btnNotification = findViewById(R.id.btnNotification);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        cardStationInfo = findViewById(R.id.cardStationInfo);
        tvStationName   = findViewById(R.id.tvStationName);
        tvStationAddress= findViewById(R.id.tvStationAddress);
        btnLocateMe     = findViewById(R.id.btnLocateMe);

        navTramXe   = findViewById(R.id.navTramXe);
        navUuDai    = findViewById(R.id.navUuDai);
        navQuetQR   = findViewById(R.id.navQuetQR); 
        navThanhToan= findViewById(R.id.navThanhToan);
        navTaiKhoan = findViewById(R.id.navTaiKhoan);

        btnNotification.setOnClickListener(v -> {
            replaceFragment(new NotificationFragment(), "NOTIFICATIONS");
            showMapUI(false);
        });

        findViewById(R.id.btnRentBike).setOnClickListener(v -> 
                Toast.makeText(this, getString(R.string.msg_developing), Toast.LENGTH_SHORT).show());
        
        btnLocateMe.setOnClickListener(v -> {
            Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("TRAM_XE");
            if (currentFragment instanceof TramXeFragment) {
                ((TramXeFragment) currentFragment).locateMe();
            }
        });
    }

    public void updateNotifBadge() {
        int unread = notifDAO.getUnreadCount();
        if (unread > 0) {
            tvNotificationBadge.setText(String.valueOf(unread));
            tvNotificationBadge.setVisibility(View.VISIBLE);
        } else {
            tvNotificationBadge.setVisibility(View.GONE);
        }
    }

    private void setupBottomNav() {
        navTramXe.setOnClickListener(v -> {
            replaceFragment(new TramXeFragment(), "TRAM_XE");
            updateBottomNavUI(navTramXe);
            showMapUI(true);
        });

        navUuDai.setOnClickListener(v -> {
            replaceFragment(new UuDaiFragment(), "UU_DAI");
            updateBottomNavUI(navUuDai);
            showMapUI(false);
        });

        navQuetQR.setOnClickListener(v -> {
            replaceFragment(new QuetQRFragment(), "QUET_QR");
            updateBottomNavUI(navQuetQR);
            showMapUI(false);
        });

        navThanhToan.setOnClickListener(v -> {
            replaceFragment(new ThanhToanFragment(), "THANH_TOAN");
            updateBottomNavUI(navThanhToan);
            showMapUI(false);
        });

        navTaiKhoan.setOnClickListener(v -> {
            replaceFragment(new ProfileFragment(), "PROFILE");
            updateBottomNavUI(navTaiKhoan);
            showMapUI(false);
        });
    }

    private void replaceFragment(Fragment fragment, String tag) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        
        // Kiểm tra nếu Fragment đã có rồi thì không replace nữa (tránh load lại)
        Fragment existingFragment = fragmentManager.findFragmentByTag(tag);
        if (existingFragment != null && existingFragment.isVisible()) return;

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment, tag);
        
        // Chỉ add vào backstack cho trang Thông báo để nhấn Back quay lại trang trước
        if (tag.equals("NOTIFICATIONS")) {
            transaction.addToBackStack(null);
        }
        
        transaction.commit();
    }

    private void showMapUI(boolean show) {
        layoutTopBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLocateMe.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) hideStationCard();
    }

    public void showStationCard(String name, String address) {
        tvStationName.setText(name);
        tvStationAddress.setText(address);
        cardStationInfo.setVisibility(View.VISIBLE);
    }

    public void hideStationCard() {
        cardStationInfo.setVisibility(View.GONE);
    }

    private void setupSearch() {
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            String query = etSearch.getText().toString().trim();
            Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("TRAM_XE");
            if (currentFragment instanceof TramXeFragment && !query.isEmpty()) {
                ((TramXeFragment) currentFragment).searchStation(query);
            }
            return true;
        });
    }

    private void updateBottomNavUI(View selectedNav) {
        int colorDefault = Color.parseColor("#888888");
        int colorSelected = Color.parseColor("#00B087");

        changeNavColor(navTramXe, colorDefault);
        changeNavColor(navUuDai, colorDefault);
        changeNavColor(navThanhToan, colorDefault);
        changeNavColor(navTaiKhoan, colorDefault);

        if (selectedNav != navQuetQR) {
            changeNavColor(selectedNav, colorSelected);
        }
    }

    private void changeNavColor(View navView, int color) {
        if (navView instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) navView;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof ImageView) {
                    ((ImageView) child).setColorFilter(color);
                } else if (child instanceof TextView) {
                    ((TextView) child).setTextColor(color);
                } else if (child instanceof ViewGroup) {
                    changeNavColor(child, color);
                }
            }
        }
    }
}
