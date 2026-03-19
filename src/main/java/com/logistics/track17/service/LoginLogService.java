package com.logistics.track17.service;

import com.logistics.track17.entity.LoginLog;
import com.logistics.track17.mapper.LoginLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class LoginLogService {

    private final LoginLogMapper loginLogMapper;

    public LoginLogService(LoginLogMapper loginLogMapper) {
        this.loginLogMapper = loginLogMapper;
    }

    /**
     * 异步记录登录日志
     */
    @Async
    public void recordLogin(Long userId, String username, String loginType, String loginResult,
            String ipAddress, String userAgent, String errorMsg) {
        try {
            LoginLog loginLog = new LoginLog();
            loginLog.setUserId(userId);
            loginLog.setUsername(username);
            loginLog.setLoginType(loginType);
            loginLog.setLoginResult(loginResult);
            loginLog.setIpAddress(ipAddress);
            loginLog.setUserAgent(userAgent);
            loginLog.setErrorMsg(errorMsg);

            // 解析 User-Agent
            if (userAgent != null) {
                loginLog.setDevice(parseDevice(userAgent));
                loginLog.setBrowser(parseBrowser(userAgent));
                loginLog.setOs(parseOs(userAgent));
            }

            loginLogMapper.insert(loginLog);
            log.debug("Login log saved: user={}, type={}, result={}", username, loginType, loginResult);
        } catch (Exception e) {
            log.error("Failed to save login log: {}", e.getMessage(), e);
        }
    }

    public List<LoginLog> getLoginLogs(int page, int size, String username, String loginType,
            String loginResult, LocalDateTime startTime, LocalDateTime endTime) {
        int offset = (page - 1) * size;
        return loginLogMapper.selectByPage(offset, size, username, loginType, loginResult, startTime, endTime);
    }

    public Long countLoginLogs(String username, String loginType, String loginResult,
            LocalDateTime startTime, LocalDateTime endTime) {
        return loginLogMapper.countAll(username, loginType, loginResult, startTime, endTime);
    }

    private String parseDevice(String ua) {
        if (ua.contains("Mobile") || ua.contains("Android") || ua.contains("iPhone")) {
            if (ua.contains("iPad")) return "iPad";
            if (ua.contains("iPhone")) return "iPhone";
            if (ua.contains("Android")) return "Android";
            return "Mobile";
        }
        return "PC";
    }

    private String parseBrowser(String ua) {
        if (ua.contains("Edg/")) return "Edge";
        if (ua.contains("Chrome/") && !ua.contains("Edg/")) return "Chrome";
        if (ua.contains("Firefox/")) return "Firefox";
        if (ua.contains("Safari/") && !ua.contains("Chrome/")) return "Safari";
        if (ua.contains("DingTalk")) return "DingTalk";
        return "Other";
    }

    private String parseOs(String ua) {
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
