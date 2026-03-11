package com.logistics.track17.util;

/**
 * 请求上下文持有者
 * 使用 ThreadLocal 管理当前请求的公司/店铺上下文
 * 在鉴权拦截器中设置，在请求结束后清理
 */
public class RequestContext {

    private static final ThreadLocal<Context> CONTEXT = new ThreadLocal<>();

    /**
     * 上下文数据
     */
    public static class Context {
        private Long userId;
        private Long companyId;
        private Long shopId;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getCompanyId() {
            return companyId;
        }

        public void setCompanyId(Long companyId) {
            this.companyId = companyId;
        }

        public Long getShopId() {
            return shopId;
        }

        public void setShopId(Long shopId) {
            this.shopId = shopId;
        }
    }

    /**
     * 初始化上下文（至少包含 userId）
     */
    public static void init(Long userId) {
        Context ctx = new Context();
        ctx.setUserId(userId);
        CONTEXT.set(ctx);
    }

    /**
     * 设置公司和店铺上下文
     */
    public static void setCompanyAndShop(Long companyId, Long shopId) {
        Context ctx = CONTEXT.get();
        if (ctx == null) {
            ctx = new Context();
            CONTEXT.set(ctx);
        }
        ctx.setCompanyId(companyId);
        ctx.setShopId(shopId);
    }

    public static Long getCurrentUserId() {
        Context ctx = CONTEXT.get();
        return ctx != null ? ctx.getUserId() : null;
    }

    public static Long getCurrentCompanyId() {
        Context ctx = CONTEXT.get();
        return ctx != null ? ctx.getCompanyId() : null;
    }

    public static Long getCurrentShopId() {
        Context ctx = CONTEXT.get();
        return ctx != null ? ctx.getShopId() : null;
    }

    public static boolean hasShopContext() {
        return getCurrentShopId() != null;
    }

    public static boolean hasCompanyContext() {
        return getCurrentCompanyId() != null;
    }

    /**
     * 清理上下文（必须在请求结束后调用，防止内存泄漏）
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
