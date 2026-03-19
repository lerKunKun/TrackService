package com.logistics.track17.service;

import com.logistics.track17.dto.OnlineSession;
import com.logistics.track17.util.JwtUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OnlineSessionService {

    private static final String SESSION_PREFIX = "login:session:";
    private static final String USER_SESSIONS_PREFIX = "login:user:sessions:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    public OnlineSessionService(RedisTemplate<String, Object> redisTemplate, JwtUtil jwtUtil) {
        this.redisTemplate = redisTemplate;
        this.jwtUtil = jwtUtil;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 创建在线会话（登录时调用）
     */
    public void createSession(Long userId, String username, String token,
            String ipAddress, String userAgent, String loginSource) {
        try {
            String tokenHash = getTokenHash(token);
            OnlineSession session = new OnlineSession();
            session.setSessionId(tokenHash);
            session.setUserId(userId);
            session.setUsername(username);
            session.setIpAddress(ipAddress);
            session.setLoginTime(LocalDateTime.now());
            session.setLoginSource(loginSource);

            if (userAgent != null) {
                session.setDevice(parseDevice(userAgent));
                session.setBrowser(parseBrowser(userAgent));
                session.setOs(parseOs(userAgent));
            }

            String sessionJson = objectMapper.writeValueAsString(session);
            long ttlSeconds = jwtExpiration / 1000;

            // 存储会话信息
            redisTemplate.opsForValue().set(SESSION_PREFIX + tokenHash, sessionJson, ttlSeconds, TimeUnit.SECONDS);

            // 关联用户 -> 会话映射
            redisTemplate.opsForSet().add(USER_SESSIONS_PREFIX + userId, tokenHash);
            redisTemplate.expire(USER_SESSIONS_PREFIX + userId, ttlSeconds, TimeUnit.SECONDS);

            log.debug("Online session created for user {}, sessionId={}", username, tokenHash);
        } catch (Exception e) {
            log.error("Failed to create online session: {}", e.getMessage(), e);
        }
    }

    /**
     * 移除会话（登出时调用）
     */
    public void removeSession(String token) {
        try {
            String tokenHash = getTokenHash(token);
            String sessionJson = (String) redisTemplate.opsForValue().get(SESSION_PREFIX + tokenHash);
            if (sessionJson != null) {
                OnlineSession session = objectMapper.readValue(sessionJson, OnlineSession.class);
                redisTemplate.opsForSet().remove(USER_SESSIONS_PREFIX + session.getUserId(), tokenHash);
            }
            redisTemplate.delete(SESSION_PREFIX + tokenHash);
            log.debug("Online session removed, sessionId={}", tokenHash);
        } catch (Exception e) {
            log.error("Failed to remove online session: {}", e.getMessage(), e);
        }
    }

    /**
     * 强制下线（管理员，通过sessionId）
     */
    public boolean forceLogout(String sessionId) {
        try {
            String sessionJson = (String) redisTemplate.opsForValue().get(SESSION_PREFIX + sessionId);
            if (sessionJson == null) {
                return false;
            }
            OnlineSession session = objectMapper.readValue(sessionJson, OnlineSession.class);

            // 将对应token加入黑名单 —— 通过 sessionId 找到 token 并黑名单
            // 由于我们只存了 tokenHash，需要通过 Redis key 模式来黑名单
            // 直接删除 session 并在 Redis 中标记 sessionId 为已强制下线
            redisTemplate.opsForValue().set("jwt:blacklist:session:" + sessionId, Boolean.TRUE,
                    jwtExpiration, TimeUnit.MILLISECONDS);

            // 移除会话
            redisTemplate.opsForSet().remove(USER_SESSIONS_PREFIX + session.getUserId(), sessionId);
            redisTemplate.delete(SESSION_PREFIX + sessionId);

            log.info("Force logout session: user={}, sessionId={}", session.getUsername(), sessionId);
            return true;
        } catch (Exception e) {
            log.error("Failed to force logout session: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取用户的所有活跃会话
     */
    public List<OnlineSession> getUserSessions(Long userId) {
        Set<Object> sessionIds = redisTemplate.opsForSet().members(USER_SESSIONS_PREFIX + userId);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Collections.emptyList();
        }
        return sessionIds.stream()
                .map(id -> getSessionByHash((String) id))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有在线会话
     */
    public List<OnlineSession> getAllOnlineSessions() {
        Set<String> keys = redisTemplate.keys(SESSION_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        return keys.stream()
                .map(key -> {
                    try {
                        String json = (String) redisTemplate.opsForValue().get(key);
                        return json != null ? objectMapper.readValue(json, OnlineSession.class) : null;
                    } catch (JsonProcessingException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(OnlineSession::getLoginTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    /**
     * 获取在线用户数（去重）
     */
    public long getOnlineUserCount() {
        List<OnlineSession> sessions = getAllOnlineSessions();
        return sessions.stream().map(OnlineSession::getUserId).distinct().count();
    }

    /**
     * 检查 sessionId 是否已被强制下线
     */
    public boolean isSessionForceLoggedOut(String sessionId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("jwt:blacklist:session:" + sessionId));
    }

    private OnlineSession getSessionByHash(String tokenHash) {
        try {
            String json = (String) redisTemplate.opsForValue().get(SESSION_PREFIX + tokenHash);
            return json != null ? objectMapper.readValue(json, OnlineSession.class) : null;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String getTokenHash(String token) {
        // 使用 token 的前16个字符作为 hash，足够唯一
        return token.length() > 16 ? token.substring(token.length() - 16) : token;
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
