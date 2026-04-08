package com.example.qride.login.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.qride.R;

public class QuenPassActivity extends AppCompatActivity {
    private ImageView imgBackLoginActivity, imgFlag;
    private TextView tvTieuDe, tvSoDienThoai, tvCountry;
    private EditText edtPhone;
    private Button btnGuiOTP ;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_quenpass);
        imgBackLoginActivity = findViewById(R.id.imgBackLoginActivity);
        imgFlag = findViewById(R.id.imgFlag);
        tvTieuDe = findViewById(R.id.tvTieuDe);
        tvSoDienThoai = findViewById(R.id.tvSoDienThoai);
        tvCountry = findViewById(R.id.tvCountry);
        edtPhone = findViewById(R.id.edtPhone);
        btnGuiOTP = findViewById(R.id.btnGuiOTP);

    }
}
