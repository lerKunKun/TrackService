package com.logistics.track17.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OnlineSession {

    private String sessionId;
    private Long userId;
    private String username;
    private String ipAddress;
    private String device;
    private String browser;
    private String os;
    private LocalDateTime loginTime;
    private String loginSource;
}
