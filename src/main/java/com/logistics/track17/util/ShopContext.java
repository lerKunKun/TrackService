package com.logistics.track17.util;

/**
 * Shop Context Holder
 * Uses ThreadLocal to manage current shop context in multi-shop scenarios
 * This ensures data isolation at shop level
 */
public class ShopContext {

    private static final ThreadLocal<Long> CURRENT_SHOP_ID = new ThreadLocal<>();

    /**
     * Set current shop ID for the request thread
     * 
     * @param shopId Shop ID
     */
    public static void setCurrentShopId(Long shopId) {
        CURRENT_SHOP_ID.set(shopId);
    }

    /**
     * Get current shop ID from thread context
     * 
     * @return Current shop ID, or null if not set
     */
    public static Long getCurrentShopId() {
        return CURRENT_SHOP_ID.get();
    }

    /**
     * Check if shop context is set
     * 
     * @return true if shop ID is set in context
     */
    public static boolean hasShopContext() {
        return CURRENT_SHOP_ID.get() != null;
    }

    /**
     * Clear shop context for current thread
     * IMPORTANT: Must be called after request processing to prevent memory leaks
     */
    public static void clear() {
        CURRENT_SHOP_ID.remove();
    }
}
