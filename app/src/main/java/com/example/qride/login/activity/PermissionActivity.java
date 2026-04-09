package com.example.qride.login.activity;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.qride.R;

/**
 * PermissionActivity
 *
 * Hiển thị 3 màn hình xin quyền theo thứ tự:
 *   1. Thông báo (Notification)  -> activity_permission_notification.xml
 *   2. Bluetooth                 -> activity_permission_bluetooth.xml
 *   3. Vị trí (Location)         -> activity_permission_location.xml
 *
 * Sau khi hoàn thành (hoặc từ chối) tất cả, chuyển sang LoginActivity.
 * Tự động bỏ qua các bước nếu quyền đã được cấp trước đó.
 */
public class PermissionActivity extends AppCompatActivity {

    // ── Request codes ──────────────────────────────────────────────────────────
    private static final int RC_NOTIFICATION = 101;
    private static final int RC_BLUETOOTH    = 102;
    private static final int RC_LOCATION     = 103;

    // ── Bước hiện tại (0 = Notification, 1 = Bluetooth, 2 = Location) ──────────
    private int currentStep = 0;

    // ──────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showStep(currentStep);
    }

    // ── Hiển thị layout tương ứng với bước hiện tại ────────────────────────────
    private void showStep(int step) {
        if (step > 2) {
            proceedToLogin();
            return;
        }

        // Nếu quyền của bước này đã có rồi, tự động sang bước tiếp theo
        if (isPermissionGrantedForStep(step)) {
            currentStep++;
            showStep(currentStep);
            return;
        }

        switch (step) {
            case 0: showNotificationStep(); break;
            case 1: showBluetoothStep();    break;
            case 2: showLocationStep();     break;
            default: proceedToLogin();      break;
        }
    }

    private boolean isPermissionGrantedForStep(int step) {
        switch (step) {
            case 0:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                            == PackageManager.PERMISSION_GRANTED;
                }
                return true; // Android < 13 không cần xin quyền POST_NOTIFICATIONS
            case 1:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                           ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
                } else {
                    return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                            == PackageManager.PERMISSION_GRANTED;
                }
            case 2:
                return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
            default:
                return true;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BƯỚC 1 – THÔNG BÁO
    // ══════════════════════════════════════════════════════════════════════════
    private void showNotificationStep() {
        setContentView(R.layout.activity_permission_notification);

        Button btnAllow = findViewById(R.id.btnAllowNotification);
        Button btnDeny  = findViewById(R.id.btnDenyNotification);

        btnAllow.setOnClickListener(v -> requestNotificationPermission());
        btnDeny.setOnClickListener(v  -> nextStep());   // bỏ qua, sang bước tiếp
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, RC_NOTIFICATION);
                return;
            }
        }
        nextStep();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BƯỚC 2 – BLUETOOTH
    // ══════════════════════════════════════════════════════════════════════════
    private void showBluetoothStep() {
        setContentView(R.layout.activity_permission_bluetooth);

        Button btnAllow = findViewById(R.id.btnAllowBluetooth);
        Button btnDeny  = findViewById(R.id.btnDenyBluetooth);

        btnAllow.setOnClickListener(v -> requestBluetoothPermission());
        btnDeny.setOnClickListener(v  -> nextStep());
    }

    private void requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            String[] perms = {
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
            };
            boolean allGranted = true;
            for (String p : perms) {
                if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                ActivityCompat.requestPermissions(this, perms, RC_BLUETOOTH);
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH}, RC_BLUETOOTH);
                return;
            }
        }
        nextStep();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BƯỚC 3 – VỊ TRÍ
    // ══════════════════════════════════════════════════════════════════════════
    private void showLocationStep() {
        setContentView(R.layout.activity_permission_location);

        Button btnOnce  = findViewById(R.id.btnLocationOnce);
        Button btnAlways= findViewById(R.id.btnLocationAlways);
        Button btnDeny  = findViewById(R.id.btnDenyLocation);

        btnOnce.setOnClickListener(v -> requestLocationPermission(false));
        btnAlways.setOnClickListener(v -> requestLocationPermission(true));
        btnDeny.setOnClickListener(v -> nextStep());
    }

    private void requestLocationPermission(boolean withCoarse) {
        String[] perms = withCoarse
                ? new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION}
                : new String[]{Manifest.permission.ACCESS_FINE_LOCATION};

        boolean allGranted = true;
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        if (!allGranted) {
            ActivityCompat.requestPermissions(this, perms, RC_LOCATION);
        } else {
            nextStep();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // XỬ LÝ KẾT QUẢ XIN QUYỀN
    // ══════════════════════════════════════════════════════════════════════════
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        nextStep();
    }

    // ── Tăng bước và hiển thị màn tiếp theo ───────────────────────────────────
    private void nextStep() {
        currentStep++;
        showStep(currentStep);
    }

    // ── Sau khi xong tất cả quyền → sang LoginActivity ─────────────────────────
    private void proceedToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
