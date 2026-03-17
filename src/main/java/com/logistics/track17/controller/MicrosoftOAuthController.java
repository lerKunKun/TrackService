package com.logistics.track17.controller;

import com.logistics.track17.service.MicrosoftOAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Microsoft OAuth2 授权接口
 *
 * GET /alert-config/email-oauth/microsoft/auth-url  — 需要登录，返回授权 URL
 * GET /oauth/microsoft/callback                     — 无需登录（在 AOP 排除路径 /api/v1/oauth/ 下）
 */
@Slf4j
@RestController
public class MicrosoftOAuthController {

    private final MicrosoftOAuthService oauthService;

    public MicrosoftOAuthController(MicrosoftOAuthService oauthService) {
        this.oauthService = oauthService;
    }

    /**
     * 获取 Microsoft OAuth2 授权 URL
     * 需要登录后调用，configId 为已保存的邮箱配置 ID
     */
    @GetMapping("/alert-config/email-oauth/microsoft/auth-url")
    public ResponseEntity<Map<String, Object>> getAuthUrl(@RequestParam Long configId) {
        Map<String, Object> result = new HashMap<>();
        if (!oauthService.isConfigured()) {
            result.put("success", false);
            result.put("message", "Microsoft OAuth2 未配置，请在 application.yml 中设置 microsoft.oauth2.client-id 和 client-secret");
            return ResponseEntity.badRequest().body(result);
        }
        String authUrl = oauthService.buildAuthUrl(configId);
        result.put("success", true);
        result.put("authUrl", authUrl);
        return ResponseEntity.ok(result);
    }

    /**
     * Microsoft OAuth2 回调接口（无需登录）
     * Microsoft 授权完成后重定向至此，换取 token 后关闭弹窗并通知父窗口
     */
    @GetMapping(value = "/oauth/microsoft/callback", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDescription) {

        if (error != null) {
            log.warn("Microsoft OAuth2 授权被拒绝: error={}, desc={}", error, errorDescription);
            return ResponseEntity.ok(buildHtmlPage(false,
                    "授权被取消或拒绝：" + (errorDescription != null ? errorDescription : error)));
        }

        if (code == null || state == null) {
            return ResponseEntity.ok(buildHtmlPage(false, "缺少 code 或 state 参数"));
        }

        Long configId = oauthService.getConfigIdFromState(state);
        if (configId == null) {
            return ResponseEntity.ok(buildHtmlPage(false, "授权超时或 state 无效，请重新授权"));
        }

        try {
            oauthService.exchangeCodeAndSave(configId, code);
            log.info("Microsoft OAuth2 授权成功: configId={}", configId);
            return ResponseEntity.ok(buildHtmlPage(true, "授权成功！此窗口将自动关闭"));
        } catch (Exception e) {
            log.error("Microsoft OAuth2 token 换取失败: configId={}", configId, e);
            return ResponseEntity.ok(buildHtmlPage(false, "授权失败：" + e.getMessage()));
        }
    }

    private String buildHtmlPage(boolean success, String message) {
        String color = success ? "#52c41a" : "#ff4d4f";
        String icon = success ? "✅" : "❌";
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Microsoft 授权</title>"
                + "<style>body{font-family:sans-serif;display:flex;align-items:center;justify-content:center;"
                + "height:100vh;margin:0;background:#f5f5f5;}"
                + ".card{background:#fff;padding:32px 40px;border-radius:8px;text-align:center;"
                + "box-shadow:0 2px 8px rgba(0,0,0,.1);}"
                + ".icon{font-size:48px;margin-bottom:16px;}"
                + ".msg{color:" + color + ";font-size:16px;}</style></head>"
                + "<body><div class='card'>"
                + "<div class='icon'>" + icon + "</div>"
                + "<div class='msg'>" + message + "</div>"
                + "</div>"
                + "<script>"
                + "try{ window.opener && window.opener.postMessage("
                + "{type:'ms-oauth-callback',success:" + success + "},'*'); }catch(e){}"
                + (success ? "setTimeout(function(){window.close();},1500);" : "")
                + "</script></body></html>";
    }
}
