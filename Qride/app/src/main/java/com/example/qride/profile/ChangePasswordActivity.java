package com.example.qride.profile;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.R;
import com.example.qride.helper.APIHelper;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private Button btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        ImageView btnBack = findViewById(R.id.btnBack);
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSubmit = findViewById(R.id.btnSubmit);

        btnBack.setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> attemptChangePassword());

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkInputs();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etCurrentPassword.addTextChangedListener(watcher);
        etNewPassword.addTextChangedListener(watcher);
        etConfirmPassword.addTextChangedListener(watcher);
        
        checkInputs();
    }

    private void checkInputs() {
        boolean valid = etCurrentPassword.getText().length() >= 6 &&
                        etNewPassword.getText().length() >= 6 &&
                        etConfirmPassword.getText().length() >= 6;
        btnSubmit.setEnabled(valid);
        btnSubmit.setAlpha(valid ? 1.0f : 0.5f);
    }

    private void attemptChangePassword() {
        String current = etCurrentPassword.getText().toString();
        String newPass = etNewPassword.getText().toString();
        String confirm = etConfirmPassword.getText().toString();

        if (!newPass.equals(confirm)) {
            etConfirmPassword.setError(getString(R.string.change_password_mismatch));
            return;
        }

        String token = APIHelper.getToken(this);
        if (token == null || token.isEmpty()) return;

        btnSubmit.setEnabled(false);

        JSONObject body = new JSONObject();
        try {
            body.put("currentPassword", current);
            body.put("newPassword", newPass);
        } catch (Exception e) {}

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                APIHelper.CHANGE_PASSWORD,
                body,
                response -> {
                    Toast.makeText(this, getString(R.string.change_password_success), Toast.LENGTH_SHORT).show();
                    finish();
                },
                error -> {
                    btnSubmit.setEnabled(true);
                    if (error.networkResponse != null) {
                        if (error.networkResponse.statusCode == 401) {
                            etCurrentPassword.setError(getString(R.string.change_password_wrong));
                        } else {
                            Toast.makeText(this, getString(R.string.error_server), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, getString(R.string.error_server), Toast.LENGTH_SHORT).show();
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }
}