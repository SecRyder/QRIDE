package com.example.qride.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.qride.R;
import com.example.qride.thanhtoan.Transaction;
import com.example.qride.thanhtoan.TransactionDetailActivity;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private Context context;
    private List<Transaction> list;

    public TransactionAdapter(Context context, List<Transaction> list) {
        this.context = context;
        this.list = list;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvAmount, tvStatus;
        ImageView imgIcon;
        LinearLayout root;

        public ViewHolder(View v) {
            super(v);
            root = v.findViewById(R.id.rootLayout);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvDate = v.findViewById(R.id.tvDate);
            tvAmount = v.findViewById(R.id.tvAmount);
            tvStatus = v.findViewById(R.id.tvStatus);
            imgIcon = v.findViewById(R.id.imgIcon);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder h, int position) {
        Transaction t = list.get(position);

        h.tvTitle.setText(
                t.description != null && !t.description.isEmpty()
                        ? t.description
                        : "Giao dịch"
        );
        h.tvDate.setText(t.created_at.replace("T", " "));
        // ===== FORMAT AMOUNT =====
        String money = String.format("%,d đ", Math.abs(t.amount));

        if (t.amount < 0) {
            h.tvAmount.setText("-" + money);
            h.tvAmount.setTextColor(Color.RED);
        } else {
            h.tvAmount.setText("+" + money);
            h.tvAmount.setTextColor(Color.parseColor("#00A86B"));
        }

        if (t.type.equals("payment") || t.type.equals("withdraw")) {
            h.tvAmount.setText("-" + money);
            h.tvAmount.setTextColor(Color.RED);
        } else {
            h.tvAmount.setText("+" + money);
            h.tvAmount.setTextColor(Color.parseColor("#00A86B"));
        }

        // ===== ICON =====
        if (t.type.equals("topup")) {
            h.imgIcon.setImageResource(R.drawable.naptien);
        } else if (t.type.equals("withdraw")) {
            h.imgIcon.setImageResource(R.drawable.rutien);
        } else {
            h.imgIcon.setImageResource(R.drawable.payment_success);
        }

        // ===== STATUS =====
        h.tvStatus.setText("Thành công");

        // ===== XEN KẼ MÀU =====
        if (position % 2 == 0) {
            h.itemView.setBackgroundColor(Color.parseColor("#F5F5F5"));
        } else {
            h.itemView.setBackgroundColor(Color.WHITE);
        }
        h.root.setOnClickListener(v -> {
            Intent intent = new Intent(context, TransactionDetailActivity.class);
            intent.putExtra("transaction_id", t.id);
            if (t.rental_id != null) {
                intent.putExtra("rental_id", t.rental_id);
            }
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}