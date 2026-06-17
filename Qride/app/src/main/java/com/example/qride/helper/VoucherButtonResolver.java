package com.example.qride.helper;

import android.content.Context;
import com.example.qride.R;
import com.example.qride.model.VoucherAction;
import com.example.qride.model.VoucherModel;
import com.example.qride.model.VoucherStatus;

/**
 * Resolver để quyết định trạng thái hiển thị của nút bấm dựa trên Logic Voucher.
 */
public class VoucherButtonResolver {

    public static class ButtonState {
        public String text;
        public int backgroundRes;
        public boolean isEnabled;
        public float alpha;

        public ButtonState(String text, int backgroundRes, boolean isEnabled, float alpha) {
            this.text = text;
            this.backgroundRes = backgroundRes;
            this.isEnabled = isEnabled;
            this.alpha = alpha;
        }
    }

    public static ButtonState resolve(Context context, VoucherModel item) {
        if (context == null || item == null) {
            return new ButtonState("", R.drawable.bg_btn_gray, false, 1.0f);
        }

        // 1. Trạng thái Hết hạn / Đã dùng
        if (item.isExpired()) {
            return new ButtonState(context.getString(R.string.voucher_status_expired), R.drawable.bg_btn_gray, false, 0.6f);
        }
        if (item.isUsed()) {
            return new ButtonState(context.getString(R.string.voucher_status_used), R.drawable.bg_btn_gray, false, 1.0f);
        }

        // 2. Lấy Text từ Helper
        String actionText = VoucherLocalizationHelper.getButtonText(context, item.getAction(), item.getStatus());

        // 3. Logic màu sắc & trạng thái đặc biệt
        int bgRes = R.drawable.bg_btn_green;
        boolean isEnabled = true;

        if (item.getAction() == VoucherAction.USING) {
            // Nếu đang dùng: Đổi sang màu Cam để nổi bật
            bgRes = R.drawable.bg_btn_orange;
        } else if (item.getButtonType() == VoucherModel.ButtonType.ORANGE) {
            bgRes = R.drawable.bg_btn_orange;
        }

        return new ButtonState(actionText, bgRes, isEnabled, 1.0f);
    }
}
