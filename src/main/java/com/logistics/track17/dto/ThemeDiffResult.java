package com.logistics.track17.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 主题Diff分析结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThemeDiffResult {

    /** Section文件变更 */
    private SectionFileChanges sectionChanges;

    /** Schema变更（sectionName -> changes） */
    private Map<String, SchemaChanges> schemaChanges;

    /** 建议的映射规则 */
    private MigrationRuleSuggestion suggestedRules;

    public boolean hasChanges() {
        return sectionChanges != null && sectionChanges.hasChanges();
    }
}
