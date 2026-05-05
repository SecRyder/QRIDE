package com.example.qride.thuexe;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.MainActivity;
import com.example.qride.R;
import com.example.qride.helper.APIHelper;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.*;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import org.json.JSONObject;

public class QRScanActivity extends AppCompatActivity {

    private PreviewView previewView;
    private ImageView btnFlash, btnBack;
    private Button btnNhapMa;

    private Camera camera;
    private boolean isFlashOn = false;
    private boolean isScanned = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quetqr);
        // Fullscreen
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
        previewView = findViewById(R.id.previewView);
        btnFlash = findViewById(R.id.btnFlash);
        btnNhapMa = findViewById(R.id.btnNhapMa);
        btnBack = findViewById(R.id.btnBack);

        requestCameraPermission();

        btnFlash.setOnClickListener(v -> toggleFlash());

        btnNhapMa.setOnClickListener(v ->
                startActivity(new Intent(this, QRNhapMaActivity.class))
        );

        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        isScanned = false;
    }

    // ================= CAMERA PERMISSION =================
    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 100);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 100 && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        }
    }

    // ================= START CAMERA =================
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                analysis.setAnalyzer(
                        ContextCompat.getMainExecutor(this),
                        new QRAnalyzer()
                );

                CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;

                provider.unbindAll();
                camera = provider.bindToLifecycle(this, selector, preview, analysis);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // ================= FLASH =================
    private void toggleFlash() {
        if (camera == null) return;

        isFlashOn = !isFlashOn;
        camera.getCameraControl().enableTorch(isFlashOn);

        btnFlash.setImageResource(
                isFlashOn ? R.drawable.flashon : R.drawable.flashoff
        );
    }

    // ================= QR ANALYZER =================
    private class QRAnalyzer implements ImageAnalysis.Analyzer {
        private final BarcodeScanner scanner =
                BarcodeScanning.getClient(
                        new BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                .build()
                );

        @OptIn(markerClass = ExperimentalGetImage.class)
        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {

            if (isScanned) {
                imageProxy.close();
                return;
            }

            Image mediaImage = imageProxy.getImage();
            if (mediaImage == null) {
                imageProxy.close();
                return;
            }

            InputImage image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.getImageInfo().getRotationDegrees()
            );

            scanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        for (Barcode barcode : barcodes) {
                            String result = barcode.getRawValue();

                            if (result != null) {
                                isScanned = true;
                                handleResult(result);
                                break;
                            }
                        }
                    })
                    .addOnFailureListener(Throwable::printStackTrace)
                    .addOnCompleteListener(task -> imageProxy.close());
        }
    }

    // ================= HANDLE QR =================
    private void handleResult(String result) {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) v.vibrate(200);

        // FORMAT: vehicleId=1
        if (result.startsWith("vehicleId=")) {
            String vehicleId = result.replace("vehicleId=", "");
            getVehicleDetail(vehicleId);
        } else {
            Toast.makeText(this, "QR không hợp lệ", Toast.LENGTH_SHORT).show();
            isScanned = false;
        }
    }

    // ================= CALL API =================
    private void getVehicleDetail(String vehicleId) {
        String url = APIHelper.BASE_URL + "vehicle/" + vehicleId;

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        Intent intent = new Intent(this, ChiTietXeActivity.class);

                        intent.putExtra("vehicleId", response.getInt("id"));
                        intent.putExtra("plate", response.getString("plate"));
                        intent.putExtra("pin", response.getString("pin"));
                        intent.putExtra("stationName", response.getString("station_name"));
                        intent.putExtra("stationAddress", response.getString("station_address"));
                        String status = response.optString("current_status", "unknown");

                        if (!status.equals("available")) {
                            Toast.makeText(this, "Xe không khả dụng", Toast.LENGTH_SHORT).show();
                            isScanned = false;
                            return;
                        }

                        intent.putExtra("status", status);
                        startActivity(intent);

                    } catch (Exception e) {
                        e.printStackTrace();
                        isScanned = false;
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(this, "Lỗi server", Toast.LENGTH_SHORT).show();
                    isScanned = false;
                }
        );

        queue.add(request);
    }
}