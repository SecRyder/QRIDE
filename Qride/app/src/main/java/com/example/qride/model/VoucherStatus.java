package com.example.qride.model;

import androidx.annotation.NonNull;

/**
 * Enum định nghĩa các trạng thái của Voucher.
 */
public enum VoucherStatus {
    NEW,            // Mới (Có thể hiện badge NEW)
    ACTIVE,         // Đang hoạt động / Có thể nhận
    USED,           // Đã sử dụng
    EXPIRED,        // Đã hết hạn
    IN_PROGRESS,    // Đang trong tiến trình nhiệm vụ
    UNKNOWN;

    @NonNull
    public static VoucherStatus parse(String status) {
        if (status == null || status.isEmpty()) {
            return UNKNOWN;
        }
        try {
            return VoucherStatus.valueOf(status.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
