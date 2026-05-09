package com.example.qride.fragment;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

/**
 * Fragment hiển thị danh sách ưu đãi và nhiệm vụ.
 * Thực hiện logic gọi API và điều phối hành động Voucher.
 */
public class UuDaiFragment extends Fragment implements VoucherActionHandler.OnVoucherActionListener {

    private TextView tabTichQua, tabGoiHoiVien;
    private RecyclerView recyclerView;
    private VoucherAdapter adapter;
    private List<VoucherModel> tichQuaList = new ArrayList<>();
    private List<VoucherModel> goiHoiVienList = new ArrayList<>();
    private boolean isCurrentTichQua = true;
    private VoucherDAO voucherDAO;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_uu_dai, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        voucherDAO = new VoucherDAO(requireContext());

        tabTichQua    = view.findViewById(R.id.tabTichQua);
        tabGoiHoiVien = view.findViewById(R.id.tabGoiHoiVien);
        recyclerView  = view.findViewById(R.id.listVouchers);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Khởi tạo adapter trống trước
        adapter = new VoucherAdapter(requireContext(), new ArrayList<>(), (item, pos) -> {
            VoucherActionHandler.handle(requireContext(), item, this);
        });
        recyclerView.setAdapter(adapter);

        // Load cached data
        tichQuaList = voucherDAO.getVouchersByType("TICH_QUA");
        goiHoiVienList = voucherDAO.getVouchersByType("GOI_HOI_VIEN");

        showTab(true);

        tabTichQua.setOnClickListener(v    -> showTab(true));
        tabGoiHoiVien.setOnClickListener(v -> showTab(false));

        syncAllVouchers();
    }

    private void syncAllVouchers() {
        fetchVouchersFromServer("TICH_QUA");
        fetchVouchersFromServer("GOI_HOI_VIEN");
    }

    private void fetchVouchersFromServer(String type) {
        Context context = getContext();
        if (context == null || !isAdded()) return;

        String url = APIHelper.VOUCHERS + "?type=" + type;
        String token = APIHelper.getToken(context);
        if (token == null) return;

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
                        }
                    } catch (Exception e) {
                        Log.e("UuDaiFragment", "Error parsing: " + e.getMessage());
                    }
                },
                error -> Log.e("API_ERROR", "Fetch failed: " + error.getMessage())
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

    /**
     * Triển khai từ VoucherActionHandler.OnVoucherActionListener.
     * Xử lý các hành động cần tương tác với Server.
     */
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
                    syncAllVouchers(); // Refresh list
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).updateNotifBadge();
                    }
                },
                error -> showToast(getString(R.string.error_server))
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
        
        adapter.updateList(isTichQua ? tichQuaList : goiHoiVienList);
    }

    private void showToast(String msg) {
        if (isAdded()) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
