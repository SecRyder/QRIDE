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
    /**
     * Lấy nội dung giảm giá (Ưu tiên Key để đa ngôn ngữ, nếu là số thì hiển thị dạng tiền tệ/phần trăm).
     */
    public static String getDiscount(Context context, VoucherModel item) {
        if (item == null || context == null) return "";
        String key = item.getDiscountText();
        if (key == null || key.isEmpty()) return "";

        // 1. Kiểm tra xem 'key' có phải là một chuỗi số thuần túy hay không (Ví dụ: "100", "50000")
        if (key.matches("\\d+")) {
            // Nếu là số, trả về dạng chuỗi kèm chữ "đ" hoặc xử lý định dạng trực tiếp để tránh Android nhận nhầm thành ID
            try {
                long value = Long.parseLong(key);
                if (value <= 100) {
                    return value + "%"; // Nếu nhỏ hơn hoặc bằng 100 thì có thể là % giảm giá
                } else {
                    return String.format("%,dđ", value); // Định dạng hiển thị tiền tệ đẹp mắt: 50,000đ, 100,000đ
                }
            } catch (NumberFormatException e) {
                return key + ""; // Biện pháp an toàn: luôn cộng chuỗi rỗng để chắc chắn nó là String văn bản
            }
        }

        // 2. Nếu không phải là số (là chữ thực sự như "voucher_free_ship"), tiến hành tìm trong strings.xml
        try {
            int resId = context.getResources().getIdentifier(key, "string", context.getPackageName());
            if (resId != 0) {
                return context.getString(resId);
            }
        } catch (Exception e) {
            // Tránh mọi trường hợp crash liên quan đến Resource
        }

        // 3. Cuối cùng, nếu không tìm thấy ID trong strings.xml, trả về chính chuỗi đó một cách an toàn
        return key + "";
    }
}
