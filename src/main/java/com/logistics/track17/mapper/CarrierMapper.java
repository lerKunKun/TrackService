package com.logistics.track17.mapper;

import com.logistics.track17.entity.Carrier;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 承运商Mapper
 */
@Mapper
public interface CarrierMapper {

    /**
     * 根据17Track carrier ID查询
     */
    Carrier selectByCarrierId(@Param("carrierId") Integer carrierId);

    /**
     * 根据系统carrier code查询
     */
    Carrier selectByCarrierCode(@Param("carrierCode") String carrierCode);

    /**
     * 查询所有启用的承运商
     */
    List<Carrier> selectAllActive();

    /**
     * 查询所有承运商
     */
    List<Carrier> selectAll();

    /**
     * 插入承运商
     */
    int insert(Carrier carrier);

    /**
     * 批量插入
     */
    int batchInsert(@Param("carriers") List<Carrier> carriers);

    /**
     * 更新承运商
     */
    int update(Carrier carrier);

    /**
     * 删除承运商
     */
    int deleteById(@Param("id") Long id);
}
