package com.example.qride.model;

/**
 * Model cho mỗi item voucher / nhiệm vụ trong màn hình Ưu đãi.
 */
public class VoucherModel {

    public enum ButtonType { GREEN, ORANGE }

    private int id;                 // Thêm ID để định danh trong DB
    private int iconResId;          // drawable icon
    private String title;           // "Nạp tiền lần đầu"
    private String discountText;    // "Voucher giảm 5.000đ"
    private String expiry;          // "HSD: 31/12/2025"
    private String actionLabel;     // "Dùng ngay" / "Mời ngay" / ...
    private ButtonType buttonType;
    private boolean hasProgress;
    private int progressCurrent;
    private int progressMax;

    public VoucherModel(int id, int iconResId, String title, String discountText,
                        String expiry, String actionLabel, ButtonType buttonType) {
        this.id          = id;
        this.iconResId   = iconResId;
        this.title       = title;
        this.discountText = discountText;
        this.expiry      = expiry;
        this.actionLabel = actionLabel;
        this.buttonType  = buttonType;
        this.hasProgress = false;
    }

    /** Constructor with progress bar */
    public VoucherModel(int id, int iconResId, String title, String discountText,
                        String expiry, String actionLabel, ButtonType buttonType,
                        int progressCurrent, int progressMax) {
        this(id, iconResId, title, discountText, expiry, actionLabel, buttonType);
        this.hasProgress     = true;
        this.progressCurrent = progressCurrent;
        this.progressMax     = progressMax;
    }

    // ─── Getters ───────────────────────────────────────────────────────────────

    public int getId()              { return id; }
    public int getIconResId()       { return iconResId; }
    public String getTitle()        { return title; }
    public String getDiscountText() { return discountText; }
    public String getExpiry()       { return expiry; }
    public String getActionLabel()  { return actionLabel; }
    public ButtonType getButtonType(){ return buttonType; }
    public boolean isHasProgress()  { return hasProgress; }
    public int getProgressCurrent() { return progressCurrent; }
    public int getProgressMax()     { return progressMax; }

    /** Returns 0-100 for ProgressBar */
    public int getProgressPercent() {
        if (progressMax == 0) return 0;
        return (int) ((progressCurrent / (float) progressMax) * 100);
    }
}
