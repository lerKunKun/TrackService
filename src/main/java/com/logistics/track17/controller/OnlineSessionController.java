package com.logistics.track17.controller;

import com.logistics.track17.annotation.RequireAuth;
import com.logistics.track17.dto.OnlineSession;
import com.logistics.track17.dto.Result;
import com.logistics.track17.service.OnlineSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/online-sessions")
@RequireAuth(admin = true)
public class OnlineSessionController {

    private final OnlineSessionService onlineSessionService;

    public OnlineSessionController(OnlineSessionService onlineSessionService) {
        this.onlineSessionService = onlineSessionService;
    }

    @GetMapping
    public Result<List<OnlineSession>> list() {
        List<OnlineSession> sessions = onlineSessionService.getAllOnlineSessions();
        return Result.success(sessions);
    }

    @GetMapping("/count")
    public Result<Map<String, Long>> count() {
        List<OnlineSession> sessions = onlineSessionService.getAllOnlineSessions();
        Map<String, Long> counts = new HashMap<>();
        counts.put("userCount", onlineSessionService.getOnlineUserCount());
        counts.put("sessionCount", (long) sessions.size());
        return Result.success(counts);
    }

    @GetMapping("/user/{userId}")
    public Result<List<OnlineSession>> userSessions(@PathVariable Long userId) {
        List<OnlineSession> sessions = onlineSessionService.getUserSessions(userId);
        return Result.success(sessions);
    }

    @DeleteMapping("/{sessionId}")
    public Result<Object> forceLogout(@PathVariable String sessionId) {
        boolean success = onlineSessionService.forceLogout(sessionId);
        if (success) {
            return Result.success("已强制下线", null);
        }
        return Result.error(404, "会话不存在或已过期");
    }
}
