package com.logistics.track17.util;

/**
 * User-Agent 解析工具类
 */
public final class UserAgentParser {

    private UserAgentParser() {
    }

    public static String parseDevice(String ua) {
        if (ua.contains("Mobile") || ua.contains("Android") || ua.contains("iPhone")) {
            if (ua.contains("iPad")) return "iPad";
            if (ua.contains("iPhone")) return "iPhone";
            if (ua.contains("Android")) return "Android";
            return "Mobile";
        }
        return "PC";
    }

    public static String parseBrowser(String ua) {
        if (ua.contains("Edg/")) return "Edge";
        if (ua.contains("Chrome/") && !ua.contains("Edg/")) return "Chrome";
        if (ua.contains("Firefox/")) return "Firefox";
        if (ua.contains("Safari/") && !ua.contains("Chrome/")) return "Safari";
        if (ua.contains("DingTalk")) return "DingTalk";
        return "Other";
    }

    public static String parseOs(String ua) {
        if (ua.contains("Windows NT 10")) return "Windows 10";
        if (ua.contains("Windows NT")) return "Windows";
        if (ua.contains("Mac OS X")) return "macOS";
        if (ua.contains("Linux") && ua.contains("Android")) return "Android";
        if (ua.contains("Linux")) return "Linux";
        if (ua.contains("iPhone OS")) return "iOS";
        if (ua.contains("iPad")) return "iPadOS";
        return "Other";
    }
}
