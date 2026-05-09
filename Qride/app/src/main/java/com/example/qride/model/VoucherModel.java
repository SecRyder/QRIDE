package com.example.qride.model;

import org.json.JSONObject;
import java.io.Serializable;

/**
 * Model cho mỗi item voucher / nhiệm vụ trong màn hình Ưu đãi.
 * Được thiết kế theo chuẩn Clean Architecture với các helper methods logic.
 */
public class VoucherModel implements Serializable {

    public enum ButtonType { GREEN, ORANGE }

    private int id;
    private VoucherType type;
    private VoucherStatus status;
    private VoucherAction action;
    private String iconName = "ic_wallet";
    private int iconResId = 0;
    private String title = "";           
    private String titleKey = "";        
    private String discountText = "";
    private int price = 0;
    private String expiry = "";
    private String actionLabel = "";     
    private ButtonType buttonType = ButtonType.GREEN;
    private boolean hasProgress = false;
    private int progressCurrent = 0;
    private int progressMax = 0;

    public VoucherModel() {}

    public static VoucherModel fromJson(JSONObject obj) {
        if (obj == null) return null;
        try {
            VoucherModel m = new VoucherModel();
            m.id = obj.optInt("id", 0);
            
            m.type = VoucherType.parse(obj.optString("type", ""));
            m.status = VoucherStatus.parse(obj.optString("status", ""));
            
            String actionStr = obj.optString("action", "UNKNOWN");
            m.action = VoucherAction.parse(actionStr);
            m.actionLabel = actionStr; 
            
            m.iconName = obj.optString("icon", "ic_wallet");
            m.title = obj.optString("title", "");
            m.titleKey = obj.optString("title_key", "");
            m.discountText = obj.optString("discount", "");
            m.price = obj.optInt("price", 0);
            m.expiry = obj.optString("expiry", "");
            
            String btnTypeStr = obj.optString("btn_type", "GREEN");
            m.buttonType = "ORANGE".equalsIgnoreCase(btnTypeStr) ? ButtonType.ORANGE : ButtonType.GREEN;

            m.hasProgress = obj.optBoolean("has_progress", false) || obj.optInt("has_progress", 0) == 1;
            m.progressCurrent = obj.optInt("prog_curr", 0);
            m.progressMax = obj.optInt("prog_max", 0);
            
            return m;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // --- Business Logic Helpers ---

    public boolean isClaimable() {
        return status == VoucherStatus.ACTIVE && action == VoucherAction.CLAIM;
    }

    public boolean isExpired() {
        return status == VoucherStatus.EXPIRED;
    }

    public boolean isUsed() {
        return status == VoucherStatus.USED;
    }

    public boolean isVipPackage() {
        return type == VoucherType.GOI_HOI_VIEN;
    }

    public boolean shouldShowProgress() {
        return hasProgress && status != VoucherStatus.USED && status != VoucherStatus.EXPIRED;
    }

    public int getProgressPercent() {
        if (progressMax <= 0) return 0;
        return (int) ((progressCurrent / (float) progressMax) * 100);
    }

    public boolean canOpenVip() {
        return isVipPackage() && !isUsed() && !isExpired();
    }

    // --- Getters & Setters ---

    public int getId() { return id; }
    public VoucherType getType() { return type != null ? type : VoucherType.UNKNOWN; }
    public VoucherStatus getStatus() { return status != null ? status : VoucherStatus.UNKNOWN; }
    public VoucherAction getAction() { return action != null ? action : VoucherAction.UNKNOWN; }
    public String getIconName() { return iconName != null ? iconName : "ic_wallet"; }
    public int getIconResId() { return iconResId; }
    public String getTitle() { return title != null ? title : ""; }
    public String getTitleKey() { return titleKey != null ? titleKey : ""; }
    public String getDiscountText() { return discountText != null ? discountText : ""; }
    public int getPrice() { return price; }
    public String getExpiry() { return expiry != null ? expiry : ""; }
    public String getActionLabel() { return actionLabel != null ? actionLabel : ""; }
    public ButtonType getButtonType() { return buttonType != null ? buttonType : ButtonType.GREEN; }
    public boolean isHasProgress() { return hasProgress; }
    public int getProgressCurrent() { return progressCurrent; }
    public int getProgressMax() { return progressMax; }

    public void setId(int id) { this.id = id; }
    public void setType(VoucherType type) { this.type = type; }
    public void setStatus(VoucherStatus status) { this.status = status; }
    public void setAction(VoucherAction action) { this.action = action; }
    public void setIconName(String iconName) { this.iconName = iconName; }
    public void setIconResId(int iconResId) { this.iconResId = iconResId; }
    public void setTitle(String title) { this.title = title; }
    public void setTitleKey(String titleKey) { this.titleKey = titleKey; }
    public void setDiscountText(String discountText) { this.discountText = discountText; }
    public void setExpiry(String expiry) { this.expiry = expiry; }
    public void setActionLabel(String actionLabel) { this.actionLabel = actionLabel; }
    public void setButtonType(ButtonType buttonType) { this.buttonType = buttonType; }
    public void setHasProgress(boolean hasProgress) { this.hasProgress = hasProgress; }
    public void setProgressCurrent(int progressCurrent) { this.progressCurrent = progressCurrent; }
    public void setProgressMax(int progressMax) { this.progressMax = progressMax; }
}
