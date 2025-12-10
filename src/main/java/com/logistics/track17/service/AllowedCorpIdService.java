package com.logistics.track17.service;

import com.logistics.track17.entity.AllowedCorpId;
import com.logistics.track17.mapper.AllowedCorpIdMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 允许登录的企业CorpId服务
 */
@Service
@Slf4j
public class AllowedCorpIdService {

    private final AllowedCorpIdMapper allowedCorpIdMapper;

    public AllowedCorpIdService(AllowedCorpIdMapper allowedCorpIdMapper) {
        this.allowedCorpIdMapper = allowedCorpIdMapper;
    }

    /**
     * 获取所有允许的CorpId
     */
    public List<AllowedCorpId> getAllAllowedCorpIds() {
        return allowedCorpIdMapper.selectAll();
    }

    /**
     * 检查CorpId是否允许登录
     */
    public boolean isCorpIdAllowed(String corpId) {
        boolean allowed = allowedCorpIdMapper.isCorpIdAllowed(corpId);
        log.info("Checking if corpId {} is allowed: {}", corpId, allowed);
        return allowed;
    }

    /**
     * 添加允许的CorpId
     */
    @Transactional
    public void addAllowedCorpId(AllowedCorpId allowedCorpId) {
        log.info("Adding allowed corpId: {}", allowedCorpId.getCorpId());
        allowedCorpIdMapper.insert(allowedCorpId);
    }

    /**
     * 软删除允许的CorpId
     */
    @Transactional
    public void removeAllowedCorpId(String corpId) {
        log.info("Soft deleting allowed corpId: {}", corpId);
        allowedCorpIdMapper.softDeleteByCorpId(corpId);
    }

    /**
     * 更新CorpId状态
     */
    @Transactional
    public void updateCorpIdStatus(String corpId, Integer status) {
        log.info("Updating corpId {} status to: {}", corpId, status);
        allowedCorpIdMapper.updateStatus(corpId, status);
    }
}
