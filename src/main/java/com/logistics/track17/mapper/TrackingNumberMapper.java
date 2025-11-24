package com.logistics.track17.mapper;

import com.logistics.track17.entity.TrackingNumber;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 运单Mapper
 */
public interface TrackingNumberMapper {

    /**
     * 插入运单
     */
    int insert(TrackingNumber trackingNumber);

    /**
     * 批量插入运单
     */
    int insertBatch(@Param("list") List<TrackingNumber> trackingNumbers);

    /**
     * 根据ID查询运单
     */
    TrackingNumber selectById(@Param("id") Long id);

    /**
     * 根据运单号查询
     */
    TrackingNumber selectByTrackingNumber(@Param("trackingNumber") String trackingNumber);

    /**
     * 批量查询运单号（检查是否存在）
     */
    List<TrackingNumber> selectByTrackingNumbers(@Param("trackingNumbers") List<String> trackingNumbers);

    /**
     * 查询运单列表
     */
    List<TrackingNumber> selectList(@Param("keyword") String keyword,
                                    @Param("shopId") Long shopId,
                                    @Param("status") String status,
                                    @Param("carrierCode") String carrierCode,
                                    @Param("startDate") String startDate,
                                    @Param("endDate") String endDate,
                                    @Param("offset") Integer offset,
                                    @Param("pageSize") Integer pageSize);

    /**
     * 统计运单数量
     */
    Long count(@Param("keyword") String keyword,
               @Param("shopId") Long shopId,
               @Param("status") String status,
               @Param("carrierCode") String carrierCode,
               @Param("startDate") String startDate,
               @Param("endDate") String endDate);

    /**
     * 更新运单
     */
    int update(TrackingNumber trackingNumber);

    /**
     * 删除运单
     */
    int deleteById(@Param("id") Long id);

    /**
     * 批量删除运单
     */
    int deleteBatch(@Param("ids") List<Long> ids);

    /**
     * 获取所有已使用的承运商代码
     */
    List<String> selectDistinctCarriers();
}
