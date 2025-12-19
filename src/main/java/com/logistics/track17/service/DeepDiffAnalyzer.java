package com.logistics.track17.service;

import com.google.gson.Gson;
import com.logistics.track17.dto.*;
import com.logistics.track17.entity.LiquidSchemaCache;
import com.logistics.track17.entity.ThemeMigrationRule;
import com.logistics.track17.mapper.LiquidSchemaCacheMapper;
import com.logistics.track17.mapper.ThemeMigrationRuleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 深度Diff分析器
 * 分析两个主题版本之间的schema变化，生成迁移规则
 */
@Slf4j
@Service
public class DeepDiffAnalyzer {

    @Autowired
    private LiquidSchemaParser schemaParser;

    @Autowired
    private LiquidSchemaCacheMapper schemaCacheMapper;

    @Autowired
    private ThemeMigrationRuleMapper ruleMapper;

    private final Gson gson = new Gson();

    /**
     * 执行深度分析并保存规则
     * 
     * @param themeName    主题名称
     * @param fromVersion  源版本
     * @param toVersion    目标版本
     * @param oldThemePath 旧版本主题路径
     * @param newThemePath 新版本主题路径
     * @param createdBy    创建人
     */
    @Transactional(rollbackFor = Exception.class)
    public void analyzeAndSaveRules(
            String themeName,
            String fromVersion,
            String toVersion,
            Path oldThemePath,
            Path newThemePath,
            String createdBy) throws Exception {

        log.info("Starting deep diff analysis: {} {} -> {}", themeName, fromVersion, toVersion);

        // 1. 提取并缓存schema
        Map<String, SectionSchema> oldSchemas = extractAndCacheSchemas(
                themeName, fromVersion, oldThemePath);
        Map<String, SectionSchema> newSchemas = extractAndCacheSchemas(
                themeName, toVersion, newThemePath);

        log.info("Extracted schemas - Old: {}, New: {}", oldSchemas.size(), newSchemas.size());

        // 2. 分析Section重命名
        List<SectionRenameRule> renameRules = analyzeSectionRenames(oldSchemas, newSchemas);
        log.info("Generated {} section rename rules", renameRules.size());

        // 3. 分析字段映射
        List<FieldMappingRule> fieldRules = analyzeFieldMappings(oldSchemas, newSchemas, renameRules);
        log.info("Generated {} field mapping rules", fieldRules.size());

        // 4. 分析默认值
        List<DefaultValueRule> defaultRules = analyzeDefaultValues(oldSchemas, newSchemas);
        log.info("Generated {} default value rules", defaultRules.size());

        // 5. 保存规则到数据库
        saveRules(themeName, fromVersion, toVersion, "SECTION_RENAME", renameRules, createdBy);
        saveRules(themeName, fromVersion, toVersion, "FIELD_MAPPING", fieldRules, createdBy);
        saveRules(themeName, fromVersion, toVersion, "DEFAULT_VALUE", defaultRules, createdBy);

        log.info("Deep diff analysis completed. Total rules: {}",
                renameRules.size() + fieldRules.size() + defaultRules.size());
    }

    /**
     * 提取所有schemas并缓存到数据库
     */
    private Map<String, SectionSchema> extractAndCacheSchemas(
            String themeName,
            String version,
            Path themePath) throws Exception {

        Map<String, SectionSchema> schemas = new HashMap<>();
        Path sectionsDir = themePath.resolve("sections");

        if (!Files.exists(sectionsDir)) {
            log.warn("Sections directory not found: {}", sectionsDir);
            return schemas;
        }

        // 删除旧缓存
        schemaCacheMapper.deleteByThemeVersion(themeName, version);

        List<LiquidSchemaCache> cacheList = new ArrayList<>();

        // 扫描所有liquid文件
        try (Stream<Path> paths = Files.walk(sectionsDir, 1)) {
            paths.filter(path -> path.toString().endsWith(".liquid"))
                    .forEach(liquidFile -> {
                        try {
                            SectionSchema schema = schemaParser.parseSchema(liquidFile);
                            if (schema != null && schemaParser.isValidSchema(schema)) {
                                String fileName = sectionsDir.relativize(liquidFile).toString();
                                String sectionKey = fileName.replace(".liquid", "");

                                schemas.put(sectionKey, schema);

                                // 创建缓存记录
                                LiquidSchemaCache cache = LiquidSchemaCache.builder()
                                        .themeName(themeName)
                                        .version(version)
                                        .filePath("sections/" + fileName)
                                        .sectionName(schema.getName())
                                        .sectionType(sectionKey)
                                        .schemaJson(gson.toJson(schema))
                                        .settingsCount(schema.getSettings() != null ? schema.getSettings().size() : 0)
                                        .settingsHash(schemaParser.calculateSettingsHash(schema))
                                        .build();

                                cacheList.add(cache);
                            }
                        } catch (Exception e) {
                            log.error("Failed to parse schema from: {}", liquidFile, e);
                        }
                    });
        }

        // 批量插入缓存
        if (!cacheList.isEmpty()) {
            schemaCacheMapper.batchInsert(cacheList);
            log.info("Cached {} schemas for {} v{}", cacheList.size(), themeName, version);
        }

        return schemas;
    }

    /**
     * 分析Section重命名
     * 基于名称相似度和schema结构相似度
     */
    private List<SectionRenameRule> analyzeSectionRenames(
            Map<String, SectionSchema> oldSchemas,
            Map<String, SectionSchema> newSchemas) {

        List<SectionRenameRule> rules = new ArrayList<>();

        // 找出被删除的sections
        Set<String> deletedSections = new HashSet<>(oldSchemas.keySet());
        deletedSections.removeAll(newSchemas.keySet());

        // 找出新增的sections
        Set<String> addedSections = new HashSet<>(newSchemas.keySet());
        addedSections.removeAll(oldSchemas.keySet());

        // 对每个被删除的section，尝试在新增的sections中找到最匹配的
        for (String oldSection : deletedSections) {
            String bestMatch = null;
            double bestScore = 0.0;

            SectionSchema oldSchema = oldSchemas.get(oldSection);

            for (String newSection : addedSections) {
                SectionSchema newSchema = newSchemas.get(newSection);

                // 计算相似度分数
                double score = calculateSectionSimilarity(oldSection, newSection, oldSchema, newSchema);

                if (score > bestScore && score >= 0.6) { // 阈值0.6
                    bestScore = score;
                    bestMatch = newSection;
                }
            }

            if (bestMatch != null) {
                String confidence = bestScore >= 0.9 ? "HIGH" : bestScore >= 0.75 ? "MEDIUM" : "LOW";

                rules.add(SectionRenameRule.builder()
                        .oldName(oldSection)
                        .newName(bestMatch)
                        .confidence(confidence)
                        .reason(String.format("Name similarity: %.2f", bestScore))
                        .gitDetected(false)
                        .nameSimilarity(bestScore)
                        .build());

                addedSections.remove(bestMatch); // 已匹配，移除
            }
        }

        return rules;
    }

    /**
     * 计算section相似度
     */
    private double calculateSectionSimilarity(
            String oldName,
            String newName,
            SectionSchema oldSchema,
            SectionSchema newSchema) {

        // 1. 名称相似度 (权重50%)
        double nameSim = calculateStringSimilarity(oldName, newName) * 0.5;

        // 2. Schema名称相似度 (权重30%)
        double schemaNameSim = 0.0;
        if (oldSchema.getName() != null && newSchema.getName() != null) {
            schemaNameSim = calculateStringSimilarity(
                    oldSchema.getName(),
                    newSchema.getName()) * 0.3;
        }

        // 3. Settings数量相似度 (权重20%)
        double settingsSim = 0.0;
        int oldCount = oldSchema.getSettings() != null ? oldSchema.getSettings().size() : 0;
        int newCount = newSchema.getSettings() != null ? newSchema.getSettings().size() : 0;
        if (oldCount > 0 && newCount > 0) {
            settingsSim = (1.0 - Math.abs(oldCount - newCount) / (double) Math.max(oldCount, newCount)) * 0.2;
        }

        return nameSim + schemaNameSim + settingsSim;
    }

    /**
     * 分析字段映射
     */
    private List<FieldMappingRule> analyzeFieldMappings(
            Map<String, SectionSchema> oldSchemas,
            Map<String, SectionSchema> newSchemas,
            List<SectionRenameRule> renameRules) {

        List<FieldMappingRule> rules = new ArrayList<>();

        // 构建重命名映射
        Map<String, String> renameMap = renameRules.stream()
                .collect(Collectors.toMap(
                        SectionRenameRule::getOldName,
                        SectionRenameRule::getNewName));

        // 对每个section分析字段映射
        for (Map.Entry<String, SectionSchema> entry : oldSchemas.entrySet()) {
            String oldSectionKey = entry.getKey();
            SectionSchema oldSchema = entry.getValue();

            // 确定对应的新section
            String newSectionKey = renameMap.getOrDefault(oldSectionKey, oldSectionKey);
            SectionSchema newSchema = newSchemas.get(newSectionKey);

            if (newSchema == null || oldSchema.getSettings() == null || newSchema.getSettings() == null) {
                continue;
            }

            // 分析settings字段映射
            List<SchemaSetting> oldSettings = oldSchema.getSettings();
            List<SchemaSetting> newSettings = newSchema.getSettings();

            // 创建新字段的ID集合
            Set<String> newFieldIds = newSettings.stream()
                    .map(SchemaSetting::getId)
                    .collect(Collectors.toSet());

            // 对每个旧字段，尝试找到最佳匹配的新字段
            for (SchemaSetting oldSetting : oldSettings) {
                String oldFieldId = oldSetting.getId();

                // 如果新schema中有完全相同的字段ID，跳过
                if (newFieldIds.contains(oldFieldId)) {
                    continue;
                }

                // 查找最佳匹配
                String bestMatch = null;
                double bestScore = 0.0;

                for (SchemaSetting newSetting : newSettings) {
                    double score = calculateFieldSimilarity(oldSetting, newSetting);
                    if (score > bestScore && score >= 0.6) {
                        bestScore = score;
                        bestMatch = newSetting.getId();
                    }
                }

                if (bestMatch != null) {
                    String confidence = bestScore >= 0.9 ? "HIGH" : bestScore >= 0.75 ? "MEDIUM" : "LOW";

                    rules.add(FieldMappingRule.builder()
                            .sectionName(newSectionKey)
                            .oldFieldId(oldFieldId)
                            .newFieldId(bestMatch)
                            .confidence(confidence)
                            .similarity(bestScore)
                            .reason(String.format("Field similarity: %.2f", bestScore))
                            .needsValueConversion(false)
                            .build());
                }
            }
        }

        return rules;
    }

    /**
     * 计算字段相似度
     */
    private double calculateFieldSimilarity(SchemaSetting oldField, SchemaSetting newField) {
        // 1. ID相似度 (权重40%)
        double idSim = calculateStringSimilarity(oldField.getId(), newField.getId()) * 0.4;

        // 2. 类型匹配 (权重30%)
        double typeSim = oldField.getType().equals(newField.getType()) ? 0.3 : 0.0;

        // 3. 标签相似度 (权重30%)
        double labelSim = 0.0;
        if (oldField.getLabel() != null && newField.getLabel() != null) {
            labelSim = calculateStringSimilarity(oldField.getLabel(), newField.getLabel()) * 0.3;
        }

        return idSim + typeSim + labelSim;
    }

    /**
     * 分析默认值规则
     * 为新增字段生成默认值
     */
    private List<DefaultValueRule> analyzeDefaultValues(
            Map<String, SectionSchema> oldSchemas,
            Map<String, SectionSchema> newSchemas) {

        List<DefaultValueRule> rules = new ArrayList<>();

        for (Map.Entry<String, SectionSchema> entry : newSchemas.entrySet()) {
            String sectionKey = entry.getKey();
            SectionSchema newSchema = entry.getValue();
            SectionSchema oldSchema = oldSchemas.get(sectionKey);

            if (newSchema.getSettings() == null) {
                continue;
            }

            // 获取旧字段ID集合
            Set<String> oldFieldIds = new HashSet<>();
            if (oldSchema != null && oldSchema.getSettings() != null) {
                oldFieldIds = oldSchema.getSettings().stream()
                        .map(SchemaSetting::getId)
                        .collect(Collectors.toSet());
            }

            // 找出新增的字段
            for (SchemaSetting newSetting : newSchema.getSettings()) {
                if (!oldFieldIds.contains(newSetting.getId())) {
                    // 这是新增字段，生成默认值规则
                    Object defaultValue = newSetting.getDefault_value();
                    if (defaultValue == null) {
                        // 根据类型推断默认值
                        defaultValue = inferDefaultValue(newSetting.getType());
                    }

                    rules.add(DefaultValueRule.builder()
                            .sectionName(sectionKey)
                            .fieldId(newSetting.getId())
                            .defaultValue(defaultValue)
                            .valueType(newSetting.getType())
                            .reason("New field in target version")
                            .required(newSetting.getRequired() != null && newSetting.getRequired())
                            .build());
                }
            }
        }

        return rules;
    }

    /**
     * 根据字段类型推断默认值
     */
    private Object inferDefaultValue(String fieldType) {
        switch (fieldType.toLowerCase()) {
            case "text":
            case "textarea":
            case "richtext":
                return "";
            case "number":
            case "range":
                return 0;
            case "checkbox":
                return false;
            case "select":
            case "radio":
                return null; // 需要从options中选择
            case "color":
                return "#000000";
            case "url":
            case "image":
            case "video":
                return "";
            default:
                return null;
        }
    }

    /**
     * 计算字符串相似度（Levenshtein距离）
     */
    private double calculateStringSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }

        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) {
            return 1.0;
        }

        int distance = levenshteinDistance(s1, s2);
        return 1.0 - (double) distance / maxLen;
    }

    /**
     * Levenshtein距离算法
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }

        return dp[s1.length()][s2.length()];
    }

    /**
     * 保存规则到数据库
     */
    private <T> void saveRules(
            String themeName,
            String fromVersion,
            String toVersion,
            String ruleType,
            List<T> rules,
            String createdBy) {

        if (rules.isEmpty()) {
            return;
        }

        List<ThemeMigrationRule> ruleEntities = new ArrayList<>();

        for (T rule : rules) {
            String sectionName = null;
            String ruleJson = gson.toJson(rule);
            String confidence = "MEDIUM";

            // 根据规则类型提取section名称和置信度
            if (rule instanceof SectionRenameRule) {
                SectionRenameRule r = (SectionRenameRule) rule;
                sectionName = r.getOldName();
                confidence = r.getConfidence();
            } else if (rule instanceof FieldMappingRule) {
                FieldMappingRule r = (FieldMappingRule) rule;
                sectionName = r.getSectionName();
                confidence = r.getConfidence();
            } else if (rule instanceof DefaultValueRule) {
                DefaultValueRule r = (DefaultValueRule) rule;
                sectionName = r.getSectionName();
            }

            ruleEntities.add(ThemeMigrationRule.builder()
                    .themeName(themeName)
                    .fromVersion(fromVersion)
                    .toVersion(toVersion)
                    .ruleType(ruleType)
                    .sectionName(sectionName)
                    .ruleJson(ruleJson)
                    .confidence(confidence)
                    .createdBy(createdBy)
                    .build());
        }

        // 批量插入
        ruleMapper.batchInsert(ruleEntities);
        log.info("Saved {} {} rules", ruleEntities.size(), ruleType);
    }
}
