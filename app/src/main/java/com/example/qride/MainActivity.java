package com.example.qride;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.qride.fragment.ProfileFragment;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final float DEFAULT_ZOOM = 15f;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng currentLocation;

    private EditText etSearch;
    private ImageButton btnNotification;
    private CardView cardStationInfo;
    private TextView tvStationName, tvStationAddress;
    private Button btnRentBike;
    private FloatingActionButton btnLocateMe;
    
    // Đổi navQuetQR sang kiểu View hoặc FrameLayout để tránh ClassCastException
    private View navTramXe, navUuDai, navQuetQR, navThanhToan, navTaiKhoan;

    private List<BikeStation> stationList = new ArrayList<>();

    static class BikeStation {
        String name;
        String address;
        int bikeCount;
        LatLng location;

        BikeStation(String name, String address, int bikeCount, LatLng location) {
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

        initViews();
        initSampleStations();
        initMap();
        setupBottomNav();
        setupSearch();
    }

    private void initViews() {
        etSearch        = findViewById(R.id.etSearch);
        btnNotification = findViewById(R.id.btnNotification);
        cardStationInfo = findViewById(R.id.cardStationInfo);
        tvStationName   = findViewById(R.id.tvStationName);
        tvStationAddress= findViewById(R.id.tvStationAddress);
        btnRentBike     = findViewById(R.id.btnRentBike);
        btnLocateMe     = findViewById(R.id.btnLocateMe);

        // Ánh xạ các mục Bottom Nav
        navTramXe   = findViewById(R.id.navTramXe);
        navUuDai    = findViewById(R.id.navUuDai);
        navQuetQR   = findViewById(R.id.navQuetQR); 
        navThanhToan= findViewById(R.id.navThanhToan);
        navTaiKhoan = findViewById(R.id.navTaiKhoan);

        btnRentBike.setOnClickListener(v -> 
                Toast.makeText(this, "Chức năng thuê xe đang được phát triển", Toast.LENGTH_SHORT).show());
        
        btnLocateMe.setOnClickListener(v -> enableMyLocation());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapContainer);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.mapContainer, mapFragment)
                    .commit();
        }
        mapFragment.getMapAsync(this);
    }

    private void initSampleStations() {
        stationList.add(new BikeStation("Trạm Bến Thành", "Quận 1, TP.HCM", 8, new LatLng(10.7726, 106.6980)));
        stationList.add(new BikeStation("Trạm Lê Thánh Tôn", "26 Lê Thánh Tôn, Q.1", 5, new LatLng(10.7742, 106.6963)));
        stationList.add(new BikeStation("Trạm Hàm Nghi", "Hàm Nghi, Q.1", 10, new LatLng(10.7710, 106.7030)));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        addStationMarkers();

        mMap.setOnMarkerClickListener(marker -> {
            BikeStation station = (BikeStation) marker.getTag();
            if (station != null) {
                showStationCard(station);
                mMap.animateCamera(CameraUpdateFactory.newLatLng(station.location));
            }
            return true;
        });

        mMap.setOnMapClickListener(latLng -> hideStationCard());
        requestLocationPermission();
    }

    private void addStationMarkers() {
        for (BikeStation station : stationList) {
            BitmapDescriptor icon = createStationMarker(station.bikeCount);
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(station.location)
                    .icon(icon));
            if (marker != null) marker.setTag(station);
        }
        LatLng center = new LatLng(10.7710, 106.7000);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, DEFAULT_ZOOM));
    }

    private BitmapDescriptor createStationMarker(int bikeCount) {
        int size = 100;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.parseColor("#1DB954"));
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 5, bgPaint);
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(6f);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 5, borderPaint);
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        canvas.drawText(String.valueOf(bikeCount), size / 2f, size / 2f + 12, textPaint);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void showStationCard(BikeStation station) {
        tvStationName.setText(station.name);
        tvStationAddress.setText(station.address);
        if (cardStationInfo.getVisibility() != View.VISIBLE) {
            cardStationInfo.setVisibility(View.VISIBLE);
            cardStationInfo.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
        }
    }

    private void hideStationCard() {
        if (cardStationInfo.getVisibility() == View.VISIBLE) {
            cardStationInfo.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
            cardStationInfo.setVisibility(View.GONE);
        }
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        }
    }

    private void enableMyLocation() {
        if (mMap == null) return;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) return;
        mMap.setMyLocationEnabled(true);
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, DEFAULT_ZOOM));
            }
        });
    }

    private void setupBottomNav() {
        navTramXe.setOnClickListener(v -> Toast.makeText(this, "Trạm xe", Toast.LENGTH_SHORT).show());
        navQuetQR.setOnClickListener(v -> Toast.makeText(this, "Quét QR", Toast.LENGTH_SHORT).show());
        navUuDai.setOnClickListener(v -> Toast.makeText(this, "Ưu đãi", Toast.LENGTH_SHORT).show());
        navThanhToan.setOnClickListener(v -> Toast.makeText(this, "Thanh toán", Toast.LENGTH_SHORT).show());
        navTaiKhoan.setOnClickListener(v -> Toast.makeText(this, "Tài khoản", Toast.LENGTH_SHORT).show());
        navTaiKhoan.setOnClickListener(v -> {
            // 1. Gọi Fragment Tài khoản để thay thế phần "ruột" ở giữa
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ProfileFragment())
                    .commit();

            // 2. Làm sáng logo và chữ của nút Tài khoản
            updateBottomNavUI(navTaiKhoan);
        });
    }

    private void setupSearch() {
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            String query = etSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                for (BikeStation s : stationList) {
                    if (s.name.toLowerCase().contains(query.toLowerCase())) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(s.location, 17f));
                        showStationCard(s);
                        break;
                    }
                }
            }
            return true;
        });
    }

    private void updateBottomNavUI(View selectedNav) {
        // Mã màu xám (khi không được chọn) và màu Xanh (khi được chọn)
        int colorDefault = Color.parseColor("#888888"); // Xám
        int colorSelected = Color.parseColor("#00B087"); // Xanh lá của app bạn

        // 1. Reset tất cả các nút về màu xám mặc định
        changeNavColor(navTramXe, colorDefault);
        changeNavColor(navUuDai, colorDefault);
        changeNavColor(navThanhToan, colorDefault);
        changeNavColor(navTaiKhoan, colorDefault);

        // 2. Đổi riêng nút đang được bấm thành màu xanh sáng
        changeNavColor(selectedNav, colorSelected);
    }

    // Hàm này sẽ tự động mò vào trong View của bạn, tìm ImageView (icon) và TextView (chữ) để đổi màu
    private void changeNavColor(View navView, int color) {
        if (navView instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) navView;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof ImageView) {
                    ((ImageView) child).setColorFilter(color); // Đổi màu Icon
                } else if (child instanceof TextView) {
                    ((TextView) child).setTextColor(color);    // Đổi màu Chữ
                }
            }
        }
    }
}
