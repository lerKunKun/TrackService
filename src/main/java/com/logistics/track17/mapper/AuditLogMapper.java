package com.logistics.track17.mapper;

import com.logistics.track17.entity.AuditLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 审计日志 Mapper
 */
@Mapper
public interface AuditLogMapper {

    /**
     * 插入审计日志
     */
    void insert(AuditLogEntity auditLog);

    /**
     * 根据ID查询
     */
    AuditLogEntity selectById(Long id);

    /**
     * 分页查询审计日志
     */
    List<AuditLogEntity> selectByPage(
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("userId") Long userId,
            @Param("operation") String operation,
            @Param("module") String module);

    /**
     * 统计总数
     */
    Long countAll(
            @Param("userId") Long userId,
            @Param("operation") String operation,
            @Param("module") String module);
}
