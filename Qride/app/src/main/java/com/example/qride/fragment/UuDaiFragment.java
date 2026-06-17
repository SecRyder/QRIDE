package com.example.qride.fragment;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.qride.MainActivity;
import com.example.qride.R;
import com.example.qride.adapter.VoucherAdapter;
import com.example.qride.helper.APIHelper;
import com.example.qride.helper.VoucherActionHandler;
import com.example.qride.model.VoucherModel;
import com.example.qride.sqlite.NotificationDAO;
import com.example.qride.sqlite.UserDAO;
import com.example.qride.sqlite.VoucherDAO;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fragment hiển thị danh sách ưu đãi và nhiệm vụ.
 * Thực hiện logic gọi API và điều phối hành động Voucher.
 */
public class UuDaiFragment extends Fragment implements VoucherActionHandler.OnVoucherActionListener {

    private static final String TAG = "UuDaiFragment";

    private TextView tabTichQua, tabGoiHoiVien;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressLoading;
    private LinearLayout layoutEmptyState;
    private TextView tvEmptyMessage;
    private TextView btnRetry;

    private VoucherAdapter adapter;
    private List<VoucherModel> tichQuaList = new ArrayList<>();
    private List<VoucherModel> goiHoiVienList = new ArrayList<>();
    private boolean isCurrentTichQua = true;
    private VoucherDAO voucherDAO;
    private NotificationDAO notificationDAO;
    private UserDAO userDAO;

    private final AtomicInteger pendingRequests = new AtomicInteger(0);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_uu_dai, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        voucherDAO = new VoucherDAO(requireContext());
        notificationDAO = new NotificationDAO(requireContext());
        userDAO = new UserDAO(requireContext());

        // Ánh xạ Views
        tabTichQua       = view.findViewById(R.id.tabTichQua);
        tabGoiHoiVien    = view.findViewById(R.id.tabGoiHoiVien);
        recyclerView     = view.findViewById(R.id.listVouchers);
        swipeRefresh     = view.findViewById(R.id.swipeRefresh);
        progressLoading  = view.findViewById(R.id.progressLoading);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        tvEmptyMessage   = view.findViewById(R.id.tvEmptyMessage);
        btnRetry         = view.findViewById(R.id.btnRetry);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new VoucherAdapter(requireContext(), new ArrayList<>(), (item, pos) -> {
            VoucherActionHandler.handle(requireContext(), item, this);
        });
        recyclerView.setAdapter(adapter);

        // Load cache
        tichQuaList    = voucherDAO.getVouchersByType("TICH_QUA");
        goiHoiVienList = voucherDAO.getVouchersByType("GOI_HOI_VIEN");
        showTab(true);

        tabTichQua.setOnClickListener(v    -> showTab(true));
        tabGoiHoiVien.setOnClickListener(v -> showTab(false));
        swipeRefresh.setOnRefreshListener(this::syncAllVouchers);
        btnRetry.setOnClickListener(v -> {
            showLoading(true);
            syncAllVouchers();
        });

        showLoading(true);
        syncAllVouchers();
    }

    private void syncAllVouchers() {
        if (!isAdded()) return;
        pendingRequests.set(2);
        fetchVouchersFromServer("TICH_QUA");
        fetchVouchersFromServer("GOI_HOI_VIEN");
    }

    private void fetchVouchersFromServer(String type) {
        Context context = getContext();
        if (context == null || !isAdded()) return;

        String url = APIHelper.VOUCHERS + "?type=" + type;
        String token = APIHelper.getToken(context);
        if (token == null) {
            onRequestFinished();
            return;
        }

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    if (!isAdded()) return;
                    try {
                        List<VoucherModel> serverList = new ArrayList<>();
                        for (int i = 0; i < response.length(); i++) {
                            VoucherModel m = VoucherModel.fromJson(response.getJSONObject(i));
                            if (m != null) serverList.add(m);
                        }

                        voucherDAO.saveVouchers(serverList, type);
                        if (type.equals("TICH_QUA")) tichQuaList = serverList;
                        else goiHoiVienList = serverList;

                        if (isCurrentTichQua == type.equals("TICH_QUA")) {
                            adapter.updateList(serverList);
                            updateEmptyState(serverList);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error: " + e.getMessage());
                    } finally {
                        onRequestFinished();
                    }
                },
                error -> onRequestFinished()
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", "Bearer " + token);
                return params;
            }
        };
        Volley.newRequestQueue(context).add(request);
    }

    private void onRequestFinished() {
        if (pendingRequests.decrementAndGet() <= 0 && isAdded()) {
            showLoading(false);
            swipeRefresh.setRefreshing(false);
            updateEmptyState(isCurrentTichQua ? tichQuaList : goiHoiVienList);
        }
    }

    @Override
    public void onServerActionRequired(VoucherModel voucher, String title) {
        if (voucher.getAction() == com.example.qride.model.VoucherAction.CLAIM) {
            activateVoucherOnServer(voucher.getId(), title);
        } else {
            sendVoucherRequest(APIHelper.UPDATE_VOUCHER_PROGRESS, voucher.getId(), title, R.string.msg_progress_updated);
        }
    }

    private void activateVoucherOnServer(int voucherId, String title) {
        sendVoucherRequest(APIHelper.ACTIVATE_VOUCHER, voucherId, title, R.string.msg_activated);
    }

    private void sendVoucherRequest(String url, int voucherId, String title, int successResId) {
        Context context = getContext();
        if (context == null || !isAdded()) return;

        JSONObject body = new JSONObject();
        try { body.put("voucherId", voucherId); } catch (Exception ignored) {}

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body,
                response -> {
                    if (!isAdded()) return;
                    
                    // 1. Thêm thông báo vào SQLite
                    int userId = userDAO.getUserId();
                    String msg = getString(successResId, title);
                    notificationDAO.addNotification(userId, "Ưu đãi", msg, "PROMOTION");

                    // 2. Cập nhật UI Badge (nếu MainActivity có method này)
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).updateNotifBadge();
                    }

                    // 3. Hiển thị thông báo thành công cho người dùng
                    if (APIHelper.ACTIVATE_VOUCHER.equals(url)) {
                        new androidx.appcompat.app.AlertDialog.Builder(context)
                                .setTitle("Kích hoạt thành công")
                                .setMessage("Ưu đãi \"" + title + "\" đã sẵn sàng! Hệ thống sẽ tự động áp dụng giảm giá cho chuyến đi tiếp theo của bạn.")
                                .setPositiveButton("Tuyệt vời", null)
                                .show();
                    } else {
                        showToast(msg);
                    }

                    // 4. Refresh để nút đổi sang "Đang dùng" (Server sẽ trả về status/action mới)
                    syncAllVouchers();
                },
                error -> {
                    if (isAdded()) showToast(getString(R.string.error_server));
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", "Bearer " + APIHelper.getToken(requireContext()));
                return params;
            }
        };
        Volley.newRequestQueue(context).add(request);
    }

    private void showTab(boolean isTichQua) {
        isCurrentTichQua = isTichQua;
        tabTichQua.setBackgroundResource(isTichQua ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
        tabGoiHoiVien.setBackgroundResource(isTichQua ? R.drawable.bg_tab_inactive : R.drawable.bg_tab_active);
        List<VoucherModel> list = isTichQua ? tichQuaList : goiHoiVienList;
        adapter.updateList(list);
        updateEmptyState(list);
    }

    private void showLoading(boolean show) {
        if (!isAdded()) return;
        progressLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) { recyclerView.setVisibility(View.GONE); layoutEmptyState.setVisibility(View.GONE); }
    }

    private void updateEmptyState(List<VoucherModel> list) {
        if (!isAdded()) return;
        boolean empty = (list == null || list.isEmpty());
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        layoutEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty && tvEmptyMessage != null) {
            tvEmptyMessage.setText(isCurrentTichQua ? R.string.voucher_empty_tich_qua : R.string.voucher_empty_membership);
        }
    }

    private void showToast(String msg) {
        if (isAdded()) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
