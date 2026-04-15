package com.example.qride.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qride.R;
import com.example.qride.model.NotificationModel;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {

    private final Context context;
    private final List<NotificationModel> list;

    public NotificationAdapter(Context context, List<NotificationModel> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        NotificationModel item = list.get(position);
        
        // Dịch Tiêu đề và Nội dung dựa trên KEY
        holder.tvTitle.setText(getStringResource(item.getTitle()));
        holder.tvMessage.setText(getStringResource(item.getMessage()));
        holder.tvTime.setText(item.getTimestamp());

        // Icon based on type
        switch (item.getType()) {
            case "TRIP": holder.ivIcon.setImageResource(R.drawable.ic_bike_station); break;
            case "VOUCHER": holder.ivIcon.setImageResource(R.drawable.ic_gift); break;
            case "CHECKIN": holder.ivIcon.setImageResource(R.drawable.ic_calendar); break;
            default: holder.ivIcon.setImageResource(R.drawable.ic_bell); break;
        }

        // Highlight unread
        holder.itemView.setBackgroundColor(item.isRead() ? Color.TRANSPARENT : Color.parseColor("#F0FFF0"));
    }

    private String getStringResource(String key) {
        if (key == null || key.isEmpty()) return "";
        // Nếu là mã nội dung (ví dụ: notif_msg_trip_start), lấy bản dịch
        int resId = context.getResources().getIdentifier(key, "string", context.getPackageName());
        if (resId != 0) {
            return context.getString(resId);
        }
        return key; // Trả về text thô nếu không phải key (đề phòng dữ liệu cũ)
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle, tvMessage, tvTime;
        VH(View v) {
            super(v);
            ivIcon = v.findViewById(R.id.ivNotifIcon);
            tvTitle = v.findViewById(R.id.tvNotifTitle);
            tvMessage = v.findViewById(R.id.tvNotifMsg);
            tvTime = v.findViewById(R.id.tvNotifTime);
        }
    }
}
