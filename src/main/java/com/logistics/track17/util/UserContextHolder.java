package com.logistics.track17.util;

import com.logistics.track17.entity.User;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 用户上下文持有者
 * 提供获取当前登录用户信息的工具方法
 */
public class UserContextHolder {

    /**
     * 获取当前用户ID
     * 
     * @return 当前用户ID，如果未登录返回 null
     */
    public static Long getCurrentUserId() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }

        Object userId = request.getAttribute("userId");
        if (userId == null) {
            return null;
        }

        if (userId instanceof Long) {
            return (Long) userId;
        }

        try {
            return Long.valueOf(userId.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 获取当前用户名
     * 
     * @return 当前用户名，如果未登录返回 null
     */
    public static String getCurrentUsername() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }

        Object username = request.getAttribute("username");
        return username != null ? username.toString() : null;
    }

    /**
     * 获取当前用户对象
     * 
     * @return 当前用户对象，如果未登录返回 null
     */
    public static User getCurrentUser() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }

        Object user = request.getAttribute("user");
        return user instanceof User ? (User) user : null;
    }

    /**
     * 检查是否已登录
     * 
     * @return true 如果已登录，否则 false
     */
    public static boolean isAuthenticated() {
        return getCurrentUserId() != null;
    }

    /**
     * 获取当前 HTTP 请求
     */
    private static HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
