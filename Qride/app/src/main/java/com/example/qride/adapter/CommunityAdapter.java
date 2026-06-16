package com.example.qride.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qride.R;
import com.example.qride.model.Post;

import java.util.List;

public class CommunityAdapter extends RecyclerView.Adapter<CommunityAdapter.Holder> {

    private List<Post> list;
    private OnPostClickListener listener;

    // Định nghĩa Interface để truyền sự kiện Click ra ngoài Activity xử lý API
    public interface OnPostClickListener {
        void onLikeClick(Post post, int position);
        void onCommentClick(Post post);
    }

    // Constructor nhận thêm listener
    public CommunityAdapter(List<Post> list, OnPostClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Post p = list.get(position);

        h.name.setText(p.getName());
        h.title.setText(p.getTitle());
        h.content.setText(p.getContent());
        h.location.setText(p.getLocation());

        // Xử lý hiển thị ảnh chuyến đi linh hoạt (Ẩn nếu bài đăng không có ảnh)
        if (p.getImageUrl() == null || p.getImageUrl().isEmpty()) {
            h.tripImage.setVisibility(View.GONE);
        } else {
            h.tripImage.setVisibility(View.VISIBLE);

            // --- LOAD ẢNH  ---
            String serverRoot = com.example.qride.helper.APIHelper.BASE_URL.replace("/api/", "");
            String fullImageUrl = serverRoot + p.getImageUrl();

            // Gọi Glide để tải ảnh từ Backend
            com.bumptech.glide.Glide.with(h.itemView.getContext())
                    .load(fullImageUrl)
                    .placeholder(R.drawable.sample_trip) // Hiện ảnh sample trong lúc tải
                    .error(R.drawable.sample_trip)       // Hiện ảnh sample nếu đường link bị lỗi
                    .into(h.tripImage);
        }

        // Ẩn vùng số sao (Do chọn Hướng 1 - Tách biệt đánh giá)
        h.rating.setVisibility(View.GONE);

        // Đổ số lượng tương tác lên giao diện mới
        h.tvLikeCount.setText(String.valueOf(p.getLikes()));
        h.tvCommentCount.setText(String.valueOf(p.getComments()));

        // Xử lý click Nút Like
        h.btnLike.setOnClickListener(v -> {
            // Tăng số lượng trong dữ liệu cục bộ trước để UI cập nhật ngay lập tức
            p.setLikes(p.getLikes() + 1);
            h.tvLikeCount.setText(String.valueOf(p.getLikes()));

            // Bắn sự kiện ra ngoài Activity để gọi API cập nhật Database
            if (listener != null) {
                listener.onLikeClick(p, position);
            }
        });

        // Xử lý click Nút Comment
        h.btnComment.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCommentClick(p);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    public static class Holder extends RecyclerView.ViewHolder {
        ImageView avatar;
        ImageView tripImage;
        TextView name;
        TextView title;
        TextView content;
        TextView location;
        TextView rating;

        View btnLike;
        TextView tvLikeCount;
        View btnComment;
        TextView tvCommentCount;

        public Holder(@NonNull View v) {
            super(v);
            avatar = v.findViewById(R.id.imgAvatar);
            tripImage = v.findViewById(R.id.imgTrip);
            rating = v.findViewById(R.id.tvRating);
            name = v.findViewById(R.id.tvUserName);
            title = v.findViewById(R.id.tvTitle);
            content = v.findViewById(R.id.tvContent);
            location = v.findViewById(R.id.tvLocation);

            // Tìm đúng ID mới của LinearLayout và TextView tương ứng
            btnLike = v.findViewById(R.id.btnLike);
            tvLikeCount = v.findViewById(R.id.tvLikeCount);
            btnComment = v.findViewById(R.id.btnComment);
            tvCommentCount = v.findViewById(R.id.tvCommentCount);
        }
    }
}