package com.example.qride.community;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.R;
import com.example.qride.adapter.CommentAdapter;
import com.example.qride.helper.APIHelper;
import com.example.qride.model.Comment;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CommentActivity extends AppCompatActivity {

    private RecyclerView recyclerComments;
    private EditText edtCommentInput;
    private Button btnSendComment;

    private ArrayList<Comment> commentList = new ArrayList<>();
    private CommentAdapter adapter;
    private int postId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        // Fullscreen
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        // Nhận ID bài viết được truyền sang
        postId = getIntent().getIntExtra("POST_ID", -1);
        if (postId == -1) {
            Toast.makeText(this, "Không tìm thấy bài viết", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Ánh xạ View
        recyclerComments = findViewById(R.id.recyclerComments);
        edtCommentInput = findViewById(R.id.edtCommentInput);
        btnSendComment = findViewById(R.id.btnSendComment);

        findViewById(R.id.btnBackComment).setOnClickListener(v -> finish());

        // Cấu hình RecyclerView
        recyclerComments.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CommentAdapter(commentList);
        recyclerComments.setAdapter(adapter);

        // Gọi API tải danh sách comment ban đầu
        loadComments();

        // Xử lý gửi bình luận mới
        btnSendComment.setOnClickListener(v -> {
            String text = edtCommentInput.getText().toString().trim();
            if (!text.isEmpty()) {
                postComment(text);
            } else {
                Toast.makeText(CommentActivity.this, "Vui lòng nhập nội dung!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 1. API Lấy danh sách bình luận (GET /api/community/comments/:postId)
    private void loadComments() {
        String url = APIHelper.BASE_URL + "community/comments/" + postId;

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    commentList.clear();
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);
                            Comment comment = new Comment();
                            comment.setContent(obj.getString("content"));
                            comment.setName(obj.getString("name"));
                            comment.setAvatar(obj.optString("avatar"));
                            commentList.add(comment);
                        }
                        adapter.notifyDataSetChanged();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(CommentActivity.this, "Lỗi tải bình luận", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> header = new HashMap<>();
                header.put("Authorization", "Bearer " + APIHelper.getToken(CommentActivity.this));
                return header;
            }
        };
        Volley.newRequestQueue(this).add(request);
    }

    // 2. API Đăng bình luận mới (POST /api/community/comment)
    private void postComment(String text) {
        String url = APIHelper.BASE_URL + "community/comment";

        JSONObject body = new JSONObject();
        try {
            body.put("postId", postId);
            body.put("content", text);
        } catch (Exception e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                body,
                response -> {
                    Toast.makeText(CommentActivity.this, "Đã bình luận!", Toast.LENGTH_SHORT).show();
                    edtCommentInput.setText(""); // Xóa trống ô nhập
                    loadComments(); // Tự động load lại danh sách để hiển thị comment mới lên luôn
                },
                error -> Toast.makeText(CommentActivity.this, "Gửi bình luận thất bại", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> header = new HashMap<>();
                header.put("Authorization", "Bearer " + APIHelper.getToken(CommentActivity.this));
                return header;
            }
        };
        Volley.newRequestQueue(this).add(request);
    }
}