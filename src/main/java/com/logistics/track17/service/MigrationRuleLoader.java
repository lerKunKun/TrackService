package com.logistics.track17.service;

import com.google.gson.Gson;
import com.logistics.track17.dto.*;
import com.logistics.track17.entity.ThemeMigrationRule;
import com.logistics.track17.mapper.ThemeMigrationRuleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 迁移规则加载器
 * 从数据库加载规则并转换为可执行格式
 */
@Slf4j
@Service
public class MigrationRuleLoader {

    @Autowired
    private ThemeMigrationRuleMapper ruleMapper;

    private final Gson gson = new Gson();

    /**
     * 加载指定版本迁移的所有规则
     */
    public ExecutableMigrationRules loadRules(
            String themeName,
            String fromVersion,
            String toVersion) {

        log.info("Loading migration rules for {} {} -> {}", themeName, fromVersion, toVersion);

        List<ThemeMigrationRule> rules = ruleMapper.selectByVersions(themeName, fromVersion, toVersion);

        if (rules.isEmpty()) {
            log.warn("No migration rules found for {} {} -> {}", themeName, fromVersion, toVersion);
            return ExecutableMigrationRules.builder().build();
        }

        return convertToExecutableRules(rules);
    }

    /**
     * 转换数据库规则为可执行规则
     */
    private ExecutableMigrationRules convertToExecutableRules(List<ThemeMigrationRule> rules) {
        ExecutableMigrationRules executableRules = ExecutableMigrationRules.builder().build();

        for (ThemeMigrationRule rule : rules) {
            switch (rule.getRuleType()) {
                case "SECTION_RENAME":
                    applySectionRenameRule(rule, executableRules);
                    break;
                case "FIELD_MAPPING":
                    applyFieldMappingRule(rule, executableRules);
                    break;
                case "DEFAULT_VALUE":
                    applyDefaultValueRule(rule, executableRules);
                    break;
                default:
                    log.warn("Unknown rule type: {}", rule.getRuleType());
            }
        }

        log.info("Loaded rules: {}", executableRules.getStatistics());
        return executableRules;
    }

    /**
     * 应用 Section 重命名规则
     */
    private void applySectionRenameRule(ThemeMigrationRule rule, ExecutableMigrationRules executableRules) {
        try {
            SectionRenameRule renameRule = gson.fromJson(rule.getRuleJson(), SectionRenameRule.class);
            executableRules.addSectionRename(renameRule.getOldName(), renameRule.getNewName());
            log.debug("Added section rename: {} -> {}", renameRule.getOldName(), renameRule.getNewName());
        } catch (Exception e) {
            log.error("Failed to parse section rename rule: {}", rule.getRuleJson(), e);
        }
    }

    /**
     * 应用字段映射规则
     */
    private void applyFieldMappingRule(ThemeMigrationRule rule, ExecutableMigrationRules executableRules) {
        try {
            FieldMappingRule mappingRule = gson.fromJson(rule.getRuleJson(), FieldMappingRule.class);
            executableRules.addFieldMapping(
                    mappingRule.getSectionName(),
                    mappingRule.getOldFieldId(),
                    mappingRule.getNewFieldId());
            log.debug("Added field mapping for {}: {} -> {}",
                    mappingRule.getSectionName(),
                    mappingRule.getOldFieldId(),
                    mappingRule.getNewFieldId());
        } catch (Exception e) {
            log.error("Failed to parse field mapping rule: {}", rule.getRuleJson(), e);
        }
    }

    /**
     * 应用默认值规则
     */
    private void applyDefaultValueRule(ThemeMigrationRule rule, ExecutableMigrationRules executableRules) {
        try {
            DefaultValueRule defaultRule = gson.fromJson(rule.getRuleJson(), DefaultValueRule.class);
            executableRules.addDefaultValue(
                    defaultRule.getSectionName(),
                    defaultRule.getFieldId(),
                    defaultRule.getDefaultValue());
            log.debug("Added default value for {}.{}: {}",
                    defaultRule.getSectionName(),
                    defaultRule.getFieldId(),
                    defaultRule.getDefaultValue());
        } catch (Exception e) {
            log.error("Failed to parse default value rule: {}", rule.getRuleJson(), e);
        }
    }

    /**
     * 检查规则是否存在
     */
    public boolean rulesExist(String themeName, String fromVersion, String toVersion) {
        List<ThemeMigrationRule> rules = ruleMapper.selectByVersions(themeName, fromVersion, toVersion);
        return !rules.isEmpty();
    }

    /**
     * 获取规则统计信息
     */
    public String getRuleStatistics(String themeName, String fromVersion, String toVersion) {
        List<ThemeMigrationRule> rules = ruleMapper.selectByVersions(themeName, fromVersion, toVersion);

        long renameCount = rules.stream().filter(r -> "SECTION_RENAME".equals(r.getRuleType())).count();
        long mappingCount = rules.stream().filter(r -> "FIELD_MAPPING".equals(r.getRuleType())).count();
        long defaultCount = rules.stream().filter(r -> "DEFAULT_VALUE".equals(r.getRuleType())).count();

        return String.format("Total: %d (Renames: %d, Mappings: %d, Defaults: %d)",
                rules.size(), renameCount, mappingCount, defaultCount);
    }
}
