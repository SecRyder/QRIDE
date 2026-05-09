package com.example.qride.model;

import androidx.annotation.NonNull;

/**
 * Enum định nghĩa loại Voucher.
 */
public enum VoucherType {
    TICH_QUA,       // Tích lũy quà tặng (Dựa trên nhiệm vụ)
    GOI_HOI_VIEN,   // Gói thành viên / VIP
    CHECKIN,        // Điểm danh nhận quà
    NAP_TIEN,       // Khuyến mãi nạp tiền
    UNKNOWN;

    @NonNull
    public static VoucherType parse(String type) {
        if (type == null || type.isEmpty()) {
            return UNKNOWN;
        }
        try {
            return VoucherType.valueOf(type.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
