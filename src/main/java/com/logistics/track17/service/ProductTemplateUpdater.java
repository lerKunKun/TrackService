package com.logistics.track17.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.logistics.track17.dto.BatchUpdateResult;
import com.logistics.track17.dto.MigrationRuleSuggestion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 产品模板更新器
 * 批量更新product.*.json模板中的section引用
 */
@Slf4j
@Service
public class ProductTemplateUpdater {

    private final Gson gson = new Gson();

    /**
     * 迁移模板：从旧版本读取product.*.json，更新后写入新版本
     */
    public BatchUpdateResult migrateTemplates(
            Path oldThemePath,
            Path newThemePath,
            MigrationRuleSuggestion rules) throws Exception {
        log.info("Starting template migration from old to new theme...");

        BatchUpdateResult result = BatchUpdateResult.builder()
                .templatesUpdated(0)
                .rulesApplied(rules.getSectionMappings().size())
                .sectionsUpdated(0)
                .templatesSkipped(0)
                .build();

        Path oldTemplatesDir = oldThemePath.resolve("templates");
        Path newTemplatesDir = newThemePath.resolve("templates");

        if (!Files.exists(oldTemplatesDir)) {
            log.warn("Old templates directory not found: {}", oldTemplatesDir);
            return result;
        }

        // 确保新版本的templates目录存在
        Files.createDirectories(newTemplatesDir);

        // 从旧版本读取所有JSON模板文件
        try (Stream<Path> paths = Files.walk(oldTemplatesDir, 1)) {
            paths.filter(path -> {
                String fileName = path.getFileName().toString();
                // 包含所有.json文件，排除配置文件
                return fileName.endsWith(".json") &&
                        !fileName.equals("config.json") &&
                        !fileName.startsWith(".");
            }).forEach(oldTemplatePath -> {
                try {
                    // 更新模板并写入新版本
                    Path newTemplatePath = newTemplatesDir.resolve(oldTemplatePath.getFileName());
                    boolean updated = migrateSingleTemplate(oldTemplatePath, newTemplatePath, rules);

                    if (updated) {
                        result.setTemplatesUpdated(result.getTemplatesUpdated() + 1);
                    } else {
                        result.setTemplatesSkipped(result.getTemplatesSkipped() + 1);
                    }
                } catch (Exception e) {
                    log.error("Failed to migrate template: {}", oldTemplatePath, e);
                }
            });
        }

        log.info("Template migration completed. Updated: {}, Skipped: {}",
                result.getTemplatesUpdated(), result.getTemplatesSkipped());

        return result;
    }

    /**
     * 迁移单个模板文件：从旧版本读取，更新后写入新版本
     */
    private boolean migrateSingleTemplate(
            Path oldTemplatePath,
            Path newTemplatePath,
            MigrationRuleSuggestion rules) throws IOException {
        log.debug("Migrating template: {}", oldTemplatePath.getFileName());

        // 从旧版本读取JSON
        String jsonContent = new String(Files.readAllBytes(oldTemplatePath));
        JsonObject template = gson.fromJson(jsonContent, JsonObject.class);

        boolean modified = false;

        // 检查是否有sections字段
        if (!template.has("sections")) {
            log.debug("Template has no sections field, copying as-is");
            // 即使没有sections，也要复制到新版本
            Files.copy(oldTemplatePath, newTemplatePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return false;
        }

        JsonObject sections = template.getAsJsonObject("sections");

        // 遍历所有section实例，更新type引用
        for (Map.Entry<String, JsonElement> entry : sections.entrySet()) {
            String sectionId = entry.getKey();
            JsonObject sectionData = entry.getValue().getAsJsonObject();

            if (sectionData.has("type")) {
                String currentType = sectionData.get("type").getAsString();
                String newType = rules.getMappedSectionType(currentType);

                if (newType != null && !newType.equals(currentType)) {
                    // 更新section type
                    sectionData.addProperty("type", newType);
                    modified = true;

                    log.debug("  Section {}: {} -> {}", sectionId, currentType, newType);
                }
            }
        }

        // 写入新版本目录
        String updatedJson = gson.toJson(template);
        Files.write(newTemplatePath, updatedJson.getBytes());

        if (modified) {
            log.info("Migrated and updated template: {}", newTemplatePath.getFileName());
        } else {
            log.debug("Migrated template without changes: {}", newTemplatePath.getFileName());
        }

        return modified;
    }

    /**
     * 更新order字段（如果存在）
     */
    private void updateOrder(JsonObject template, MigrationRuleSuggestion rules) {
        if (!template.has("order")) {
            return;
        }

        JsonArray order = template.getAsJsonArray("order");

        for (int i = 0; i < order.size(); i++) {
            String sectionId = order.get(i).getAsString();

            // 检查sections中对应的type
            if (template.has("sections")) {
                JsonObject sections = template.getAsJsonObject("sections");
                if (sections.has(sectionId)) {
                    JsonObject sectionData = sections.getAsJsonObject(sectionId);
                    if (sectionData.has("type")) {
                        String type = sectionData.get("type").getAsString();
                        // order字段只是section ID列表，不需要修改
                        // 实际type已经在sections字段中更新了
                    }
                }
            }
        }
    }
}
