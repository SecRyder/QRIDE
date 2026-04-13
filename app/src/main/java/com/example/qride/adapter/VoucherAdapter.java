package com.example.qride.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qride.R;
import com.example.qride.model.VoucherModel;

import java.util.List;

public class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.VH> {

    public interface OnActionClick {
        void onAction(VoucherModel item, int position);
    }

    private final Context context;
    private final List<VoucherModel> items;
    private final OnActionClick listener;

    public VoucherAdapter(Context context, List<VoucherModel> items, OnActionClick listener) {
        this.context  = context;
        this.items    = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_voucher, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        VoucherModel item = items.get(position);

        h.ivIcon.setImageResource(item.getIconResId());
        h.tvTitle.setText(item.getTitle());
        h.tvDiscount.setText(item.getDiscountText());
        h.tvExpiry.setText(item.getExpiry());
        h.btnAction.setText(item.getActionLabel());

        // Button color
        if (item.getButtonType() == VoucherModel.ButtonType.ORANGE) {
            h.btnAction.setBackgroundResource(R.drawable.bg_btn_orange);
        } else {
            h.btnAction.setBackgroundResource(R.drawable.bg_btn_green);
        }

        // Progress bar visibility
        if (item.isHasProgress()) {
            h.layoutProgress.setVisibility(View.VISIBLE);
            h.progressBar.setProgress(item.getProgressPercent());
            h.tvProgressLabel.setText(item.getProgressCurrent() + "/" + item.getProgressMax());
        } else {
            h.layoutProgress.setVisibility(View.GONE);
        }

        h.btnAction.setOnClickListener(v -> {
            if (listener != null) listener.onAction(item, h.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    // ─── ViewHolder ────────────────────────────────────────────────────────────

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle, tvDiscount, tvExpiry, btnAction, tvProgressLabel;
        ProgressBar progressBar;
        LinearLayout layoutProgress;

        VH(@NonNull View itemView) {
            super(itemView);
            ivIcon         = itemView.findViewById(R.id.ivVoucherIcon);
            tvTitle        = itemView.findViewById(R.id.tvVoucherTitle);
            tvDiscount     = itemView.findViewById(R.id.tvVoucherDiscount);
            tvExpiry       = itemView.findViewById(R.id.tvVoucherExpiry);
            btnAction      = itemView.findViewById(R.id.btnVoucherAction);
            layoutProgress = itemView.findViewById(R.id.layoutProgress);
            progressBar    = itemView.findViewById(R.id.progressVoucher);
            tvProgressLabel= itemView.findViewById(R.id.tvProgressLabel);
        }
    }
}
