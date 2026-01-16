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

    @Autowired
    private RoleService roleService;

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
     * 同步用户（优化版）
     */
    @Transactional
    public int syncUsers() {
        log.info("开始同步钉钉用户...");

        // 1. 从钉钉获取所有部门的用户
        List<DingtalkUserDTO> allDingUsers = getAllDingtalkUsers();
        log.info("从钉钉获取到 {} 个用户", allDingUsers.size());

        // 2. 获取数据库中所有钉钉用户
        List<User> allDbUsers = userMapper.selectAllDingtalkUsers();
        Map<String, User> dbUserMapByUnionId = allDbUsers.stream()
                .filter(u -> u.getDingUnionId() != null)
                .collect(Collectors.toMap(User::getDingUnionId, u -> u, (u1, u2) -> u1));
        Map<String, User> dbUserMapByUserId = allDbUsers.stream()
                .filter(u -> u.getDingUserId() != null)
                .collect(Collectors.toMap(User::getDingUserId, u -> u, (u1, u2) -> u1));


        List<User> usersToInsert = new ArrayList<>();
        List<User> usersToUpdate = new ArrayList<>();

        // 3. 遍历钉钉用户，进行比较和处理
        for (DingtalkUserDTO dingUser : allDingUsers) {
            User existingUser = dbUserMapByUnionId.get(dingUser.getUnionid());
            if (existingUser == null) {
                existingUser = dbUserMapByUserId.get(dingUser.getUserid());
            }

            if (existingUser == null) {
                // 新用户
                usersToInsert.add(createNewUserFromDingtalk(dingUser));
            } else {
                // 已存在用户，检查是否需要更新
                if (isUserNeedsUpdate(existingUser, dingUser)) {
                    updateUserFromDingtalk(existingUser, dingUser);
                    usersToUpdate.add(existingUser);
                }
            }
        }

        int processedCount = 0;

        // 4. 批量插入
        if (!usersToInsert.isEmpty()) {
            userMapper.batchInsert(usersToInsert);
            processedCount += usersToInsert.size();
            log.info("批量插入 {} 个新用户", usersToInsert.size());
        }

        // 5. 批量更新
        if (!usersToUpdate.isEmpty()) {
            userMapper.batchUpdate(usersToUpdate);
            processedCount += usersToUpdate.size();
            log.info("批量更新 {} 个用户", usersToUpdate.size());
        }

        log.info("用户同步完成，共处理 {} 个用户", processedCount);
        return processedCount;
    }

    private List<DingtalkUserDTO> getAllDingtalkUsers() {
        List<DingtalkDeptMapping> deptMappings = dingtalkDeptMappingMapper.selectAll();
        Set<String> processedUserIds = new HashSet<>();
        List<DingtalkUserDTO> allDingUsers = new ArrayList<>();

        for (DingtalkDeptMapping mapping : deptMappings) {
            try {
                List<DingtalkUserDTO> dingUsers = dingtalkApiService.getDepartmentUsers(mapping.getDingtalkDeptId());
                for (DingtalkUserDTO dingUser : dingUsers) {
                    if (processedUserIds.add(dingUser.getUserid())) {
                        allDingUsers.add(dingUser);
                    }
                }
            } catch (Exception e) {
                log.error("获取部门用户失败: {}", mapping.getDingtalkDeptName(), e);
            }
        }
        return allDingUsers;
    }

    private User createNewUserFromDingtalk(DingtalkUserDTO dingUser) {
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
        newUser.setStatus(dingUser.getActive() != null && dingUser.getActive() ? 1 : 0);
        newUser.setSyncEnabled(1);
        newUser.setRole("USER");
        newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        return newUser;
    }

    private void updateUserFromDingtalk(User existingUser, DingtalkUserDTO dingUser) {
        Integer oldStatus = existingUser.getStatus();
        Integer newStatus = dingUser.getActive() != null && dingUser.getActive() ? 1 : 0;

        existingUser.setDingUserId(dingUser.getUserid());
        existingUser.setDingUnionId(dingUser.getUnionid());
        existingUser.setRealName(dingUser.getName());
        existingUser.setPhone(dingUser.getMobile());
        existingUser.setEmail(dingUser.getEmail());
        existingUser.setTitle(dingUser.getTitle());
        existingUser.setJobNumber(dingUser.getJobNumber());
        existingUser.setAvatar(dingUser.getAvatar());
        existingUser.setStatus(newStatus);

        if (!oldStatus.equals(newStatus)) {
            log.info("用户状态变更: {} ({} -> {})", existingUser.getUsername(),
                    oldStatus == 1 ? "启用" : "禁用",
                    newStatus == 1 ? "启用" : "禁用");
        }
    }

    private boolean isUserNeedsUpdate(User user, DingtalkUserDTO dingUser) {
        // 比较关键字段
        return !Objects.equals(user.getRealName(), dingUser.getName()) ||
                !Objects.equals(user.getPhone(), dingUser.getMobile()) ||
                !Objects.equals(user.getEmail(), dingUser.getEmail()) ||
                !Objects.equals(user.getTitle(), dingUser.getTitle()) ||
                !Objects.equals(user.getJobNumber(), dingUser.getJobNumber()) ||
                !Objects.equals(user.getAvatar(), dingUser.getAvatar()) ||
                !Objects.equals(user.getStatus(), dingUser.getActive() != null && dingUser.getActive() ? 1 : 0);
    }

    /**
     * 应用角色映射规则（优化版）
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

        // 2. 获取所有需要同步角色的用户
        List<User> syncEnabledUsers = userMapper.selectAllSyncEnabledUsers();
        log.info("共有 {} 个用户启用了自动同步", syncEnabledUsers.size());

        if (syncEnabledUsers.isEmpty()) {
            return 0;
        }

        // 3. 为每个用户匹配角色，并收集分配任务
        List<RoleService.UserRoleAssignment> assignments = new ArrayList<>();
        for (User user : syncEnabledUsers) {
            List<Long> matchedRoleIds = matchRolesForUser(user, rules);
            if (!matchedRoleIds.isEmpty()) {
                assignments.add(new RoleService.UserRoleAssignment(user.getId(), matchedRoleIds));
            }
        }

        // 4. 批量分配角色
        if (!assignments.isEmpty()) {
            roleService.batchAssignRolesToUsers(assignments);
            log.info("批量为 {} 个用户分配了角色", assignments.size());
        }

        log.info("角色映射完成");
        return assignments.size();
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
