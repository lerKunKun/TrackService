package com.logistics.track17.service;

import com.logistics.track17.entity.Carrier;
import com.logistics.track17.mapper.CarrierMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 承运商服务
 */
@Slf4j
@Service
public class CarrierService {

    private final CarrierMapper carrierMapper;

    public CarrierService(CarrierMapper carrierMapper) {
        this.carrierMapper = carrierMapper;
    }

    /**
     * 根据17Track carrier ID获取承运商（带缓存）
     */
    @Cacheable(value = "carrier", key = "'id:' + #carrierId", unless = "#result == null")
    public Carrier getByCarrierId(Integer carrierId) {
        log.debug("Querying carrier by ID from database: {}", carrierId);
        return carrierMapper.selectByCarrierId(carrierId);
    }

    /**
     * 根据系统carrier code获取承运商（带缓存）
     */
    @Cacheable(value = "carrier", key = "'code:' + #carrierCode", unless = "#result == null")
    public Carrier getByCarrierCode(String carrierCode) {
        log.debug("Querying carrier by code from database: {}", carrierCode);
        return carrierMapper.selectByCarrierCode(carrierCode);
    }

    /**
     * 获取所有启用的承运商
     */
    public List<Carrier> getAllActive() {
        return carrierMapper.selectAllActive();
    }

    /**
     * 获取所有承运商
     */
    public List<Carrier> getAll() {
        return carrierMapper.selectAll();
    }

    /**
     * 批量导入承运商（清除缓存）
     */
    @CacheEvict(value = "carrier", allEntries = true)
    public int batchImport(List<Carrier> carriers) {
        if (carriers == null || carriers.isEmpty()) {
            return 0;
        }
        log.info("Batch importing {} carriers, clearing cache", carriers.size());
        return carrierMapper.batchInsert(carriers);
    }
}
