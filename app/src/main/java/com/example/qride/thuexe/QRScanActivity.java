package com.example.qride.thuexe;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.camera.core.Camera;

import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.qride.MainActivity;
import com.example.qride.R;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import org.checkerframework.checker.nullness.qual.NonNull;

public class QRScanActivity extends AppCompatActivity {

    private PreviewView previewView;
    private ImageView btnFlash;

    private Camera camera;
    private boolean isFlashOn = false;
    private Button btnNhapMa;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quetqr);

        // Bo sung de khong bi chiem phan tren cung cua app (status bar)
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        previewView = findViewById(R.id.previewView);
        btnFlash = findViewById(R.id.btnFlash);

        requestCameraPermission();

        btnFlash.setOnClickListener(v -> toggleFlash());

        btnNhapMa = findViewById(R.id.btnNhapMa);
        btnNhapMa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(QRScanActivity.this, QRNhapMaActivity.class);
                startActivity(intent);
            }
        });

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(QRScanActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    // ================= PERMISSION =================
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        }
    }

    // ================= CAMERA =================
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis =
                        new ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();

                imageAnalysis.setAnalyzer(
                        ContextCompat.getMainExecutor(this),
                        new QRAnalyzer()
                );

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();

                camera = (Camera) cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageAnalysis
                );

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

    // ================= ANALYZER =================
    private class QRAnalyzer implements ImageAnalysis.Analyzer {

        private final BarcodeScanner scanner =
                BarcodeScanning.getClient(
                        new BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                .build()
                );

        private boolean isScanned = false;

        @OptIn(markerClass = ExperimentalGetImage.class)
        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {
            if (isScanned) {
                imageProxy.close();
                return;
            }

            @androidx.camera.core.ExperimentalGetImage
            Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {

                InputImage image =
                        InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.getImageInfo().getRotationDegrees()
                        );

                scanner.process(image)
                        .addOnSuccessListener(barcodes -> {
                            for (Barcode barcode : barcodes) {
                                String result = barcode.getRawValue();
                                if (result != null) {

                                    isScanned = true;

                                    runOnUiThread(() -> {
                                        handleResult(result);
                                    });

                                    break;
                                }
                            }
                        })
                        .addOnFailureListener(Throwable::printStackTrace)
                        .addOnCompleteListener(task -> imageProxy.close());
            } else {
                imageProxy.close();
            }
        }
    }

    // ================= HANDLE RESULT =================
    private void handleResult(String result) {

        // rung nhẹ
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) v.vibrate(200);

        // ví dụ xử lý
        if (result.startsWith("http")) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(result));
            startActivity(intent);
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Kết quả QR")
                    .setMessage(result)
                    .setPositiveButton("OK", (d, w) -> finish())
                    .show();
        }
    }
}