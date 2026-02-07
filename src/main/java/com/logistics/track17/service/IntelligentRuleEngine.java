package com.logistics.track17.service;

import com.logistics.track17.dto.MigrationRuleSuggestion;
import com.logistics.track17.dto.SectionFileChanges;
import com.logistics.track17.dto.ThemeDiffResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 智能规则推断引擎
 * 基于Git Diff结果生成迁移规则建议
 */
@Slf4j
@Service
public class IntelligentRuleEngine {

    /**
     * 基于Diff结果推断迁移规则
     */
    public MigrationRuleSuggestion inferRules(ThemeDiffResult diffResult) {
        log.info("Starting intelligent rule inference...");

        MigrationRuleSuggestion suggestion = new MigrationRuleSuggestion();
        SectionFileChanges changes = diffResult.getSectionChanges();

        // 1. 处理Git检测到的重命名（置信度：CONFIRMED）
        processConfirmedRenames(changes, suggestion);

        // 2. 匹配剩余的deleted和added（使用名称相似度）
        matchRemainingChanges(changes, suggestion);

        log.info("Rule inference completed. Generated {} mappings",
                suggestion.getSectionMappings().size());

        return suggestion;
    }

    /**
     * 处理Git确认的重命名
     */
    private void processConfirmedRenames(SectionFileChanges changes,
            MigrationRuleSuggestion suggestion) {
        Map<String, String> renamed = changes.getRenamed();

        for (Map.Entry<String, String> entry : renamed.entrySet()) {
            String oldName = entry.getKey();
            String newName = entry.getValue();

            suggestion.addMapping(oldName, newName, "CONFIRMED",
                    "Git detected rename (100% content match)");

            log.debug("Confirmed rename: {} -> {}", oldName, newName);
        }
    }

    /**
     * 匹配剩余的deleted和added sections
     */
    private void matchRemainingChanges(SectionFileChanges changes,
            MigrationRuleSuggestion suggestion) {

        // 为每个deleted section找到最佳匹配的added section
        for (String deletedSection : changes.getDeleted()) {
            String bestMatch = null;
            double highestScore = 0.0;

            for (String addedSection : changes.getAdded()) {
                // 跳过已经被映射的section
                if (isSectionAlreadyMapped(addedSection, suggestion)) {
                    continue;
                }

                // 计算名称相似度
                double similarity = calculateNameSimilarity(deletedSection, addedSection);

                if (similarity > highestScore) {
                    highestScore = similarity;
                    bestMatch = addedSection;
                }
            }

            // 如果找到足够相似的匹配
            if (bestMatch != null && highestScore > 0.5) {
                String confidence = getConfidenceLevel(highestScore);
                String reason = String.format("Name similarity: %.0f%%", highestScore * 100);

                suggestion.addMapping(deletedSection, bestMatch, confidence, reason);

                log.debug("Matched: {} -> {} (similarity: {}, confidence: {})",
                        deletedSection, bestMatch, highestScore, confidence);
            }
        }
    }

    /**
     * 检查section是否已被映射
     */
    private boolean isSectionAlreadyMapped(String sectionName,
            MigrationRuleSuggestion suggestion) {
        return suggestion.getSectionMappings().values().stream()
                .anyMatch(mapping -> mapping.getNewSection().equals(sectionName));
    }

    /**
     * 计算两个section名称的相似度
     * 使用Levenshtein距离算法
     */
    private double calculateNameSimilarity(String s1, String s2) {
        // 转换为小写以忽略大小写
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        int distance = levenshteinDistance(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());

        // 相似度 = 1 - (距离 / 最大长度)
        return 1.0 - ((double) distance / maxLength);
    }

    /**
     * Levenshtein距离算法
     * 计算将字符串s1转换为s2所需的最少编辑次数
     */
    private int levenshteinDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();

        int[][] dp = new int[m + 1][n + 1];

        // 初始化
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
        }

        // 动态规划
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(
                            Math.min(dp[i - 1][j], dp[i][j - 1]),
                            dp[i - 1][j - 1]) + 1;
                }
            }
        }

        return dp[m][n];
    }

    /**
     * 根据相似度分数确定置信度等级
     */
    private String getConfidenceLevel(double similarity) {
        if (similarity >= 0.9) {
            return "HIGH";
        } else if (similarity >= 0.7) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
}
