package com.example.qride;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.community.CommunityActivity;
import com.example.qride.fragment.NotificationFragment;
import com.example.qride.fragment.ProfileFragment;
import com.example.qride.fragment.QuetQRFragment;
import com.example.qride.fragment.ThanhToanFragment;
import com.example.qride.fragment.TramXeFragment;
import com.example.qride.fragment.UuDaiFragment;
import com.example.qride.helper.APIHelper;
import com.example.qride.profile.SupportCenterActivity;
import com.example.qride.sqlite.NotificationDAO;
import com.example.qride.thanhtoan.TransactionDetailActivity;
import com.example.qride.thuexe.ChiTietXeActivity;
import com.example.qride.thuexe.EndRideActivity;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.android.gms.location.*;

public class MainActivity extends AppCompatActivity implements TramXeFragment.OnVehicleLoadListener {

    private EditText etSearch;
    private ImageButton btnNotification;
    private TextView tvNotificationBadge;
    private CardView cardStationInfo;
    private TextView tvStationName, tvStationAddress;
    private FloatingActionButton btnLocateMe, btnCommunity;
    private LinearLayout layoutTopBar;

    private View navTramXe, navUuDai, navQuetQR, navThanhToan, navTaiKhoan;
    private NotificationDAO notifDAO;

    private CardView cardRiding;
    private View btnSupportRiding;
    private TextView tvBikeCode, tvBikeStatus, tvBikePin;

    private TextView tvTime, tvDistance, tvPrice;
    private Handler handler = new Handler();
    private Runnable runnable;
    private long startTime = 0;
    private Location lastLocation;
    private double totalDistance = 0;
    private int vehicleId = -1;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    public static class BikeStation {
        public int id;
        public String name;
        public String address;
        public int bikeCount;
        public LatLng location;

        public BikeStation(int id, String name, String address, int bikeCount, LatLng location) {
            this.id = id;
            this.name = name;
            this.address = address;
            this.bikeCount = bikeCount;
            this.location = location;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // 1. Lấy cấu hình Dark Mode đã lưu
        SharedPreferences darkPref = getSharedPreferences("Settings", MODE_PRIVATE);
        int savedMode = darkPref.getInt("Dark_Mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // 2. Áp dụng ngay lập tức TRƯỚC KHI nạp layout
        AppCompatDelegate.setDefaultNightMode(savedMode);


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestLocationPermission();

        notifDAO = new NotificationDAO(this);
        initViews();
        loadRideState();
        setupBottomNav();
        setupSearch();
        updateNotifBadge();

        // Lắng nghe sự kiện quay lại Fragment để hiện/ẩn Header tự động
        // Sửa lại đoạn này trong onCreate của bạn
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragment instanceof TramXeFragment) {
                showMapUI(true);
                updateBottomNavUI(navTramXe);
            } else if (currentFragment instanceof ProfileFragment) {
                showMapUI(false);
                updateBottomNavUI(navTaiKhoan);
            } else if (currentFragment instanceof UuDaiFragment) {
                showMapUI(false);
                updateBottomNavUI(navUuDai);
            } else if (currentFragment instanceof ThanhToanFragment) {
                showMapUI(false);
                updateBottomNavUI(navThanhToan);
            } else if (currentFragment instanceof QuetQRFragment) {
                showMapUI(false);
                updateBottomNavUI(navQuetQR);
            }
        });

        if (savedInstanceState == null) {
            replaceFragment(new TramXeFragment(), "TRAM_XE");
            updateBottomNavUI(navTramXe);
            showMapUI(true);
        } else {
            // FIX LỖI DARK MODE TẠI ĐÂY
            // Tìm xem Fragment nào đang hiện khi App vừa nạp lại
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

            if (currentFragment instanceof ProfileFragment) {
                showMapUI(false);
                updateBottomNavUI(navTaiKhoan);
            } else if (currentFragment instanceof TramXeFragment) {
                showMapUI(true);
                updateBottomNavUI(navTramXe);
            } else if (currentFragment instanceof UuDaiFragment) {
                showMapUI(false);
                updateBottomNavUI(navUuDai);
            } else if (currentFragment instanceof ThanhToanFragment) {
                showMapUI(false);
                updateBottomNavUI(navThanhToan);
            } else if (currentFragment instanceof QuetQRFragment) {
                showMapUI(false);
                updateBottomNavUI(navQuetQR);
            }
        }

        SharedPreferences pref = getSharedPreferences("ride_state", MODE_PRIVATE);
        boolean isRiding = getIntent().getBooleanExtra("isRiding", false);
        if (isRiding) {
            String plate = getIntent().getStringExtra("plate");
            String pin = getIntent().getStringExtra("pin");

            showRidingUI(plate, pin);
            showUnlockSuccessDialog();
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
        layoutTopBar = findViewById(R.id.layoutTopBar);
        etSearch = findViewById(R.id.etSearch);
        btnNotification = findViewById(R.id.btnNotification);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        cardStationInfo = findViewById(R.id.cardStationInfo);
        tvStationName = findViewById(R.id.tvStationName);
        tvStationAddress = findViewById(R.id.tvStationAddress);
        btnLocateMe = findViewById(R.id.btnLocateMe);
        btnCommunity = findViewById(R.id.btnCommunity);

        cardRiding = findViewById(R.id.cardRiding);
        btnSupportRiding = findViewById(R.id.btnSupportRiding);
        tvBikeCode = findViewById(R.id.tvBikeCode);
        tvBikeStatus = findViewById(R.id.tvBikeStatus);
        tvBikePin = findViewById(R.id.tvBikePin);
        tvTime = findViewById(R.id.tvTime);
        tvDistance = findViewById(R.id.tvDistance);
        tvPrice = findViewById(R.id.tvPrice);

        navTramXe = findViewById(R.id.navTramXe);
        navUuDai = findViewById(R.id.navUuDai);
        navQuetQR = findViewById(R.id.navQuetQR);
        navThanhToan = findViewById(R.id.navThanhToan);
        navTaiKhoan = findViewById(R.id.navTaiKhoan);

        btnNotification.setOnClickListener(v -> {
            replaceFragment(new NotificationFragment(), "NOTIFICATIONS");
            showMapUI(false);
        });

        btnLocateMe.setOnClickListener(v -> {
            Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("TRAM_XE");
            if (currentFragment instanceof TramXeFragment) {
                ((TramXeFragment) currentFragment).locateMe();
            }
        });

        btnCommunity.setOnClickListener(v->{
            Intent intent = new Intent(MainActivity.this, CommunityActivity.class);
            startActivity(intent);
        });
        if (btnSupportRiding != null) {
            btnSupportRiding.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, SupportCenterActivity.class)));
        }

        findViewById(R.id.btnEndRide).setOnClickListener(v -> endRide());

        // Animate QR Scanner Line
        View viewScannerLine = findViewById(R.id.viewScannerLine);
        if (viewScannerLine != null) {
            float distance = 7 * getResources().getDisplayMetrics().density;
            ObjectAnimator animator = ObjectAnimator.ofFloat(viewScannerLine, "translationY", -distance, distance);
            animator.setDuration(1500);
            animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.start();
        }
    }

    public void updateNotifBadge() {
        com.example.qride.sqlite.UserDAO userDAO = new com.example.qride.sqlite.UserDAO(this);
        int userId = userDAO.getUserId();
        if (userId == -1) {
            tvNotificationBadge.setVisibility(View.GONE);
            return;
        }

        int unread = notifDAO.getUnreadCount(userId);
        if (unread > 0) {
            tvNotificationBadge.setText(String.valueOf(unread));
            tvNotificationBadge.setVisibility(View.VISIBLE);
        } else {
            tvNotificationBadge.setVisibility(View.GONE);
        }
    }

    private void setupBottomNav() {

        View btnCircleQR = findViewById(R.id.btnTronQR);
        if (btnCircleQR != null) {
            btnCircleQR.setOnClickListener(v -> {
                replaceFragment(new QuetQRFragment(), "QUET_QR");
                updateBottomNavUI(navQuetQR); // Vẫn dùng navQuetQR để đổi màu toàn bộ icon/chữ Quét QR
                showMapUI(false);
            });
        }
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

//        navQuetQR.setOnClickListener(v -> {
//            replaceFragment(new QuetQRFragment(), "QUET_QR");
//            updateBottomNavUI(navQuetQR);
//            showMapUI(false);
//        });

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
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // Kiểm tra fragment hiện tại để tránh nạp lại trùng lặp
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
        if (currentFragment != null && tag.equals(currentFragment.getTag())) {
            return;
        }

        transaction.replace(R.id.fragment_container, fragment, tag);

        // Giữ lại backstack cho các màn hình phụ
        if (tag.equals("NOTIFICATIONS")) {
            transaction.addToBackStack(null);
        }

        transaction.commitAllowingStateLoss();
    }

    private void showMapUI(boolean show) {
        layoutTopBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLocateMe.setVisibility(show ? View.VISIBLE : View.GONE);
//        if (!show) hideStationCard();
    }

    public void showStationCard(String name, String address, int stationId) {
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

    // Ham dialog mo khoa thanh cong
    private void showUnlockSuccessDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_unlock_success, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        view.findViewById(R.id.btnOK).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // ======================== RIDE ==========================
    private void startRide(String plate, String pin, int vehicleId) {
        SharedPreferences pref = getSharedPreferences("ride_state", MODE_PRIVATE);
        pref.edit()
                .putBoolean("isRiding", true)
                .putString("plate", plate)
                .putString("pin", pin)
                .putInt("vehicleId", vehicleId)
                .putLong("startTime", System.currentTimeMillis())
                .apply();

        showRidingUI(plate, pin);
        loadRideState(); // đảm bảo sync
    }

    // Hien thi Card xe
    private void loadRideState() {
        SharedPreferences pref = getSharedPreferences("ride_state", MODE_PRIVATE);
        boolean isRiding = pref.getBoolean("isRiding", false);
        if (!isRiding) {
            cardRiding.setVisibility(View.GONE);
            stopTimer();
            return;
        }
        startTime = pref.getLong("startTime", -1);
        if (startTime <= 0) {
            clearRideState();
            return;
        }
        vehicleId = pref.getInt("vehicleId", -1);
        String plate = pref.getString("plate", "");
        String pin = pref.getString("pin", "");
        showRidingUI(plate, pin);
        startTimer();
    }

    private void showRidingUI(String plate, String pin) {
        cardRiding.setVisibility(View.VISIBLE);
        tvBikeCode.setText(plate);
        tvBikeStatus.setText("Đang di chuyển");
        tvBikePin.setText(pin + "%");
    }

    private void clearRideState() {
        getSharedPreferences("ride_state", MODE_PRIVATE).edit().clear().apply();
        stopTimer();


        cardRiding.setVisibility(View.GONE);
        totalDistance = 0;
        lastLocation = null;
    }



    // ============================ TIMER =========================
    private void startTimer() {
        if (runnable != null) return;
        runnable = () -> {
            long diff = System.currentTimeMillis() - startTime;
            if (diff < 0) diff = 0;
            int seconds = (int) (diff / 1000);
            int minutes = seconds / 60;
            int hours = minutes / 60;
            tvTime.setText(String.format("%02d:%02d:%02d",
                    hours, minutes % 60, seconds % 60));
            double km = totalDistance / 1000;
            tvDistance.setText(String.format("%.2f km", km));
            tvPrice.setText(calculatePrice(minutes, km) + "đ");
            handler.postDelayed(runnable, 1000);
        };
        handler.post(runnable);
    }

    private void stopTimer() {
        if (runnable != null) {
            handler.removeCallbacks(runnable);
            runnable = null;
        }
    }

    private int calculatePrice(int minutes, double km) {
        if (minutes < 0) minutes = 0;
        if (km < 0) km = 0;
        int price = (minutes <= 15)
                ? 5000
                : 5000 + ((minutes - 15) / 5) * 2000;
        price += (int) (km * 1000);
        return price;
    }

    // ============================ GPS ===================
    private void startLocationUpdates() {
        if (!isRiding()) return;

        LocationRequest request = LocationRequest.create();
        request.setInterval(2000);
        request.setPriority(Priority.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                for (Location loc : result.getLocations()) {
                    updateLocation(loc);
                }
            }
        };

        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, getMainLooper());
        }
    }

    private void updateLocation(Location newLoc) {
        if (lastLocation != null) {
            float d = lastLocation.distanceTo(newLoc);
            if (d > 5 && d < 100) totalDistance += d;
        }
        lastLocation = newLoc;
        sendTracking(newLoc);
    }

    private void sendTracking(Location loc) {
        if (vehicleId == -1) return;

        String url = APIHelper.BASE_URL + "tracking";
        String token = APIHelper.getToken(this);

        JSONObject body = new JSONObject();
        try {
            body.put("vehicleId", vehicleId);
            body.put("lat", loc.getLatitude());
            body.put("lng", loc.getLongitude());
        } catch (Exception e) {
        }

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                url,
                body,
                res -> {
                },
                err -> err.printStackTrace()
        ) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                if (token != null) headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };

        Volley.newRequestQueue(this).add(req);
    }

    private boolean isRiding() {
        return getSharedPreferences("ride_state", MODE_PRIVATE)
                .getBoolean("isRiding", false);
    }

    // ================= LIFCYCLE ===================
    @Override
    protected void onResume() {
        super.onResume();
        loadRideState();
        requestLocationPermission();
        boolean rideEnded = getIntent().getBooleanExtra("rideEnded", false);
        if (rideEnded) {
            clearRideState();
            showEndRideSuccessDialog();
            getIntent().removeExtra("rideEnded");
        }
    }

    private void showEndRideSuccessDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_endride_success, null);
        int transactionId = getIntent().getIntExtra("transaction_id", -1);
        int rentalId = getIntent().getIntExtra("rental_id",-1);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        view.findViewById(R.id.btnDetail).setOnClickListener(v -> {

            if (transactionId == -1) {
                Toast.makeText(this, "Không có dữ liệu giao dịch", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(MainActivity.this, TransactionDetailActivity.class);
            intent.putExtra("transaction_id", transactionId);
            startActivity(intent);

            dialog.dismiss();
        });
//        view.findViewById(R.id.btnRate).setOnClickListener(v -> {
//            Toast.makeText(this, "Cảm ơn bạn đã đánh giá chuyến đi!", Toast.LENGTH_SHORT).show();
//            dialog.dismiss();
//        });
        view.findViewById(R.id.btnRate).setOnClickListener(v -> {
            dialog.dismiss();
            View ratingView = getLayoutInflater().inflate(R.layout.dialog_rating, null);
            AlertDialog ratingDialog = new AlertDialog.Builder(this).setView(ratingView).create();
            RatingBar ratingBar = ratingView.findViewById(R.id.ratingBar);
            EditText edtComment = ratingView.findViewById(R.id.edtComment);
            ratingView.findViewById(R.id.btnSubmitRating).setOnClickListener(v1 -> {
                int rating = (int) ratingBar.getRating();
                String comment = edtComment.getText().toString();
                if (rating == 0) {
                    Toast.makeText(this, "Vui lòng chọn số sao", Toast.LENGTH_SHORT).show();
                    return;
                }
                sendReview(rentalId, rating, comment);
                ratingDialog.dismiss();
            });
            ratingDialog.show();
        });
        dialog.show();
    }

    // Gui danh gia
    private void sendReview(
            int rentalId,
            int rating,
            String comment
    ) {
        String url = APIHelper.CREATE_REVIEW;
        String token = APIHelper.getToken(this);
        JSONObject body = new JSONObject();
        try {
            body.put("rental_id", rentalId);
            body.put("rating", rating);
            body.put("comment", comment);
        } catch (Exception e) {
            e.printStackTrace();
        }
        JsonObjectRequest request =
                new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        body,
                        response -> {
                            Toast.makeText(
                                    this,
                                    "Đánh giá thành công!",
                                    Toast.LENGTH_SHORT
                            ).show();
                        },
                        error -> {
                            error.printStackTrace();
                            if (error.networkResponse != null) {
                                String msg =
                                        new String(
                                                error.networkResponse.data
                                        );

                                Toast.makeText(
                                        this,
                                        msg,
                                        Toast.LENGTH_LONG
                                ).show();

                            } else {

                                Toast.makeText(
                                        this,
                                        "Không kết nối được server",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                ) {
                    @Override
                    public java.util.Map<String, String> getHeaders() {

                        java.util.Map<String, String> headers =
                                new java.util.HashMap<>();

                        headers.put(
                                "Authorization",
                                "Bearer " + token
                        );

                        headers.put(
                                "Content-Type",
                                "application/json"
                        );

                        return headers;
                    }

                };


        Volley.newRequestQueue(this)
                .add(request);

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        boolean rideEnded = intent.getBooleanExtra("rideEnded", false);
        if (rideEnded) {
            clearRideState();
            showEndRideSuccessDialog();

            intent.removeExtra("rideEnded");
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        stopTimer();
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    // ================= PERMISSION =====================
    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    200
            );
        }
    }

    // ============= VEHICLE =======================
    @Override
    public void onVehiclesLoaded(JSONArray vehicles) {
        try {
            if (vehicles == null || vehicles.length() == 0) {
                Toast.makeText(this, "Trạm hiện tại không có xe sẵn sàng.", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONObject firstVehicle = vehicles.getJSONObject(0);
            int vehicleId = firstVehicle.getInt("id");
            Intent intent = new Intent(this, ChiTietXeActivity.class);
            intent.putExtra("vehicleId", vehicleId);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi dữ liệu xe", Toast.LENGTH_SHORT).show();
        }
    }

    private void showVehicleDialog(JSONArray vehicles, String[] list) {
        new AlertDialog.Builder(this)
                .setTitle("Chọn xe")
                .setItems(list, (d, i) -> {
                    try {
                        JSONObject o = vehicles.getJSONObject(i);
                        callRentAPI(o);
                    } catch (Exception e) {
                        Toast.makeText(this, "Lỗi dữ liệu", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void callRentAPI(JSONObject vehicle) {
        // không có GPS thì không cho thuê
        if (lastLocation == null) {
            Toast.makeText(this, "Chưa có vị trí GPS", Toast.LENGTH_SHORT).show();
            return;
        }
        String url = APIHelper.BASE_URL + "rent";
        String token = APIHelper.getToken(this);
        JSONObject body = new JSONObject();
        try {
            body.put("vehicleId", vehicle.getInt("id"));
            body.put("lat", lastLocation.getLatitude());
            body.put("lng", lastLocation.getLongitude());
        } catch (Exception e) {
            e.printStackTrace();
        }

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                url,
                body,
                res -> {
                    String msg = res.optString("message");
                    if ("SUCCESS".equals(msg)) {
                        try {
                            startRide(
                                    vehicle.getString("plate"),
                                    String.valueOf(vehicle.getInt("pin")),
                                    vehicle.getInt("id")
                            );
                            showUnlockSuccessDialog();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if ("NOT_ENOUGH_MONEY".equals(msg)) {
                        int balance = res.optInt("balance", 0);
                        int need = res.optInt("need", 0);
                        Toast.makeText(this,
                                "Số dư không đủ (" + balance + "đ / cần " + need + "đ)",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    }
                },
                err -> {
                    err.printStackTrace();
                    Toast.makeText(this, "Thuê xe thất bại", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                if (token != null) headers.put("Authorization", "Bearer " + token);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };
        Volley.newRequestQueue(this).add(req);
    }

    private void endRide() {
        if (vehicleId == -1 || lastLocation == null) return;
        isNearStation(station -> {
            if (station == null) {
                View view = getLayoutInflater().inflate(R.layout.dialog_not_in_station, null);
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setView(view)
                        .create();
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                }
                view.findViewById(R.id.btnOK).setOnClickListener(v -> dialog.dismiss());
                dialog.show();
                return;
            }

            View view = getLayoutInflater().inflate(R.layout.dialog_confirm_endride, null);
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(view)
                    .create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
            view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
            view.findViewById(R.id.btnConfirm).setOnClickListener(v -> {
                dialog.dismiss();
                Intent intent = new Intent(MainActivity.this, EndRideActivity.class);
                intent.putExtra("vehicleId", vehicleId);
                if (lastLocation != null) {
                    intent.putExtra("lat", lastLocation.getLatitude());
                    intent.putExtra("lng", lastLocation.getLongitude());
                    intent.putExtra("time", tvTime.getText().toString());
                    intent.putExtra("distance", tvDistance.getText().toString());
                    intent.putExtra("price", tvPrice.getText().toString());
                }
                try {
                    intent.putExtra("stationName", station.getString("name"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                startActivity(intent);
            });

            dialog.show();
        });
    }

    private double getDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000; // mét
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private void isNearStation(NearestStationCallback callback) {
        if (lastLocation == null) {
            callback.onResult(null);
            return;
        }
        double userLat = lastLocation.getLatitude();
        double userLng = lastLocation.getLongitude();
        String url = APIHelper.BASE_URL + "stations";
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    JSONObject nearest = null;
                    double minDistance = Double.MAX_VALUE;
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject s = response.getJSONObject(i);
                            double lat = s.getDouble("lat");
                            double lng = s.getDouble("lng");
                            double distance = getDistance(userLat, userLng, lat, lng);
                            if (distance < minDistance) {
                                minDistance = distance;
                                nearest = s;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (nearest != null && minDistance <= 5000) {
                        callback.onResult(nearest);
                    } else {
                        callback.onResult(null);
                    }
                },
                error -> {
                    error.printStackTrace();
                    callback.onResult(null);
                }
        );
        queue.add(request);
    }

    interface NearestStationCallback {
        void onResult(JSONObject station);
    }
}


