package com.logistics.track17.controller;

import com.logistics.track17.dto.Result;
import com.logistics.track17.dto.Track17QueryResponse;
import com.logistics.track17.dto.Track17RegisterRequest;
import com.logistics.track17.dto.Track17RegisterResponse;
import com.logistics.track17.service.Track17Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

/**
 * 17Track测试控制器
 * 用于测试17Track API集成
 */
@Slf4j
@RestController
@RequestMapping("/test/17track")
public class Track17TestController {

    private final Track17Service track17Service;

    public Track17TestController(Track17Service track17Service) {
        this.track17Service = track17Service;
    }

    /**
     * 测试注册运单
     * GET /api/v1/test/17track/register?number=xxx&carrier=ups
     */
    @GetMapping("/register")
    public Result<Track17RegisterResponse> testRegister(
            @RequestParam String number,
            @RequestParam String carrier) {
        log.info("Testing 17Track register API: number={}, carrier={}", number, carrier);

        Track17RegisterResponse response = track17Service.registerTracking(number, carrier);

        return Result.success("注册成功", response);
    }

    /**
     * 测试查询运单
     * GET /api/v1/test/17track/query?number=xxx&carrier=ups
     */
    @GetMapping("/query")
    public Result<Track17QueryResponse> testQuery(
            @RequestParam String number,
            @RequestParam String carrier) {
        log.info("Testing 17Track query API: number={}, carrier={}", number, carrier);

        Track17QueryResponse response = track17Service.queryTracking(number, carrier);

        return Result.success("查询成功", response);
    }

    /**
     * 测试批量注册
     * POST /api/v1/test/17track/batch-register
     * Body: [{"number":"xxx","carrier":"ups"}]
     */
    @PostMapping("/batch-register")
    public Result<Track17RegisterResponse> testBatchRegister(@RequestBody java.util.List<Track17RegisterRequest> requests) {
        log.info("Testing 17Track batch register API: {} items", requests.size());

        Track17RegisterResponse response = track17Service.registerTrackingBatch(requests);

        return Result.success("批量注册成功", response);
    }

    /**
     * 测试批量查询
     * POST /api/v1/test/17track/batch-query
     * Body: [{"number":"xxx","carrier":"ups"}]
     */
    @PostMapping("/batch-query")
    public Result<Track17QueryResponse> testBatchQuery(@RequestBody java.util.List<Track17RegisterRequest> requests) {
        log.info("Testing 17Track batch query API: {} items", requests.size());

        Track17QueryResponse response = track17Service.queryTrackingBatch(requests);

        return Result.success("批量查询成功", response);
    }
}
