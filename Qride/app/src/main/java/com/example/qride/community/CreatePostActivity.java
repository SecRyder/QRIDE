package com.example.qride.community;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.R;
import com.example.qride.helper.APIHelper;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CreatePostActivity extends AppCompatActivity {

    EditText edtTitle;
    EditText edtContent;
    EditText edtLocation;
    Button btnPost;

    // BỔ SUNG: Khai báo cho thành phần giao diện chọn ảnh
    ImageView imgPreview;
    Button btnChooseImage;
    Bitmap bitmapImage = null; // Lưu trữ dữ liệu ảnh dưới dạng Bitmap

    // Bộ công cụ mở thư viện ảnh và nhận kết quả trả về
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        // Chuyển đổi URI ảnh thành đối tượng Bitmap để hiển thị và xử lý
                        bitmapImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                        imgPreview.setImageBitmap(bitmapImage);
                        imgPreview.setVisibility(View.VISIBLE);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Không thể mở ảnh này", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_create_post);

        // Fullscreen
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        edtTitle = findViewById(R.id.edtTitle);
        edtContent = findViewById(R.id.edtContent);
        edtLocation = findViewById(R.id.edtLocation);
        btnPost = findViewById(R.id.btnPost);

        // BỔ SUNG: Ánh xạ ID giao diện chọn ảnh
        imgPreview = findViewById(R.id.imgPreview);
        btnChooseImage = findViewById(R.id.btnChooseImage);

        // BỔ SUNG: Sự kiện khi click nút "Chọn ảnh chuyến đi"
        btnChooseImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        btnPost.setOnClickListener(v -> createPost());
    }

    private void createPost() {
        String title = edtTitle.getText().toString().trim();
        String content = edtContent.getText().toString().trim();
        String location = edtLocation.getText().toString().trim();

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "Nhập nội dung bài viết", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject body = new JSONObject();
        try {
            body.put("title", title);
            body.put("content", content);
            body.put("location", location);
            body.put("rental_id", JSONObject.NULL);

            // BỔ SUNG: Chuyển đổi ảnh Bitmap thành chuỗi Base64 nếu người dùng có chọn ảnh
            if (bitmapImage != null) {
                String base64Image = convertBitmapToBase64(bitmapImage);
                body.put("image_url", base64Image); // Đính kèm dữ liệu ảnh vào trường image_url
            } else {
                body.put("image_url", JSONObject.NULL);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                APIHelper.CREATE_POST,
                body,
                response -> {
                    Toast.makeText(this, "Đăng bài thành công", Toast.LENGTH_SHORT).show();
                    finish();
                },
                error -> {
                    Toast.makeText(this, "Lỗi đăng bài", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.HashMap<String, String> h = new java.util.HashMap<>();
                String token = APIHelper.getToken(CreatePostActivity.this);
                h.put("Authorization", "Bearer " + token);
                h.put("Content-Type", "application/json");
                return h;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

    // Hàm bổ trợ chuyển đổi Bitmap sang String Base64 để truyền qua JSON gọn nhẹ
    private String convertBitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // Nén ảnh xuống chất lượng 70% để giảm tải băng thông mạng
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
        byte[] byteArray = outputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }
}