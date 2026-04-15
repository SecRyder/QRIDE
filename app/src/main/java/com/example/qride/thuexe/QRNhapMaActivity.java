package com.example.qride.thuexe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.qride.R;

public class QRNhapMaActivity extends AppCompatActivity {
    private EditText edtNhapMaQR;
    private Button btnXemChiTiet;
    private ImageView btnBack;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nhapmaqr);

        // Bo sung de khong bi chiem phan tren cung cua app (status bar)
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        edtNhapMaQR = findViewById(R.id.edtNhapMaQR);
        btnXemChiTiet = findViewById(R.id.btnNhapMa);
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(QRNhapMaActivity.this, QRScanActivity.class);
                startActivity(intent);
            }
        });

        btnXemChiTiet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(QRNhapMaActivity.this, ChiTietXeActivity.class);
                startActivity(intent);
            }
        });
    }
}
