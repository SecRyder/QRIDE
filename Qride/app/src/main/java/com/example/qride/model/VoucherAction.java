package com.example.qride.model;

import androidx.annotation.NonNull;

/**
 * Enum định nghĩa các hành động có thể thực hiện trên Voucher.
 * Hỗ trợ cả format mới từ server (CLAIM, INVITE...) lẫn format cũ (action_use_now...).
 */
public enum VoucherAction {
    CLAIM,
    INVITE,
    REGISTER_VIP,
    CHECKIN,
    PERFORM,
    TOPUP,
    USING,
    IN_PROGRESS,
    UNKNOWN;

    @NonNull
    public static VoucherAction parse(String action) {
        if (action == null || action.isEmpty()) return UNKNOWN;

        // Thử map từ format key cũ (action_use_now, action_invite_now, ...)
        String clean = action.toLowerCase().trim();
        switch (clean) {
            case "action_use_now":     return CLAIM;
            case "action_invite_now":  return INVITE;
            case "action_register":    return REGISTER_VIP;
            case "action_checkin":     return CHECKIN;
            case "action_perform":     return PERFORM;
            case "action_topup":       return TOPUP;
            case "action_using":       return USING;
            case "action_in_progress": return IN_PROGRESS;
            // Format mới (trực tiếp từ server)
            case "claim":              return CLAIM;
            case "invite":             return INVITE;
            case "register_vip":       return REGISTER_VIP;
            case "checkin":            return CHECKIN;
            case "perform":            return PERFORM;
            case "topup":              return TOPUP;
            case "using":              return USING;
            case "in_progress":        return IN_PROGRESS;
            default:
                // Fallback: thử valueOf trực tiếp
                try {
                    return VoucherAction.valueOf(action.toUpperCase().trim());
                } catch (Exception e) {
                    return UNKNOWN;
                }
        }
    }
}
