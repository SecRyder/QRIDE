package com.example.qride.helper;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import com.example.qride.R;
import com.example.qride.model.VoucherModel;
import com.example.qride.uudai.VipCheckoutActivity;

/**
 * Lớp xử lý tập trung tất cả các hành động của Voucher.
 * Điều phối giữa UI (Toast, Share) và Logic nghiệp vụ (Callback).
 */
public class VoucherActionHandler {

    /**
     * Interface để Fragment/Activity xử lý logic nghiệp vụ phức tạp hoặc gọi API.
     */
    public interface OnVoucherActionListener {
        void onServerActionRequired(VoucherModel voucher, String title);
    }

    /**
     * Thực thi hành động của Voucher.
     */
    public static void handle(Context context, VoucherModel voucher, OnVoucherActionListener listener) {
        if (voucher == null || context == null) return;

        String title = VoucherLocalizationHelper.getTitle(context, voucher);

        switch (voucher.getAction()) {
            case CLAIM:
            case CHECKIN:
            case PERFORM:
            case IN_PROGRESS:
                // Các hành động này cần tương tác với Server -> Callback cho Fragment
                if (listener != null) {
                    listener.onServerActionRequired(voucher, title);
                }
                break;

            case REGISTER_VIP:
                if (voucher.canOpenVip()) {
                    openVipCheckout(context, voucher, title);
                }
                break;

            case INVITE:
                // Mở màn hình Mời bạn bè chuyên nghiệp
                Intent inviteIntent = new Intent(context, com.example.qride.profile.InviteFriendsActivity.class);
                context.startActivity(inviteIntent);
                break;

            case TOPUP:
                showToast(context, context.getString(R.string.msg_opening_topup));
                Intent topupIntent = new Intent(context, com.example.qride.thanhtoan.NapTienActivity.class);
                context.startActivity(topupIntent);
                break;

            case USING:
                showToast(context, context.getString(R.string.msg_voucher_in_use));
                break;

            case UNKNOWN:
            default:
                showToast(context, context.getString(R.string.msg_developing) + ": " + title);
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
