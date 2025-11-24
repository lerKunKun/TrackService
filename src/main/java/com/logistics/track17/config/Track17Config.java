package com.logistics.track17.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 17Track API配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "track17.api")
public class Track17Config {
    private String url;
    private String token;
    private String registerEndpoint;
    private String queryEndpoint;
    private Integer timeout;
}
