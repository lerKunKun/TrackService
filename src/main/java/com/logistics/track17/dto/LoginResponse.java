package com.logistics.track17.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String username;
    private String realName; // 真实姓名（用于显示）
    private String avatar; // 用户头像URL
    private Long expiresIn; // 过期时间（毫秒）
}
