package com.example.qride.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qride.R;
import com.example.qride.helper.APIHelper;
import com.example.qride.model.Comment;
import com.bumptech.glide.Glide;

import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentHolder> {

    private List<Comment> commentList;

    public CommentAdapter(List<Comment> commentList) {
        this.commentList = commentList;
    }

    @NonNull
    @Override
    public CommentHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentHolder holder, int position) {
        Comment comment = commentList.get(position);

        holder.tvCommentUserName.setText(comment.getName());
        holder.tvCommentContent.setText(comment.getContent());

        // --- XỬ LÝ LOAD ẢNH AVATAR TỪ SERVER TỰ ĐỘNG THEO APIHELPER ---
        String avatarPath = comment.getAvatar(); // Ví dụ: "/uploads/avatar-abc.jpg"

        if (avatarPath != null && !avatarPath.isEmpty()) {
            // Tự động cắt bỏ "/api/" để lấy "http://192.168.100.13:3000"
            String serverRoot = APIHelper.BASE_URL.replace("/api/", "");
            String fullAvatarUrl = serverRoot + avatarPath;

            Glide.with(holder.itemView.getContext())
                    .load(fullAvatarUrl)
                    .placeholder(R.drawable.ic_person) // Ảnh mặc định nếu đang tải
                    .error(R.drawable.ic_person)       // Ảnh mặc định nếu lỗi link
                    .circleCrop()                      // Bo tròn ảnh avatar cho đẹp
                    .into(holder.imgCommentAvatar);
        } else {
            holder.imgCommentAvatar.setImageResource(R.drawable.ic_person);
        }
    }

    @Override
    public int getItemCount() {
        return commentList == null ? 0 : commentList.size();
    }

    public static class CommentHolder extends RecyclerView.ViewHolder {
        ImageView imgCommentAvatar;
        TextView tvCommentUserName, tvCommentContent;

        public CommentHolder(@NonNull View itemView) {
            super(itemView);
            imgCommentAvatar = itemView.findViewById(R.id.imgCommentAvatar);
            tvCommentUserName = itemView.findViewById(R.id.tvCommentUserName);
            tvCommentContent = itemView.findViewById(R.id.tvCommentContent);
        }
    }
}