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
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qride.R;
import com.example.qride.helper.VoucherButtonResolver;
import com.example.qride.helper.VoucherLocalizationHelper;
import com.example.qride.model.VoucherModel;
import com.example.qride.model.VoucherStatus;

import java.util.List;

/**
 * Adapter hiển thị danh sách Voucher/Nhiệm vụ theo chuẩn Production.
 * Sử dụng ButtonResolver để quản lý trạng thái nút bấm.
 */
public class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.VH> {

    private final Context context;
    private List<VoucherModel> items;
    private final OnVoucherClickListener listener;

    public interface OnVoucherClickListener {
        void onClick(VoucherModel item, int position);
    }

    public VoucherAdapter(Context context, List<VoucherModel> items, OnVoucherClickListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    /**
     * Cập nhật danh sách sử dụng DiffUtil để tối ưu hiệu năng và animation.
     */
    public void updateList(List<VoucherModel> newList) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new VoucherDiffCallback(this.items, newList));
        this.items = newList;
        result.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_voucher, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        VoucherModel item = items.get(position);

        // 1. Render Icon
        int iconResId = item.getIconResId();
        if (iconResId == 0) {
            iconResId = context.getResources().getIdentifier(item.getIconName(), "drawable", context.getPackageName());
        }
        h.ivIcon.setImageResource(iconResId != 0 ? iconResId : R.drawable.ic_wallet);

        // 2. Render Title & Discount (Dùng LocalizationHelper)
        h.tvTitle.setText(VoucherLocalizationHelper.getTitle(context, item));
        h.tvDiscount.setText(VoucherLocalizationHelper.getDiscount(context, item));
        h.tvExpiry.setText(item.getExpiry());

        // 3. Render Button (Dùng ButtonResolver)
        VoucherButtonResolver.ButtonState btnState = VoucherButtonResolver.resolve(context, item);
        h.btnAction.setText(btnState.text);
        h.btnAction.setBackgroundResource(btnState.backgroundRes);
        h.btnAction.setEnabled(btnState.isEnabled);
        
        // 4. Alpha hiệu ứng (Dùng cho Expired)
        h.itemView.setAlpha(btnState.alpha);

        // 5. Badge "NEW"
        h.badgeNew.setVisibility(item.getStatus() == VoucherStatus.NEW ? View.VISIBLE : View.GONE);

        // 6. Progress Bar (Sử dụng helper logic từ Model)
        if (item.shouldShowProgress()) {
            h.layoutProgress.setVisibility(View.VISIBLE);
            h.progressBar.setProgress(item.getProgressPercent());
            h.tvProgressLabel.setText(item.getProgressCurrent() + "/" + item.getProgressMax());
        } else {
            h.layoutProgress.setVisibility(View.GONE);
        }

        // 7. Click Listener
        h.btnAction.setOnClickListener(v -> {
            if (listener != null) listener.onClick(item, position);
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle, tvDiscount, tvExpiry, btnAction, tvProgressLabel;
        View badgeNew;
        ProgressBar progressBar;
        LinearLayout layoutProgress;

        VH(@NonNull View itemView) {
            super(itemView);
            ivIcon          = itemView.findViewById(R.id.ivVoucherIcon);
            tvTitle         = itemView.findViewById(R.id.tvVoucherTitle);
            tvDiscount      = itemView.findViewById(R.id.tvVoucherDiscount);
            tvExpiry        = itemView.findViewById(R.id.tvVoucherExpiry);
            btnAction       = itemView.findViewById(R.id.btnVoucherAction);
                badgeNew        = itemView.findViewById(R.id.badgeNew);
            layoutProgress  = itemView.findViewById(R.id.layoutProgress);
            progressBar     = itemView.findViewById(R.id.progressVoucher);
            tvProgressLabel = itemView.findViewById(R.id.tvProgressLabel);
        }
    }

    /**
     * DiffCallback để tính toán sự thay đổi giữa 2 danh sách.
     */
    private static class VoucherDiffCallback extends DiffUtil.Callback {
        private final List<VoucherModel> oldList;
        private final List<VoucherModel> newList;

        public VoucherDiffCallback(List<VoucherModel> oldList, List<VoucherModel> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override public int getOldListSize() { return oldList != null ? oldList.size() : 0; }
        @Override public int getNewListSize() { return newList != null ? newList.size() : 0; }

        @Override
        public boolean areItemsTheSame(int oldPos, int newPos) {
            return oldList.get(oldPos).getId() == newList.get(newPos).getId();
        }

        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            VoucherModel oldItem = oldList.get(oldPos);
            VoucherModel newItem = newList.get(newPos);
            return oldItem.getStatus() == newItem.getStatus() &&
                   oldItem.getProgressCurrent() == newItem.getProgressCurrent() &&
                   oldItem.getAction() == newItem.getAction();
        }
    }
}
