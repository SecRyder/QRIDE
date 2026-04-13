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

import com.example.qride.R;
import com.example.qride.adapter.VoucherAdapter;
import com.example.qride.model.VoucherModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Màn hình Ưu đãi – hiển thị danh sách voucher theo 2 tab:
 *   • Tích quà
 *   • Gói hội viên
 */
public class UuDaiFragment extends Fragment {

    private TextView tabTichQua, tabGoiHoiVien;
    private RecyclerView recyclerView;

    private List<VoucherModel> tichQuaList    = new ArrayList<>();
    private List<VoucherModel> goiHoiVienList = new ArrayList<>();

    private boolean isTichQuaTab = true;

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

        tabTichQua    = view.findViewById(R.id.tabTichQua);
        tabGoiHoiVien = view.findViewById(R.id.tabGoiHoiVien);
        recyclerView  = view.findViewById(R.id.listVouchers);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        buildData();
        showTab(true);   // mặc định tab Tích quà

        tabTichQua.setOnClickListener(v    -> showTab(true));
        tabGoiHoiVien.setOnClickListener(v -> showTab(false));
    }

    // ─── Dữ liệu mẫu ──────────────────────────────────────────────────────────

    private void buildData() {
        // Tab Tích quà
        tichQuaList.add(new VoucherModel(
                R.drawable.ic_wallet,
                "Nạp tiền lần đầu",
                "Voucher giảm 5.000đ",
                "HSD: 31/12/2025",
                "Dùng ngay",
                VoucherModel.ButtonType.GREEN
        ));

        tichQuaList.add(new VoucherModel(
                R.drawable.ic_bike_station,
                "Hoàn thành chuyến đầu",
                "Voucher giảm 50%",
                "HSD: 31/12/2025",
                "Dùng ngay",
                VoucherModel.ButtonType.GREEN
        ));

        tichQuaList.add(new VoucherModel(
                R.drawable.ic_membership_crown,
                "Đăng ký gói hội viên",
                "Voucher giảm 30%",
                "HSD: 31/12/2025",
                "Dùng ngay",
                VoucherModel.ButtonType.GREEN
        ));

        tichQuaList.add(new VoucherModel(
                R.drawable.ic_membership,
                "Mời bạn bè sử dụng app",
                "Voucher miễn phí chuyến đi 30 phút",
                "",
                "Mời ngay",
                VoucherModel.ButtonType.ORANGE,
                3, 5          // progress 3/5
        ));

        tichQuaList.add(new VoucherModel(
                R.drawable.ic_calendar,
                "Điểm danh 3 lần hôm nay",
                "Voucher giảm 3.000đ",
                "",
                "Điểm danh",
                VoucherModel.ButtonType.ORANGE,
                1, 3          // progress 1/3
        ));

        tichQuaList.add(new VoucherModel(
                R.drawable.ic_star,
                "Đánh giá chuyến đi",
                "Voucher giảm 10%",
                "",
                "Thực hiện",
                VoucherModel.ButtonType.ORANGE,
                1, 3
        ));

        // Tab Gói hội viên (ví dụ)
        goiHoiVienList.add(new VoucherModel(
                R.drawable.ic_membership,
                "Gói Cơ bản – 1 tháng",
                "Miễn phí 30 phút/chuyến",
                "HSD: 31/12/2025",
                "Đăng ký",
                VoucherModel.ButtonType.GREEN
        ));

        goiHoiVienList.add(new VoucherModel(
                R.drawable.ic_membership,
                "Gói Tiêu chuẩn – 3 tháng",
                "Giảm 20% tất cả chuyến đi",
                "HSD: 31/12/2025",
                "Đăng ký",
                VoucherModel.ButtonType.GREEN
        ));

        goiHoiVienList.add(new VoucherModel(
                R.drawable.ic_membership,
                "Gói Cao cấp – 12 tháng",
                "Giảm 40% + ưu tiên xe tốt",
                "HSD: 31/12/2025",
                "Đăng ký",
                VoucherModel.ButtonType.ORANGE
        ));
    }

    // ─── Tab switching ─────────────────────────────────────────────────────────

    private void showTab(boolean isTichQua) {
        isTichQuaTab = isTichQua;

        // Cập nhật visual tab
        tabTichQua.setBackgroundResource(
                isTichQua ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
        tabGoiHoiVien.setBackgroundResource(
                isTichQua ? R.drawable.bg_tab_inactive : R.drawable.bg_tab_active);

        List<VoucherModel> list = isTichQua ? tichQuaList : goiHoiVienList;

        VoucherAdapter adapter = new VoucherAdapter(
                requireContext(),
                list,
                (item, pos) -> onVoucherAction(item)
        );
        recyclerView.setAdapter(adapter);
    }

    // ─── Xử lý nút hành động ──────────────────────────────────────────────────

    private void onVoucherAction(VoucherModel item) {
        switch (item.getActionLabel()) {
            case "Dùng ngay":
                Toast.makeText(requireContext(),
                        "Đã sử dụng: " + item.getTitle(), Toast.LENGTH_SHORT).show();
                break;
            case "Mời ngay":
                // Mở màn hình mời bạn bè / share link
                Toast.makeText(requireContext(),
                        "Mời bạn bè tham gia!", Toast.LENGTH_SHORT).show();
                break;
            case "Điểm danh":
                Toast.makeText(requireContext(),
                        "Điểm danh thành công!", Toast.LENGTH_SHORT).show();
                break;
            case "Thực hiện":
                // Điều hướng đến màn hình đánh giá
                Toast.makeText(requireContext(),
                        "Hãy đánh giá chuyến đi gần nhất", Toast.LENGTH_SHORT).show();
                break;
            case "Đăng ký":
                Toast.makeText(requireContext(),
                        "Đăng ký: " + item.getTitle(), Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }
}