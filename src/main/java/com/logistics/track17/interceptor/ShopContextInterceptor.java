package com.logistics.track17.interceptor;

import com.logistics.track17.exception.ForbiddenException;
import com.logistics.track17.service.UserShopService;
import com.logistics.track17.util.ShopContext;
import com.logistics.track17.util.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Shop Context Interceptor
 * Validates user shop access and sets shop context from X-Shop-Id header
 */
@Slf4j
@Component
public class ShopContextInterceptor implements HandlerInterceptor {

    @Autowired
    private UserShopService userShopService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            // 1. Get shop ID from header
            String shopIdHeader = request.getHeader("X-Shop-Id");

            if (shopIdHeader != null && !shopIdHeader.isEmpty()) {
                Long shopId = Long.parseLong(shopIdHeader);
                Long userId = UserContextHolder.getCurrentUserId();

                // 2. Validate if user has access to this shop
                if (userId == null) {
                    log.warn("Shop context requested but user not authenticated");
                    throw new ForbiddenException("User not authenticated");
                }

                if (!userShopService.hasShopAccess(userId, shopId)) {
                    log.warn("User {} attempted to access shop {} without permission", userId, shopId);
                    throw new ForbiddenException("No permission to access shop: " + shopId);
                }

                // 3. Set shop context
                ShopContext.setCurrentShopId(shopId);
                log.debug("Shop context set: shopId={}, userId={}", shopId, userId);
            }

            return true;

        } catch (NumberFormatException e) {
            log.error("Invalid X-Shop-Id header format", e);
            throw new ForbiddenException("Invalid shop ID format");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex) {
        // Clear shop context to prevent memory leaks
        ShopContext.clear();
    }
}
