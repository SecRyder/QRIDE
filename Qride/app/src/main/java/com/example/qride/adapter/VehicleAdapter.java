package com.example.qride.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qride.R;

import org.json.JSONObject;

import java.util.List;

public class VehicleAdapter extends RecyclerView.Adapter<VehicleAdapter.ViewHolder> {
    private List<JSONObject> list;
    private OnItemClickListener listener;
    public interface OnItemClickListener {
        void onItemClick(JSONObject vehicle);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
    public VehicleAdapter(List<JSONObject> list) { this.list = list; }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vehicle, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            JSONObject xe = list.get(position);
            holder.tvPlate.setText(xe.getString("plate"));
            holder.tvPin.setText("Pin: " + xe.getInt("pin") + "%");

            // Kiểm tra loại xe để đổi hình ảnh
            String type = xe.optString("type", "bike");
            if (type.equals("motor")) {
                holder.imgType.setImageResource(R.drawable.xemay);
            } else {
                holder.imgType.setImageResource(R.drawable.xedap);
            }


            // Click vào từng xe để xem chi tiết
            holder.itemView.setOnClickListener(v -> {
                // Code mở ChiTietXeActivity
                JSONObject xeHienTai = list.get(holder.getAdapterPosition());
                listener.onItemClick(xeHienTai);
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPlate, tvPin;
        ImageView imgType;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlate = itemView.findViewById(R.id.tvPlate);
            tvPin = itemView.findViewById(R.id.tvPin);
            imgType = itemView.findViewById(R.id.imgType);
        }
    }


}