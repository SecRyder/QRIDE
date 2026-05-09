package com.example.qride.model;

import androidx.annotation.NonNull;

/**
 * Enum định nghĩa các hành động có thể thực hiện trên Voucher.
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
        
        String clean = action.toLowerCase().trim();
        switch (clean) {
            case "action_use_now":    return CLAIM;
            case "action_invite_now": return INVITE;
            case "action_register":   return REGISTER_VIP;
            case "action_checkin":    return CHECKIN;
            case "action_perform":    return PERFORM;
            case "action_topup":      return TOPUP;
            case "action_using":      return USING;
            case "action_in_progress": return IN_PROGRESS;
            default:
                try {
                    return VoucherAction.valueOf(action.toUpperCase().trim());
                } catch (Exception e) {
                    return UNKNOWN;
                }
        }
    }
}
