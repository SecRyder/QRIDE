package com.example.qride.helper;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import com.example.qride.R;
import com.example.qride.model.VoucherModel;
import com.example.qride.uudai.VipCheckoutActivity;

/**
 * Lớp xử lý tập trung tất cả các hành động của Voucher.
 * Điều phối giữa UI và Logic nghiệp vụ thông qua Callback.
 */
public class VoucherActionHandler {

    public interface OnVoucherActionListener {
        void onServerActionRequired(VoucherModel voucher, String title);
    }

    public static void handle(Context context, VoucherModel voucher, OnVoucherActionListener listener) {
        if (voucher == null || context == null) return;

        String title = VoucherLocalizationHelper.getTitle(context, voucher);

        switch (voucher.getAction()) {
            case CLAIM:
                // Kích hoạt Voucher -> Gửi yêu cầu về Fragment để gọi API activate
                if (listener != null) {
                    listener.onServerActionRequired(voucher, title);
                }
                break;

            case USING:
                // Nếu đã đang dùng, hiển thị thông báo thay vì kích hoạt lại
                new androidx.appcompat.app.AlertDialog.Builder(context)
                        .setTitle(title)
                        .setMessage("Ưu đãi này đã được kích hoạt thành công và sẽ được hệ thống tự động áp dụng giảm giá cho chuyến đi tiếp theo của bạn!")
                        .setPositiveButton("Đã hiểu", null)
                        .show();
                break;

            case REGISTER_VIP:
                if (voucher.canOpenVip()) {
                    openVipCheckout(context, voucher, title);
                }
                break;

            case INVITE:
                Intent inviteIntent = new Intent(context, com.example.qride.profile.InviteFriendsActivity.class);
                context.startActivity(inviteIntent);
                break;

            case TOPUP:
                Intent topupIntent = new Intent(context, com.example.qride.thanhtoan.NapTienActivity.class);
                context.startActivity(topupIntent);
                break;

            case CHECKIN:
            case PERFORM:
            case IN_PROGRESS:
                if (listener != null) {
                    listener.onServerActionRequired(voucher, title);
                }
                break;

            case UNKNOWN:
            default:
                showToast(context, context.getString(R.string.msg_developing));
                break;
        }
    }

    private static void openVipCheckout(Context context, VoucherModel voucher, String title) {
        Intent intent = new Intent(context, VipCheckoutActivity.class);
        intent.putExtra("VOUCHER_ID", voucher.getId());
        intent.putExtra("VOUCHER_TITLE", title);
        intent.putExtra("VOUCHER_DISCOUNT", VoucherLocalizationHelper.getDiscount(context, voucher));
        intent.putExtra("VOUCHER_PRICE", voucher.getPrice());
        context.startActivity(intent);
    }

    private static void showToast(Context context, String message) {
        if (context != null && message != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }
}
