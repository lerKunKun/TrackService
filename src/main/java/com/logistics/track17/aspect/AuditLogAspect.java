package com.logistics.track17.aspect;

import com.alibaba.fastjson.JSON;
import com.logistics.track17.annotation.AuditLog;
import com.logistics.track17.entity.AuditLogEntity;
import com.logistics.track17.service.AuditLogService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 审计日志AOP切面
 * 拦截带有 @AuditLog 注解的方法，自动记录操作日志
 */
@Aspect
@Component
@Slf4j
@Order(2) // 在 AuthenticationAspect 之后执行
public class AuditLogAspect {

    @Autowired
    private AuditLogService auditLogService;

    /**
     * 环绕通知：记录审计日志
     */
    @Around("@annotation(auditLog)")
    public Object logAudit(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取 HTTP 请求
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        // 创建审计日志实体
        AuditLogEntity log = new AuditLogEntity();
        log.setOperation(auditLog.operation());
        log.setModule(auditLog.module().isEmpty() ? "未分类" : auditLog.module());

        // 记录方法信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        log.setMethod(signature.getDeclaringTypeName() + "." + signature.getName());

        // 记录请求信息
        if (request != null) {
            log.setIpAddress(getIpAddress(request));
            log.setUserAgent(request.getHeader("User-Agent"));

            // 获取当前用户信息
            Object username = request.getAttribute("username");
            Object userId = request.getAttribute("userId");
            if (username != null) {
                log.setUsername(username.toString());
            }
            if (userId != null) {
                log.setUserId(Long.valueOf(userId.toString()));
            }
        }

        // 记录请求参数
        if (auditLog.logParams()) {
            try {
                Object[] args = joinPoint.getArgs();
                // 过滤掉 HttpServletRequest、HttpServletResponse 等参数
                Object[] filteredArgs = filterSensitiveArgs(args);
                log.setParams(JSON.toJSONString(filteredArgs));
            } catch (Exception e) {
                log.setParams("参数序列化失败: " + e.getMessage());
            }
        }

        try {
            // 执行目标方法
            Object result = joinPoint.proceed();

            // 记录成功
            log.setResult("SUCCESS");
            log.setExecutionTime((int) (System.currentTimeMillis() - startTime));

            // 异步保存日志
            auditLogService.saveAsync(log);

            return result;
        } catch (Throwable e) {
            // 记录失败
            log.setResult("FAILURE");
            log.setErrorMsg(e.getMessage());
            log.setExecutionTime((int) (System.currentTimeMillis() - startTime));

            // 异步保存日志
            auditLogService.saveAsync(log);

            // 继续抛出异常
            throw e;
        }
    }

    /**
     * 获取真实IP地址
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多级代理的情况
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 过滤敏感参数
     */
    private Object[] filterSensitiveArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return new Object[0];
        }

        Object[] filtered = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            // 过滤掉 Servlet 相关对象
            if (arg instanceof HttpServletRequest ||
                    arg instanceof javax.servlet.http.HttpServletResponse ||
                    arg instanceof javax.servlet.ServletContext) {
                filtered[i] = "[" + arg.getClass().getSimpleName() + "]";
            } else {
                filtered[i] = arg;
            }
        }
        return filtered;
    }
}
