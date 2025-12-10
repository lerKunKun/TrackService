package com.logistics.track17.service;

import com.logistics.track17.config.DingTalkConfig;
import com.logistics.track17.dto.DingTalkUserInfo;
import com.logistics.track17.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 钉钉OAuth服务
 */
@Service
@Slf4j
public class DingTalkService {

    private final DingTalkConfig dingTalkConfig;
    private final RestTemplate restTemplate;
    private final AllowedCorpIdService allowedCorpIdService;

    public DingTalkService(DingTalkConfig dingTalkConfig, RestTemplate restTemplate,
            AllowedCorpIdService allowedCorpIdService) {
        this.dingTalkConfig = dingTalkConfig;
        this.restTemplate = restTemplate;
        this.allowedCorpIdService = allowedCorpIdService;
    }

    /**
     * 使用authCode换取accessToken
     *
     * @param code 授权码
     * @return accessToken
     */
    public String getAccessToken(String code) {
        log.info("Getting access token with code: {}", code);

        try {
            String url = dingTalkConfig.getTokenUrl();

            Map<String, Object> request = new HashMap<>();
            request.put("clientId", dingTalkConfig.getAppKey());
            request.put("clientSecret", dingTalkConfig.getAppSecret());
            request.put("code", code);
            request.put("grantType", "authorization_code");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new BusinessException("获取AccessToken失败");
            }

            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("accessToken")) {
                throw new BusinessException("AccessToken响应格式错误");
            }

            String accessToken = (String) body.get("accessToken");
            log.info("Successfully obtained access token");
            return accessToken;

        } catch (Exception e) {
            log.error("Failed to get access token", e);
            throw BusinessException.of("获取钉钉AccessToken失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户信息
     *
     * @param accessToken 访问令牌
     * @return 用户信息
     */
    public DingTalkUserInfo getUserInfo(String accessToken) {
        log.info("Getting user info with access token");

        try {
            String url = dingTalkConfig.getUserInfoUrl();

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-acs-dingtalk-access-token", accessToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<DingTalkUserInfo> response = restTemplate.exchange(url, HttpMethod.GET, entity,
                    DingTalkUserInfo.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new BusinessException("获取用户信息失败");
            }

            DingTalkUserInfo userInfo = response.getBody();
            if (userInfo == null) {
                throw new BusinessException("用户信息响应为空");
            }

            log.info("Successfully obtained user info for unionId: {}", userInfo.getUnionId());
            return userInfo;

        } catch (Exception e) {
            log.error("Failed to get user info", e);
            throw BusinessException.of("获取钉钉用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 验证用户企业CorpId是否允许登录
     * 检查CorpId是否在allowed_corp_ids白名单中
     *
     * @param userCorpId 用户所属企业ID
     * @return 是否允许登录
     */
    public boolean validateCorpId(String userCorpId) {
        boolean isAllowed = allowedCorpIdService.isCorpIdAllowed(userCorpId);
        log.info("CorpId validation: corpId={}, allowed={}", userCorpId, isAllowed);
        return isAllowed;
    }
}
