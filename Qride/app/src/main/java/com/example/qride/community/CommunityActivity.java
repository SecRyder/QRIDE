package com.example.qride.community;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.R;
import com.example.qride.adapter.CommunityAdapter;
import com.example.qride.helper.APIHelper;
import com.example.qride.model.Post;

import org.json.JSONObject;

import java.util.ArrayList;

public class CommunityActivity extends AppCompatActivity {
    RecyclerView recyclerCommunity;
    ArrayList<Post> postList = new ArrayList<>();
    CommunityAdapter adapter;

    @Override
    protected void onCreate(Bundle b){
        super.onCreate(b);
        setContentView(R.layout.activity_community);

        // Fullscreen
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        recyclerCommunity = findViewById(R.id.recyclerCommunity);
        recyclerCommunity.setLayoutManager(new LinearLayoutManager(this));

        // CHỈNH SỬA: Khởi tạo adapter kèm interface lắng nghe sự kiện Click từ Item
        adapter = new CommunityAdapter(postList, new CommunityAdapter.OnPostClickListener() {
            @Override
            public void onLikeClick(Post post, int position) {
                // Gọi API gửi lượt like lên server khi user bấm Tim
                sendLikeToServer(post.getId());
            }

            @Override
            public void onCommentClick(Post post) {
                Intent intent = new Intent(CommunityActivity.this, CommentActivity.class);
                intent.putExtra("POST_ID", post.getId());
                startActivity(intent);
            }
        });

        recyclerCommunity.setAdapter(adapter);

        loadFeed();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCreatePost).setOnClickListener(v -> {
            Intent intent = new Intent(CommunityActivity.this, CreatePostActivity.class);
            startActivity(intent);
        });
    }

    private void loadFeed(){
        String url = APIHelper.COMMUNITY_FEED;
        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    postList.clear();
                    try{
                        for(int i=0; i<response.length(); i++){
                            JSONObject obj = response.getJSONObject(i);

                            // CHỈNH SỬA: Đọc thêm trường image_url và truyền đúng constructor 9 tham số
                            Post post = new Post(
                                    obj.getInt("id"),
                                    obj.getString("name"),
                                    obj.optString("avatar"),
                                    obj.getString("title"),
                                    obj.getString("content"),
                                    obj.optString("location"),
                                    obj.optString("image_url"), // Đọc thêm image_url
                                    obj.optInt("likes"),
                                    obj.optInt("comments")
                            );
                            postList.add(post);
                        }
                        adapter.notifyDataSetChanged();
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                },
                error -> {
                    Toast.makeText(this, "Không tải được Community", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public java.util.Map<String,String> getHeaders(){
                java.util.HashMap<String,String> header = new java.util.HashMap<>();
                String token = APIHelper.getToken(CommunityActivity.this);
                header.put("Authorization", "Bearer " + token);
                return header;
            }
        };
        Volley.newRequestQueue(this).add(request);
    }

    // BỔ SUNG: Hàm gọi API Like bài viết lên Backend
    private void sendLikeToServer(int postId) {
        String url = APIHelper.LIKE_POST;

        JSONObject body = new JSONObject();
        try {
            body.put("postId", postId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                body,
                response -> {
                    // Bạn có thể check response { liked: true/false } từ server nếu cần xử lý đổi màu icon
                },
                error -> {
                    Toast.makeText(this, "Lỗi kết nối mạng", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.HashMap<String, String> header = new java.util.HashMap<>();
                String token = APIHelper.getToken(CommunityActivity.this);
                header.put("Authorization", "Bearer " + token);
                return header;
            }
        };
        Volley.newRequestQueue(this).add(request);
    }

    @Override
    protected void onResume(){
        super.onResume();
        loadFeed(); // Tự động làm mới danh sách bài viết khi quay lại từ màn hình Tạo bài đăng
    }
}