package com.logistics.track17.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 可执行的迁移规则
 * 用于模板更新器执行迁移
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutableMigrationRules {

    /**
     * Section重命名映射
     * key: 旧section名称, value: 新section名称
     */
    @Builder.Default
    private Map<String, String> sectionRenames = new HashMap<>();

    /**
     * 字段映射
     * key: section名称, value: 字段映射(旧字段ID -> 新字段ID)
     */
    @Builder.Default
    private Map<String, Map<String, String>> fieldMappings = new HashMap<>();

    /**
     * 默认值
     * key: section名称, value: 字段默认值(字段ID -> 默认值)
     */
    @Builder.Default
    private Map<String, Map<String, Object>> defaultValues = new HashMap<>();

    /**
     * 获取section的映射名称
     */
    public String getMappedSectionName(String oldName) {
        return sectionRenames.getOrDefault(oldName, oldName);
    }

    /**
     * 获取字段的映射ID
     */
    public String getMappedFieldId(String sectionName, String oldFieldId) {
        Map<String, String> sectionFieldMappings = fieldMappings.get(sectionName);
        if (sectionFieldMappings == null) {
            return oldFieldId;
        }
        return sectionFieldMappings.getOrDefault(oldFieldId, oldFieldId);
    }

    /**
     * 获取字段的默认值
     */
    public Object getDefaultValue(String sectionName, String fieldId) {
        Map<String, Object> sectionDefaults = defaultValues.get(sectionName);
        if (sectionDefaults == null) {
            return null;
        }
        return sectionDefaults.get(fieldId);
    }

    /**
     * 检查section是否有字段映射
     */
    public boolean hasFieldMappings(String sectionName) {
        return fieldMappings.containsKey(sectionName) &&
                !fieldMappings.get(sectionName).isEmpty();
    }

    /**
     * 检查section是否有默认值
     */
    public boolean hasDefaultValues(String sectionName) {
        return defaultValues.containsKey(sectionName) &&
                !defaultValues.get(sectionName).isEmpty();
    }

    /**
     * 添加section重命名规则
     */
    public void addSectionRename(String oldName, String newName) {
        sectionRenames.put(oldName, newName);
    }

    /**
     * 添加字段映射规则
     */
    public void addFieldMapping(String sectionName, String oldFieldId, String newFieldId) {
        fieldMappings.computeIfAbsent(sectionName, k -> new HashMap<>())
                .put(oldFieldId, newFieldId);
    }

    /**
     * 添加默认值规则
     */
    public void addDefaultValue(String sectionName, String fieldId, Object defaultValue) {
        defaultValues.computeIfAbsent(sectionName, k -> new HashMap<>())
                .put(fieldId, defaultValue);
    }

    /**
     * 获取统计信息
     */
    public String getStatistics() {
        return String.format("Section renames: %d, Field mappings: %d sections, Default values: %d sections",
                sectionRenames.size(),
                fieldMappings.size(),
                defaultValues.size());
    }
}
