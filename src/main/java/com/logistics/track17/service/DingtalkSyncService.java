package com.logistics.track17.service;

import com.logistics.track17.dto.DingtalkDepartmentDTO;
import com.logistics.track17.dto.DingtalkUserDTO;
import com.logistics.track17.entity.*;
import com.logistics.track17.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 钉钉同步服务
 * 负责从钉钉同步组织架构和员工信息
 */
@Service
@Slf4j
public class DingtalkSyncService {

    @Autowired
    private DingtalkApiService dingtalkApiService;

    @Autowired
    private DingtalkDeptMappingMapper dingtalkDeptMappingMapper;

    @Autowired
    private RoleMappingRuleMapper roleMappingRuleMapper;

    @Autowired
    private DingtalkSyncLogMapper dingtalkSyncLogMapper;

    @Autowired
    private UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 全量同步（部门 + 用户 + 角色映射）
     */
    @Transactional
    public DingtalkSyncLog fullSync() {
        DingtalkSyncLog syncLog = createSyncLog("FULL", "MANUAL");
        syncLog.setStartedAt(LocalDateTime.now());

        try {
            log.info("=== 开始全量同步 ===");

            // 1. 同步部门
            log.info("步骤1: 同步部门...");
            int deptCount = syncDepartments();
            log.info("部门同步完成，共处理 {} 个部门", deptCount);

            // 2. 同步用户
            log.info("步骤2: 同步用户...");
            int userCount = syncUsers();
            log.info("用户同步完成，共处理 {} 个用户", userCount);

            // 3. 应用角色映射规则
            log.info("步骤3: 应用角色映射规则...");
            int roleAssignCount = applyRoleMappingRules();
            log.info("角色分配完成，共分配 {} 个用户", roleAssignCount);

            syncLog.setStatus("SUCCESS");
            syncLog.setTotalCount(deptCount + userCount);
            syncLog.setSuccessCount(deptCount + userCount);
            syncLog.setFailedCount(0);
            log.info("=== 全量同步成功完成 ===");

        } catch (Exception e) {
            log.error("全量同步失败", e);
            syncLog.setStatus("FAILED");
            syncLog.setErrorMessage(e.getMessage());
        } finally {
            syncLog.setCompletedAt(LocalDateTime.now());
            dingtalkSyncLogMapper.insert(syncLog);
        }

        return syncLog;
    }

    /**
     * 同步部门
     */
    @Transactional
    public int syncDepartments() {
        log.info("开始同步钉钉部门...");

        // 1. 从钉钉获取所有部门（递归）
        List<DingtalkDepartmentDTO> dingDepts = dingtalkApiService.getAllDepartments();
        log.info("从钉钉获取到 {} 个部门", dingDepts.size());

        int processedCount = 0;

        // 2. 创建或更新部门映射
        for (DingtalkDepartmentDTO dingDept : dingDepts) {
            try {
                // 查找是否已存在映射
                DingtalkDeptMapping existingMapping = dingtalkDeptMappingMapper
                        .selectByDingtalkDeptId(dingDept.getDeptId());

                if (existingMapping == null) {
                    // 新建映射（这里简化处理，实际应该创建系统部门）
                    DingtalkDeptMapping newMapping = new DingtalkDeptMapping();
                    newMapping.setDingtalkDeptId(dingDept.getDeptId());
                    newMapping.setDingtalkDeptName(dingDept.getName());
                    // systemDeptId 需要在实际部门表中创建后再设置，这里暂时设置为钉钉部门ID
                    newMapping.setSystemDeptId(dingDept.getDeptId());

                    dingtalkDeptMappingMapper.insert(newMapping);
                    log.debug("创建部门映射: {} (钉钉ID: {})", dingDept.getName(), dingDept.getDeptId());
                } else {
                    // 更新部门名称
                    existingMapping.setDingtalkDeptName(dingDept.getName());
                    dingtalkDeptMappingMapper.update(existingMapping);
                    log.debug("更新部门映射: {} (钉钉ID: {})", dingDept.getName(), dingDept.getDeptId());
                }

                processedCount++;
            } catch (Exception e) {
                log.error("处理部门失败: {}", dingDept.getName(), e);
            }
        }

        log.info("部门同步完成，处理 {} 个部门", processedCount);
        return processedCount;
    }

    /**
     * 同步用户
     */
    @Transactional
    public int syncUsers() {
        log.info("开始同步钉钉用户...");

        // 1. 获取所有部门映射
        List<DingtalkDeptMapping> deptMappings = dingtalkDeptMappingMapper.selectAll();
        log.info("获取到 {} 个部门映射", deptMappings.size());

        int processedCount = 0;
        Set<String> processedUserIds = new HashSet<>(); // 避免重复处理

        // 2. 遍历每个部门，获取用户
        for (DingtalkDeptMapping mapping : deptMappings) {
            try {
                List<DingtalkUserDTO> dingUsers = dingtalkApiService
                        .getDepartmentUsers(mapping.getDingtalkDeptId());

                for (DingtalkUserDTO dingUser : dingUsers) {
                    // 去重：如果用户在多个部门，只处理一次
                    if (processedUserIds.contains(dingUser.getUserid())) {
                        continue;
                    }

                    try {
                        syncSingleUser(dingUser);
                        processedUserIds.add(dingUser.getUserid());
                        processedCount++;
                    } catch (Exception e) {
                        log.error("同步用户失败: {}", dingUser.getName(), e);
                    }
                }
            } catch (Exception e) {
                log.error("获取部门用户失败: {}", mapping.getDingtalkDeptName(), e);
            }
        }

        log.info("用户同步完成，处理 {} 个用户", processedCount);
        return processedCount;
    }

    /**
     * 同步单个用户
     */
    private void syncSingleUser(DingtalkUserDTO dingUser) {
        // 1. 查找用户（通过钉钉userid）
        User existingUser = userMapper.selectByDingUserId(dingUser.getUserid());

        if (existingUser == null) {
            // 新建用户
            User newUser = new User();
            newUser.setUsername(generateUniqueUsername(dingUser));
            newUser.setDingUserId(dingUser.getUserid());
            newUser.setDingUnionId(dingUser.getUnionid());
            newUser.setRealName(dingUser.getName());
            newUser.setPhone(dingUser.getMobile());
            newUser.setEmail(dingUser.getEmail());
            newUser.setTitle(dingUser.getTitle());
            newUser.setJobNumber(dingUser.getJobNumber());
            newUser.setAvatar(dingUser.getAvatar());
            newUser.setLoginSource("DINGTALK");

            // 根据钉钉active状态设置用户状态（active=true启用，active=false禁用）
            newUser.setStatus(dingUser.getActive() != null && dingUser.getActive() ? 1 : 0);
            newUser.setSyncEnabled(1); // 默认启用自动同步
            newUser.setRole("USER"); // 默认角色
            newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

            userMapper.insert(newUser);
            log.debug("创建新用户: {} (钉钉ID: {}), 状态: {}",
                    dingUser.getName(),
                    dingUser.getUserid(),
                    newUser.getStatus() == 1 ? "启用" : "禁用");

        } else {
            // 更新用户信息
            Integer oldStatus = existingUser.getStatus();
            Integer newStatus = dingUser.getActive() != null && dingUser.getActive() ? 1 : 0;

            existingUser.setRealName(dingUser.getName());
            existingUser.setPhone(dingUser.getMobile());
            existingUser.setEmail(dingUser.getEmail());
            existingUser.setTitle(dingUser.getTitle());
            existingUser.setJobNumber(dingUser.getJobNumber());
            existingUser.setAvatar(dingUser.getAvatar());
            existingUser.setStatus(newStatus);

            userMapper.update(existingUser);

            // 如果状态发生变化，记录详细日志
            if (!oldStatus.equals(newStatus)) {
                if (newStatus == 0) {
                    log.info("员工离职 - 禁用用户: {} (钉钉ID: {})", dingUser.getName(), dingUser.getUserid());
                } else {
                    log.info("员工复职 - 启用用户: {} (钉钉ID: {})", dingUser.getName(), dingUser.getUserid());
                }
            } else {
                log.debug("更新用户: {} (钉钉ID: {})", dingUser.getName(), dingUser.getUserid());
            }
        }
    }

    /**
     * 应用角色映射规则
     * 只处理 sync_enabled=1 的用户
     */
    @Transactional
    public int applyRoleMappingRules() {
        log.info("开始应用角色映射规则...");

        // 1. 获取所有启用的映射规则
        List<RoleMappingRule> rules = roleMappingRuleMapper.selectAllEnabled();
        if (rules.isEmpty()) {
            log.warn("没有启用的角色映射规则");
            return 0;
        }
        log.info("获取到 {} 条启用的映射规则", rules.size());

        // 2. 获取所有启用自动同步的用户
        List<User> allUsers = userMapper.selectAll();
        List<User> syncEnabledUsers = allUsers.stream()
                .filter(user -> user.getSyncEnabled() != null && user.getSyncEnabled() == 1)
                .filter(user -> user.getDeletedAt() == null)
                .collect(Collectors.toList());

        log.info("共有 {} 个用户启用了自动同步", syncEnabledUsers.size());

        int assignedCount = 0;

        // 3. 为每个用户匹配角色
        for (User user : syncEnabledUsers) {
            try {
                List<Long> matchedRoleIds = matchRolesForUser(user, rules);

                if (!matchedRoleIds.isEmpty()) {
                    // TODO: 这里需要调用 UserService.assignRolesToUser()
                    // 暂时只记录日志
                    log.debug("用户 {} 匹配到 {} 个角色", user.getUsername(), matchedRoleIds.size());
                    assignedCount++;
                }
            } catch (Exception e) {
                log.error("为用户分配角色失败: {}", user.getUsername(), e);
            }
        }

        log.info("角色映射完成，共处理 {} 个用户", assignedCount);
        return assignedCount;
    }

    /**
     * 为用户匹配角色
     */
    private List<Long> matchRolesForUser(User user, List<RoleMappingRule> rules) {
        return rules.stream()
                .filter(rule -> isRuleMatched(user, rule))
                .sorted(Comparator.comparing(RoleMappingRule::getPriority).reversed())
                .map(RoleMappingRule::getSystemRoleId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 判断规则是否匹配
     */
    private boolean isRuleMatched(User user, RoleMappingRule rule) {
        switch (rule.getRuleType()) {
            case "DEPT":
                // 按部门匹配（需要部门关联表支持）
                // TODO: 实现部门匹配逻辑
                return false;

            case "TITLE":
                // 按职位匹配
                if (user.getTitle() == null || rule.getDingtalkTitle() == null) {
                    return false;
                }
                return user.getTitle().contains(rule.getDingtalkTitle());

            case "DEPT_TITLE":
                // 部门+职位匹配
                // TODO: 实现复合匹配逻辑
                return false;

            default:
                return false;
        }
    }

    /**
     * 生成唯一的用户名
     * 注意：这个方法只用于新用户创建，不用于判断用户是否存在
     */
    private String generateUniqueUsername(DingtalkUserDTO dingUser) {
        // 优先使用钉钉userid作为username
        String baseUsername = dingUser.getUserid();

        // 检查username是否已被占用（与当前ding_userid不同的用户）
        User existingUser = userMapper.selectByUsername(baseUsername);

        // 如果username未被占用，或者占用者就是当前用户，则直接返回
        if (existingUser == null || baseUsername.equals(existingUser.getDingUserId())) {
            return baseUsername;
        }

        // 如果被其他用户占用，添加后缀（理论上不应该发生）
        log.warn("用户名 {} 已被占用，生成新的用户名", baseUsername);
        return baseUsername + "_" + dingUser.getName();
    }

    /**
     * 创建同步日志
     */
    private DingtalkSyncLog createSyncLog(String syncType, String syncMode) {
        DingtalkSyncLog log = new DingtalkSyncLog();
        log.setSyncType(syncType);
        log.setSyncMode(syncMode);
        log.setStatus("RUNNING");
        log.setTotalCount(0);
        log.setSuccessCount(0);
        log.setFailedCount(0);
        return log;
    }
}
