package com.logistics.track17.dto;

import lombok.Data;
import java.util.*;

/**
 * 迁移规则建议
 */
@Data
public class MigrationRuleSuggestion {

    /** Section映射 (oldSection -> newSection) */
    private Map<String, SectionMapping> sectionMappings = new HashMap<>();

    /** Settings映射 */
    private Map<String, Map<String, String>> settingsMappings = new HashMap<>();

    /**
     * Section映射详情
     */
    @Data
    public static class SectionMapping {
        private String oldSection;
        private String newSection;
        private String confidence; // CONFIRMED, HIGH, MEDIUM, LOW
        private String reason;
    }

    public void addMapping(String oldSection, String newSection,
            String confidence, String reason) {
        SectionMapping mapping = new SectionMapping();
        mapping.setOldSection(oldSection);
        mapping.setNewSection(newSection);
        mapping.setConfidence(confidence);
        mapping.setReason(reason);
        sectionMappings.put(oldSection, mapping);
    }

    public String getMappedSectionType(String oldType) {
        SectionMapping mapping = sectionMappings.get(oldType);
        return mapping != null ? mapping.getNewSection() : null;
    }
}
