package com.logistics.track17.service;

import com.logistics.track17.entity.Carrier;
import com.logistics.track17.mapper.CarrierMapper;
import lombok.extern.slf4j.Slf4j;
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
     * 根据17Track carrier ID获取承运商
     */
    public Carrier getByCarrierId(Integer carrierId) {
        return carrierMapper.selectByCarrierId(carrierId);
    }

    /**
     * 根据系统carrier code获取承运商
     */
    public Carrier getByCarrierCode(String carrierCode) {
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
     * 批量导入承运商
     */
    public int batchImport(List<Carrier> carriers) {
        if (carriers == null || carriers.isEmpty()) {
            return 0;
        }
        return carrierMapper.batchInsert(carriers);
    }
}
