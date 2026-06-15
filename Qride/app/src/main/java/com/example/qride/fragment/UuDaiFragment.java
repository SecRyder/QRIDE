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

    // Đếm số lượng request đang chạy để biết khi nào xong
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

        // Load dữ liệu cache trước (hiện ngay)
        tichQuaList    = voucherDAO.getVouchersByType("TICH_QUA");
        goiHoiVienList = voucherDAO.getVouchersByType("GOI_HOI_VIEN");
        showTab(true);

        // Tab click listeners
        tabTichQua.setOnClickListener(v    -> showTab(true));
        tabGoiHoiVien.setOnClickListener(v -> showTab(false));

        // SwipeRefresh
        swipeRefresh.setColorSchemeResources(R.color.xanhNgoc);
        swipeRefresh.setOnRefreshListener(this::syncAllVouchers);

        // Retry button
        btnRetry.setOnClickListener(v -> {
            showLoading(true);
            syncAllVouchers();
        });

        // Lần đầu: show loading bar, sync từ server
        showLoading(true);
        syncAllVouchers();
    }

    // ============================================================
    // Sync từ server
    // ============================================================

    private void syncAllVouchers() {
        if (!isAdded()) return;
        pendingRequests.set(2); // 2 requests: TICH_QUA + GOI_HOI_VIEN
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

                        // Lưu cache
                        voucherDAO.saveVouchers(serverList, type);

                        if (type.equals("TICH_QUA")) tichQuaList = serverList;
                        else goiHoiVienList = serverList;

                        // Cập nhật UI nếu đang xem tab này
                        if (isCurrentTichQua == type.equals("TICH_QUA")) {
                            adapter.updateList(serverList);
                            updateEmptyState(serverList);
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing vouchers: " + e.getMessage());
                    } finally {
                        onRequestFinished();
                    }
                },
                error -> {
                    Log.e(TAG, "Fetch failed (" + type + "): " + error.getMessage());
                    onRequestFinished();
                }
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

    /** Gọi khi mỗi request kết thúc (thành công hay lỗi), tắt indicators khi cả 2 xong */
    private void onRequestFinished() {
        if (pendingRequests.decrementAndGet() <= 0) {
            if (isAdded()) {
                showLoading(false);
                swipeRefresh.setRefreshing(false);
                // Cập nhật empty state theo tab hiện tại
                List<VoucherModel> current = isCurrentTichQua ? tichQuaList : goiHoiVienList;
                updateEmptyState(current);
            }
        }
    }

    // ============================================================
    // Server Actions (callback từ VoucherActionHandler)
    // ============================================================

    @Override
    public void onServerActionRequired(VoucherModel voucher, String title) {
        switch (voucher.getAction()) {
            case CLAIM:
                activateVoucherOnServer(voucher.getId(), title);
                break;
            case PERFORM:
            case CHECKIN:
            case IN_PROGRESS:
                updateVoucherProgressOnServer(voucher.getId(), title);
                break;
            default:
                break;
        }
    }

    private void updateVoucherProgressOnServer(int voucherId, String title) {
        sendVoucherRequest(APIHelper.UPDATE_VOUCHER_PROGRESS, voucherId, title, R.string.msg_progress_updated);
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
                    showToast(getString(successResId, title));
                    // Refresh danh sách sau action thành công
                    syncAllVouchers();
                    // Cập nhật notification badge nếu cần
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).updateNotifBadge();
                    }
                },
                error -> {
                    if (!isAdded()) return;
                    showToast(getString(R.string.error_server));
                    Log.e(TAG, "Voucher action failed: " + error.getMessage());
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

    // ============================================================
    // UI Helpers
    // ============================================================

    private void showTab(boolean isTichQua) {
        isCurrentTichQua = isTichQua;
        tabTichQua.setBackgroundResource(isTichQua ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
        tabGoiHoiVien.setBackgroundResource(isTichQua ? R.drawable.bg_tab_inactive : R.drawable.bg_tab_active);

        List<VoucherModel> list = isTichQua ? tichQuaList : goiHoiVienList;
        adapter.updateList(list);
        updateEmptyState(list);
    }

    /** Hiển thị / ẩn loading bar đầu trang (không phải swipe refresh) */
    private void showLoading(boolean show) {
        if (!isAdded()) return;
        progressLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }

    /** Cập nhật empty state dựa trên danh sách hiện tại */
    private void updateEmptyState(List<VoucherModel> list) {
        if (!isAdded()) return;
        boolean empty = (list == null || list.isEmpty());

        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        layoutEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);

        if (empty && tvEmptyMessage != null) {
            tvEmptyMessage.setText(isCurrentTichQua
                    ? R.string.voucher_empty_tich_qua
                    : R.string.voucher_empty_membership);
        }
    }

    private void showToast(String msg) {
        if (isAdded()) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
