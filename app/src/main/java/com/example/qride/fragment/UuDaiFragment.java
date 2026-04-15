package com.example.qride.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qride.MainActivity;
import com.example.qride.R;
import com.example.qride.adapter.VoucherAdapter;
import com.example.qride.model.VoucherModel;
import com.example.qride.sqlite.NotificationDAO;
import com.example.qride.sqlite.VoucherDAO;

import java.util.List;

/**
 * Màn hình Ưu đãi – hiển thị danh sách voucher và xử lý logic tiến trình + thông báo.
 */
public class UuDaiFragment extends Fragment {

    private TextView tabTichQua, tabGoiHoiVien;
    private RecyclerView recyclerView;
    private VoucherDAO voucherDAO;
    private NotificationDAO notifDAO;
    private boolean isCurrentTichQua = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_uu_dai, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        voucherDAO = new VoucherDAO(requireContext());
        notifDAO = new NotificationDAO(requireContext());

        tabTichQua    = view.findViewById(R.id.tabTichQua);
        tabGoiHoiVien = view.findViewById(R.id.tabGoiHoiVien);
        recyclerView  = view.findViewById(R.id.listVouchers);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        showTab(true);

        tabTichQua.setOnClickListener(v    -> showTab(true));
        tabGoiHoiVien.setOnClickListener(v -> showTab(false));
    }

    private void showTab(boolean isTichQua) {
        isCurrentTichQua = isTichQua;
        tabTichQua.setBackgroundResource(
                isTichQua ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
        tabGoiHoiVien.setBackgroundResource(
                isTichQua ? R.drawable.bg_tab_inactive : R.drawable.bg_tab_active);

        refreshList();
    }

    private void refreshList() {
        String type = isCurrentTichQua ? "TICH_QUA" : "GOI_HOI_VIEN";
        List<VoucherModel> list = voucherDAO.getVouchersByType(type);

        VoucherAdapter adapter = new VoucherAdapter(
                requireContext(),
                list,
                (item, pos) -> onVoucherAction(item)
        );
        recyclerView.setAdapter(adapter);
    }

    private void onVoucherAction(VoucherModel item) {
        String actionKey = item.getActionLabel();
        String title = getStringResource(item.getTitle());

        if (actionKey.equals("action_checkin") || actionKey.equals("action_invite_now") || actionKey.equals("action_perform")) {
            if (voucherDAO.updateProgress(item.getId())) {
                // Thêm thông báo vào SQLite
                if (actionKey.equals("action_checkin")) {
                    notifDAO.addNotification("notif_title_checkin_success", "notif_msg_checkin_success", "CHECKIN");
                } else {
                    notifDAO.addNotification("notif_title_welcome", title + " " + getString(R.string.msg_progress_updated, ""), "VOUCHER");
                }
                
                Toast.makeText(requireContext(), getString(R.string.msg_progress_updated, title), Toast.LENGTH_SHORT).show();
                updateBadgeAndRefresh();
            }
        } 
        else if (actionKey.equals("action_use_now")) {
            if (voucherDAO.activateVoucher(item.getId())) {
                // Thêm thông báo
                notifDAO.addNotification("notif_title_voucher_activated", getString(R.string.notif_msg_voucher_activated, title), "VOUCHER");
                
                Toast.makeText(requireContext(), getString(R.string.msg_activated, title), Toast.LENGTH_SHORT).show();
                updateBadgeAndRefresh();
            }
        } 
        else if (actionKey.equals("action_register")) {
            if (voucherDAO.activateMembership(item.getId())) {
                // Thêm thông báo
                notifDAO.addNotification("notif_title_membership_success", getString(R.string.notif_msg_membership_success, title), "VOUCHER");
                
                Toast.makeText(requireContext(), getString(R.string.msg_registered, title), Toast.LENGTH_SHORT).show();
                updateBadgeAndRefresh();
            }
        }
        else if (actionKey.equals("action_using")) {
            Toast.makeText(requireContext(), getString(R.string.action_using), Toast.LENGTH_SHORT).show();
        }
        else if (actionKey.equals("action_in_progress")) {
            Toast.makeText(requireContext(), getString(R.string.action_in_progress), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateBadgeAndRefresh() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateNotifBadge();
        }
        refreshList();
    }

    private String getStringResource(String key) {
        if (key == null || key.isEmpty()) return "";
        int resId = getResources().getIdentifier(key, "string", requireContext().getPackageName());
        return resId != 0 ? getString(resId) : key;
    }
}
