package com.example.qride.helper;

import android.content.Context;
import androidx.annotation.NonNull;
import com.example.qride.R;
import com.example.qride.model.VoucherAction;
import com.example.qride.model.VoucherModel;
import com.example.qride.model.VoucherStatus;

/**
 * Helper xử lý đa ngôn ngữ cho Voucher mà không hardcode text.
 * Đồng bộ hóa Enum VoucherAction và VoucherStatus với strings.xml.
 */
public class VoucherLocalizationHelper {

    /**
     * Lấy text hiển thị cho nút bấm dựa trên hành động và trạng thái của Voucher.
     */
    @NonNull
    public static String getButtonText(Context context, VoucherAction action, VoucherStatus status) {
        if (context == null) return "";

        // 1. Kiểm tra trạng thái Voucher trước (Đã dùng hoặc Hết hạn)
        if (status == VoucherStatus.USED) {
            return context.getString(R.string.voucher_status_used);
        }
        if (status == VoucherStatus.EXPIRED) {
            return context.getString(R.string.voucher_status_expired);
        }

        // 2. Bảo vệ chống Null cho action
        if (action == null) {
            return context.getString(R.string.voucher_action_view);
        }

        // 3. Xử lý logic hiển thị theo hành động cụ thể
        switch (action) {
            case CLAIM:
                return context.getString(R.string.voucher_action_claim);
            case INVITE:
                return context.getString(R.string.voucher_action_invite);
            case REGISTER_VIP:
                return context.getString(R.string.voucher_action_register_vip);
            case TOPUP:
                return context.getString(R.string.voucher_action_topup);
            case CHECKIN:
                return context.getString(R.string.voucher_action_checkin);
            case USING:
                return context.getString(R.string.voucher_action_using);
            case PERFORM:
            case IN_PROGRESS:
                return context.getString(R.string.voucher_action_in_progress);
            case UNKNOWN:
            default:
                return context.getString(R.string.voucher_action_view);
        }
    }

    /**
     * Lấy tiêu đề hiển thị (Ưu tiên titleKey để đa ngôn ngữ).
     */
    public static String getTitle(Context context, VoucherModel item) {
        if (item == null || context == null) return "";
        String key = item.getTitleKey();
        if (key == null || key.isEmpty()) return item.getTitle();
        
        int resId = context.getResources().getIdentifier(key, "string", context.getPackageName());
        return resId != 0 ? context.getString(resId) : item.getTitle();
    }

    /**
     * Lấy nội dung giảm giá (Ưu tiên Key để đa ngôn ngữ).
     */
    public static String getDiscount(Context context, VoucherModel item) {
        if (item == null || context == null) return "";
        String key = item.getDiscountText();
        if (key == null || key.isEmpty()) return "";
        
        int resId = context.getResources().getIdentifier(key, "string", context.getPackageName());
        return resId != 0 ? context.getString(resId) : key;
    }
}
