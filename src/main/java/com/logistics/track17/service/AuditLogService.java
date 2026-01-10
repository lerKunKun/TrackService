package com.logistics.track17.service;

import com.logistics.track17.entity.AuditLogEntity;
import com.logistics.track17.mapper.AuditLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 审计日志服务
 */
@Service
@Slf4j
public class AuditLogService {

    @Autowired
    private AuditLogMapper auditLogMapper;

    /**
     * 异步保存审计日志
     * 使用异步方式避免影响主业务性能
     */
    @Async
    public void saveAsync(AuditLogEntity auditLog) {
        try {
            auditLogMapper.insert(auditLog);
            log.debug("Audit log saved: operation={}, user={}, result={}",
                    auditLog.getOperation(), auditLog.getUsername(), auditLog.getResult());
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage(), e);
            // 记录日志失败不应影响主业务，仅记录错误
        }
    }

    /**
     * 同步保存审计日志
     * 用于关键操作，确保日志不丢失
     */
    public void saveSync(AuditLogEntity auditLog) {
        auditLogMapper.insert(auditLog);
        log.debug("Audit log saved (sync): operation={}, user={}, result={}",
                auditLog.getOperation(), auditLog.getUsername(), auditLog.getResult());
    }

    /**
     * 查询审计日志列表（分页）
     */
    public List<AuditLogEntity> getAuditLogs(int page, int size, Long userId, String operation, String module) {
        int offset = (page - 1) * size;
        return auditLogMapper.selectByPage(offset, size, userId, operation, module);
    }

    /**
     * 统计审计日志总数
     */
    public Long countAuditLogs(Long userId, String operation, String module) {
        return auditLogMapper.countAll(userId, operation, module);
    }

    /**
     * 根据ID查询审计日志
     */
    public AuditLogEntity getById(Long id) {
        return auditLogMapper.selectById(id);
    }
}
