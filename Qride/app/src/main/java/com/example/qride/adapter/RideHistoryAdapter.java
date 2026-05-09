package com.example.qride.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qride.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RideHistoryAdapter extends RecyclerView.Adapter<RideHistoryAdapter.ViewHolder> {

    private JSONArray rides;

    public RideHistoryAdapter(JSONArray rides) {
        this.rides = rides;
    }

    public void updateData(JSONArray newRides) {
        this.rides = newRides;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ride_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            JSONObject ride = rides.getJSONObject(position);

            // Plate
            holder.tvPlate.setText(ride.optString("plate", "N/A"));

            // Station
            holder.tvStation.setText(ride.optString("station_name", ""));

            // Status
            String status = ride.optString("status", "done");
            if ("done".equals(status)) {
                holder.tvStatus.setText("Hoàn thành");
                holder.tvStatus.setBackgroundResource(R.drawable.btn_solid_green);
            } else if ("renting".equals(status)) {
                holder.tvStatus.setText("Đang thuê");
                holder.tvStatus.setBackgroundResource(R.drawable.btn_outline_green);
            } else {
                holder.tvStatus.setText("Đã hủy");
                holder.tvStatus.setBackgroundResource(R.drawable.btn_outline_green);
            }

            // Time
            String startTime = ride.optString("start_time", "");
            if (!startTime.isEmpty() && !startTime.equals("null")) {
                try {
                    SimpleDateFormat inputFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                    Date date = inputFmt.parse(startTime);
                    SimpleDateFormat outputFmt = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    holder.tvTime.setText(outputFmt.format(date));
                } catch (Exception e) {
                    holder.tvTime.setText(startTime.substring(0, Math.min(16, startTime.length())));
                }
            } else {
                holder.tvTime.setText("—");
            }

            // Price
            int price = ride.optInt("total_price", 0);
            if (price > 0) {
                NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
                holder.tvPrice.setText(nf.format(price) + "đ");
            } else {
                holder.tvPrice.setText("—");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return rides != null ? rides.length() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPlate, tvStation, tvStatus, tvTime, tvPrice;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlate = itemView.findViewById(R.id.tvPlate);
            tvStation = itemView.findViewById(R.id.tvStation);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvPrice = itemView.findViewById(R.id.tvPrice);
        }
    }
}
