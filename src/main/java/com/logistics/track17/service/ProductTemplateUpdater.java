package com.logistics.track17.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.logistics.track17.dto.BatchUpdateResult;
import com.logistics.track17.dto.ExecutableMigrationRules;
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
            ExecutableMigrationRules rules) throws Exception {
        log.info("Starting template migration from old to new theme...");

        BatchUpdateResult result = BatchUpdateResult.builder()
                .templatesUpdated(0)
                .rulesApplied(rules.getSectionRenames().size() +
                        rules.getFieldMappings().size() +
                        rules.getDefaultValues().size())
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
            ExecutableMigrationRules rules) throws IOException {
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

        // 遍历所有section实例，应用迁移规则
        for (Map.Entry<String, JsonElement> entry : sections.entrySet()) {
            String sectionId = entry.getKey();
            JsonObject sectionData = entry.getValue().getAsJsonObject();

            // 1. Section重命名：更新type字段
            if (sectionData.has("type")) {
                String currentType = sectionData.get("type").getAsString();
                String newType = rules.getMappedSectionName(currentType);

                if (!newType.equals(currentType)) {
                    sectionData.addProperty("type", newType);
                    modified = true;
                    log.debug("  Section {}: renamed type {} -> {}", sectionId, currentType, newType);
                }

                // 2. 字段映射：更新settings中的字段ID
                if (sectionData.has("settings") && rules.hasFieldMappings(newType)) {
                    JsonObject settings = sectionData.getAsJsonObject("settings");
                    boolean settingsModified = applyFieldMappings(settings, newType, rules);
                    if (settingsModified) {
                        modified = true;
                    }
                }

                // 3. 默认值：为新字段添加默认值
                if (rules.hasDefaultValues(newType)) {
                    if (!sectionData.has("settings")) {
                        sectionData.add("settings", new JsonObject());
                    }
                    JsonObject settings = sectionData.getAsJsonObject("settings");
                    boolean defaultsAdded = applyDefaultValues(settings, newType, rules);
                    if (defaultsAdded) {
                        modified = true;
                    }
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
     * 应用字段映射规则
     */
    private boolean applyFieldMappings(JsonObject settings, String sectionName, ExecutableMigrationRules rules) {
        boolean modified = false;
        Map<String, String> fieldMappings = rules.getFieldMappings().get(sectionName);

        if (fieldMappings == null || fieldMappings.isEmpty()) {
            return false;
        }

        // 需要重命名的字段
        Map<String, JsonElement> fieldsToRename = new HashMap<>();

        for (Map.Entry<String, String> mapping : fieldMappings.entrySet()) {
            String oldFieldId = mapping.getKey();
            String newFieldId = mapping.getValue();

            if (settings.has(oldFieldId)) {
                // 保存旧字段的值
                fieldsToRename.put(newFieldId, settings.get(oldFieldId));
                // 移除旧字段
                settings.remove(oldFieldId);
                modified = true;
                log.debug("    Mapped field: {} -> {}", oldFieldId, newFieldId);
            }
        }

        // 添加重命名后的字段
        for (Map.Entry<String, JsonElement> entry : fieldsToRename.entrySet()) {
            settings.add(entry.getKey(), entry.getValue());
        }

        return modified;
    }

    /**
     * 应用默认值规则
     */
    private boolean applyDefaultValues(JsonObject settings, String sectionName, ExecutableMigrationRules rules) {
        boolean modified = false;
        Map<String, Object> defaultValues = rules.getDefaultValues().get(sectionName);

        if (defaultValues == null || defaultValues.isEmpty()) {
            return false;
        }

        for (Map.Entry<String, Object> entry : defaultValues.entrySet()) {
            String fieldId = entry.getKey();
            Object defaultValue = entry.getValue();

            // 只为不存在的字段添加默认值
            if (!settings.has(fieldId)) {
                addJsonValue(settings, fieldId, defaultValue);
                modified = true;
                log.debug("    Added default value for {}: {}", fieldId, defaultValue);
            }
        }

        return modified;
    }

    /**
     * 添加JSON值（根据类型）
     */
    private void addJsonValue(JsonObject obj, String key, Object value) {
        if (value == null) {
            obj.add(key, null);
        } else if (value instanceof String) {
            obj.addProperty(key, (String) value);
        } else if (value instanceof Number) {
            obj.addProperty(key, (Number) value);
        } else if (value instanceof Boolean) {
            obj.addProperty(key, (Boolean) value);
        } else {
            // 复杂对象，转为JSON
            obj.add(key, gson.toJsonTree(value));
        }
    }
}
