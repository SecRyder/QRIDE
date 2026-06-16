package com.example.qride.helper;

import android.content.Context;
import com.example.qride.R;
import com.example.qride.model.VoucherModel;
import com.example.qride.model.VoucherStatus;

/**
 * Resolver để quyết định trạng thái hiển thị của nút bấm dựa trên Logic Voucher.
 * Tách biệt UI Logic ra khỏi Adapter.
 */
public class VoucherButtonResolver {

    public static class ButtonState {
        public String text;
        public int backgroundRes;
        public boolean isEnabled;
        public float alpha;

        public ButtonState(String text, int backHOIgroundRes, boolean isEnabled, float alpha) {
            this.text = text;
            this.backgroundRes = backgroundRes;
            this.isEnabled = isEnabled;
            this.alpha = alpha;
        }
    }

    /**
     * Phân giải trạng thái nút bấm từ Model.
     */
    public static ButtonState resolve(Context context, VoucherModel item) {
        if (context == null || item == null) {
            return new ButtonState("", R.drawable.bg_btn_gray, false, 1.0f);
        }

        // 1. Kiểm tra trạng thái đặc biệt trước (Ghi đè hành động)
        if (item.isExpired()) {
            return new ButtonState(
                    context.getString(R.string.voucher_status_expired),
                    R.drawable.bg_btn_gray,
                    false,
                    0.6f
            );
        }

        if (item.isUsed()) {
            return new ButtonState(
                    context.getString(R.string.voucher_status_used),
                    R.drawable.bg_btn_gray,
                    false,
                    1.0f
            );
        }

        // 2. Lấy Text dựa trên Hành động (Action)
        String actionText = VoucherLocalizationHelper.getButtonText(context, item.getAction(), item.getStatus());

        // 3. Quyết định màu sắc dựa trên ButtonType từ Model
        int bgRes = (item.getButtonType() == VoucherModel.ButtonType.ORANGE) 
                ? R.drawable.bg_btn_orange 
                : R.drawable.bg_btn_green;

        return new ButtonState(actionText, bgRes, true, 1.0f);
    }
}
