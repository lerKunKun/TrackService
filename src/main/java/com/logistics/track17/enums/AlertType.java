package com.logistics.track17.enums;

/**
 * Alert类型枚举
 */
public enum AlertType {
    DISPUTE("DISPUTE", "支付争议", "CRITICAL"),
    INFRINGEMENT("INFRINGEMENT", "知识产权侵权", "CRITICAL"),
    ACCOUNT_RESTRICTED("ACCOUNT_RESTRICTED", "账号受限", "CRITICAL"),
    PAYMENT_HOLD("PAYMENT_HOLD", "资金冻结", "CRITICAL"),
    APP_UNINSTALLED("APP_UNINSTALLED", "应用卸载", "HIGH"),
    SHOP_ALERT("SHOP_ALERT", "店铺管理提示", "MEDIUM"),
    DAILY_SUMMARY("DAILY_SUMMARY", "每日汇总", "INFO"),
    MONTHLY_SUMMARY("MONTHLY_SUMMARY", "月度汇总", "INFO"),
    QUARTERLY_SUMMARY("QUARTERLY_SUMMARY", "季度汇总", "INFO"),
    YEARLY_SUMMARY("YEARLY_SUMMARY", "年度汇总", "INFO");

    private final String code;
    private final String description;
    private final String defaultSeverity;

    AlertType(String code, String description, String defaultSeverity) {
        this.code = code;
        this.description = description;
        this.defaultSeverity = defaultSeverity;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getDefaultSeverity() {
        return defaultSeverity;
    }
}
