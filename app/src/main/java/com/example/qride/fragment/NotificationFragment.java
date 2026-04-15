package com.example.qride.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qride.MainActivity;
import com.example.qride.R;
import com.example.qride.adapter.NotificationAdapter;
import com.example.qride.model.NotificationModel;
import com.example.qride.sqlite.NotificationDAO;

import java.util.List;

public class NotificationFragment extends Fragment {

    private RecyclerView rvNotifications;
    private TextView tvEmpty;
    private NotificationDAO notifDAO;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        notifDAO = new NotificationDAO(requireContext());
        rvNotifications = view.findViewById(R.id.rvNotifications);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).onBackPressed();
            }
        });

        loadNotifications();
        
        // Khi đã vào xem, đánh dấu tất cả là đã đọc
        notifDAO.markAllAsRead();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateNotifBadge();
        }
    }

    private void loadNotifications() {
        List<NotificationModel> list = notifDAO.getAllNotifications();
        if (list.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvNotifications.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvNotifications.setVisibility(View.VISIBLE);
            rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvNotifications.setAdapter(new NotificationAdapter(requireContext(), list));
        }
    }
}
