package com.logistics.track17.service;

import com.alibaba.fastjson.JSON;
import com.logistics.track17.config.Track17Config;
import com.logistics.track17.dto.Track17RegisterRequest;
import com.logistics.track17.dto.Track17Response;
import com.logistics.track17.dto.Track17RegisterResponse;
import com.logistics.track17.dto.Track17QueryResponse;
import com.logistics.track17.dto.Track17V2Response;
import com.logistics.track17.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 17Track API服务
 * API文档: https://api.17track.net/en/doc
 */
@Slf4j
@Service
public class Track17Service {

    private final Track17Config config;
    private final OkHttpClient httpClient;

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    public Track17Service(Track17Config config) {
        this.config = config;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 注册单个运单到17Track
     */
    public Track17RegisterResponse registerTracking(String trackingNumber, String carrierCode) {
        return registerTrackingBatch(Collections.singletonList(
                new Track17RegisterRequest(trackingNumber, carrierCode)
        ));
    }

    /**
     * 批量注册运单到17Track（最多40个）
     * API: POST /track/v2/register
     */
    public Track17RegisterResponse registerTrackingBatch(List<Track17RegisterRequest> requests) {
        log.info("Registering {} tracking numbers to 17Track", requests.size());

        if (requests.size() > 40) {
            log.warn("Maximum 40 tracking numbers per batch, current: {}", requests.size());
            throw BusinessException.of("单次最多注册40个运单");
        }

        try {
            String url = config.getUrl() + config.getRegisterEndpoint();
            String requestBody = JSON.toJSONString(requests);

            log.debug("17Track register request: {}", requestBody);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("17token", config.getToken())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, JSON_MEDIA_TYPE))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                log.debug("17Track register response [{}]: {}", response.code(), responseBody);

                if (!response.isSuccessful()) {

                    throw BusinessException.of("17Track API请求失败，HTTP状态码: " + response.code());
                }

                Track17RegisterResponse apiResponse = JSON.parseObject(responseBody, Track17RegisterResponse.class);

                if (apiResponse.getCode() != null && apiResponse.getCode() == 0) {
                    log.info("Successfully registered {} tracking numbers", requests.size());
                    return apiResponse;
                } else {
                    log.error("17Track register returned error code {}: {}",
                            apiResponse.getCode(), apiResponse.getMsg());
                    throw BusinessException.of("17Track注册失败: " + apiResponse.getMsg());
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to register tracking numbers to 17Track", e);
            throw BusinessException.of("注册运单到17Track失败: " + e.getMessage());
        }
    }

    /**
     * 查询运单信息
     * API: POST /track/v2/gettrackinfo
     */
    public Track17QueryResponse queryTracking(String trackingNumber, String carrierCode) {
        log.info("Querying tracking info for: {} (carrier: {})", trackingNumber, carrierCode);

        Track17RegisterRequest queryRequest = new Track17RegisterRequest(trackingNumber, carrierCode);
        return queryTrackingBatch(Collections.singletonList(queryRequest));
    }

    /**
     * 批量查询运单信息（最多40个）
     * API: POST /track/v2/gettrackinfo
     */
    public Track17QueryResponse queryTrackingBatch(List<Track17RegisterRequest> requests) {
        log.info("Querying {} tracking numbers from 17Track", requests.size());

        if (requests.size() > 40) {
            log.warn("Maximum 40 tracking numbers per batch, current: {}", requests.size());
            throw BusinessException.of("单次最多查询40个运单");
        }

        try {
            String url = config.getUrl() + config.getQueryEndpoint();
            String requestBody = JSON.toJSONString(requests);

            log.debug("17Track query request: {}", requestBody);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("17token", config.getToken())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, JSON_MEDIA_TYPE))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                log.debug("17Track query response [{}]: {}", response.code(), responseBody);

                if (!response.isSuccessful()) {
                    throw BusinessException.of("17Track API请求失败，HTTP状态码: " + response.code());
                }

                Track17QueryResponse apiResponse = JSON.parseObject(responseBody, Track17QueryResponse.class);

                if (apiResponse.getCode() != null && apiResponse.getCode() == 0) {
                    log.info("Successfully queried tracking info");
                    return apiResponse;
                } else {
                    log.error("17Track query returned error code {}: {}",
                            apiResponse.getCode(), apiResponse.getMsg());
                    throw BusinessException.of("17Track查询失败: " + apiResponse.getMsg());
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to query tracking info from 17Track", e);
            throw BusinessException.of("查询物流信息失败: " + e.getMessage());
        }
    }

    /**
     * 查询运单信息（V2格式，返回详细的track_info）
     * API: POST /track/v2.2/gettrackinfo
     */
    public Track17V2Response queryTrackingV2(String trackingNumber, String carrierCode) {
        log.info("Querying V2 tracking info for: {} (carrier: {})", trackingNumber, carrierCode);

        Track17RegisterRequest queryRequest = new Track17RegisterRequest(trackingNumber, carrierCode);
        return queryTrackingV2Batch(Collections.singletonList(queryRequest));
    }

    /**
     * 批量查询运单信息V2（最多40个）
     * API: POST /track/v2.2/gettrackinfo
     */
    public Track17V2Response queryTrackingV2Batch(List<Track17RegisterRequest> requests) {
        log.info("Querying V2 format for {} tracking numbers from 17Track", requests.size());

        if (requests.size() > 40) {
            log.warn("Maximum 40 tracking numbers per batch, current: {}", requests.size());
            throw BusinessException.of("单次最多查询40个运单");
        }

        try {
            // 使用配置的query endpoint（v2.4格式也支持track_info）
            String url = config.getUrl() + config.getQueryEndpoint();
            String requestBody = JSON.toJSONString(requests);

            log.debug("17Track V2 query request to {}: {}", url, requestBody);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("17token", config.getToken())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, JSON_MEDIA_TYPE))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                log.debug("17Track V2 query response [{}]: {}", response.code(),
                         responseBody.length() > 500 ? responseBody.substring(0, 500) + "..." : responseBody);

                if (!response.isSuccessful()) {
                    throw BusinessException.of("17Track API请求失败，HTTP状态码: " + response.code());
                }

                Track17V2Response apiResponse = JSON.parseObject(responseBody, Track17V2Response.class);

                if (apiResponse.getCode() != null && apiResponse.getCode() == 0) {
                    log.info("Successfully queried V2 tracking info");
                    return apiResponse;
                } else {
                    log.error("17Track V2 query returned error code {}: {}",
                            apiResponse.getCode(), apiResponse.getMsg());
                    throw BusinessException.of("17Track查询失败: " + apiResponse.getMsg());
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to query V2 tracking info from 17Track", e);
            throw BusinessException.of("查询物流信息失败: " + e.getMessage());
        }
    }

    /**
     * 验证Webhook签名
     */
    public boolean validateWebhookSignature(String signature, String payload) {
        // TODO: 实现17Track的Webhook签名验证逻辑
        // 17Track使用的是HMAC-SHA256签名
        return true;
    }
}
