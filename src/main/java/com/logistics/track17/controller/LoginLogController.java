package com.logistics.track17.controller;

import com.logistics.track17.annotation.RequireAuth;
import com.logistics.track17.dto.PageResult;
import com.logistics.track17.dto.Result;
import com.logistics.track17.entity.LoginLog;
import com.logistics.track17.service.LoginLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/login-logs")
@RequireAuth(admin = true)
public class LoginLogController {

    private final LoginLogService loginLogService;

    public LoginLogController(LoginLogService loginLogService) {
        this.loginLogService = loginLogService;
    }

    @GetMapping
    public Result<PageResult<LoginLog>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String loginType,
            @RequestParam(required = false) String loginResult,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        var list = loginLogService.getLoginLogs(page, pageSize, username, loginType, loginResult, startTime, endTime);
        var total = loginLogService.countLoginLogs(username, loginType, loginResult, startTime, endTime);
        return Result.success(PageResult.of(list, total, page, pageSize));
    }
}
