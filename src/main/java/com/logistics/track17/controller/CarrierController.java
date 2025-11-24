package com.logistics.track17.controller;

import com.logistics.track17.dto.Result;
import com.logistics.track17.entity.Carrier;
import com.logistics.track17.service.CarrierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 承运商控制器
 */
@Slf4j
@RestController
@RequestMapping("/carriers")
public class CarrierController {

    private final CarrierService carrierService;

    public CarrierController(CarrierService carrierService) {
        this.carrierService = carrierService;
    }

    /**
     * 获取所有启用的承运商
     */
    @GetMapping
    public Result<List<Carrier>> getAllActive() {
        List<Carrier> carriers = carrierService.getAllActive();
        return Result.success(carriers);
    }

    /**
     * 获取所有承运商（包括未启用）
     */
    @GetMapping("/all")
    public Result<List<Carrier>> getAll() {
        List<Carrier> carriers = carrierService.getAll();
        return Result.success(carriers);
    }
}
