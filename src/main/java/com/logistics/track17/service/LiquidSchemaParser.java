package com.logistics.track17.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.logistics.track17.dto.SectionSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Liquid Schema解析器
 * 从Liquid文件中提取{% schema %}块并解析为Java对象
 */
@Slf4j
@Service
public class LiquidSchemaParser {

    private final Gson gson;

    // Liquid schema块的正则表达式
    private static final Pattern SCHEMA_PATTERN = Pattern.compile(
            "\\{%\\s*schema\\s*%\\}(.+?)\\{%\\s*endschema\\s*%\\}",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    public LiquidSchemaParser() {
        // 创建Gson实例，配置为宽松模式处理Liquid中的JSON
        this.gson = new GsonBuilder()
                .setLenient()
                .create();
    }

    /**
     * 从Liquid文件中解析schema
     * 
     * @param liquidFile Liquid文件路径
     * @return SectionSchema对象，如果没有找到schema则返回null
     * @throws Exception 文件读取或JSON解析异常
     */
    public SectionSchema parseSchema(Path liquidFile) throws Exception {
        if (!Files.exists(liquidFile)) {
            throw new IllegalArgumentException("Liquid file not found: " + liquidFile);
        }

        if (!liquidFile.toString().endsWith(".liquid")) {
            log.warn("File is not a .liquid file: {}", liquidFile);
        }

        // 读取文件内容（Java 8兼容方式）
        String content = new String(Files.readAllBytes(liquidFile), java.nio.charset.StandardCharsets.UTF_8);

        // 查找schema块
        Matcher matcher = SCHEMA_PATTERN.matcher(content);

        if (!matcher.find()) {
            log.debug("No schema block found in file: {}", liquidFile.getFileName());
            return null;
        }

        // 提取schema JSON字符串
        String schemaJson = matcher.group(1).trim();

        try {
            // 解析JSON为SectionSchema对象
            SectionSchema schema = gson.fromJson(schemaJson, SectionSchema.class);

            log.debug("Parsed schema from {}: name={}, settings={}",
                    liquidFile.getFileName(),
                    schema.getName(),
                    schema.getSettings() != null ? schema.getSettings().size() : 0);

            return schema;

        } catch (Exception e) {
            log.error("Failed to parse schema JSON from file: {}", liquidFile, e);
            log.error("Schema JSON content: {}", schemaJson);
            throw new Exception("Failed to parse schema from " + liquidFile + ": " + e.getMessage(), e);
        }
    }

    /**
     * 从Liquid文件内容字符串中解析schema
     * 
     * @param liquidContent Liquid文件的完整内容
     * @return SectionSchema对象，如果没有找到schema则返回null
     */
    public SectionSchema parseSchemaFromString(String liquidContent) {
        if (liquidContent == null || liquidContent.isEmpty()) {
            return null;
        }

        Matcher matcher = SCHEMA_PATTERN.matcher(liquidContent);

        if (!matcher.find()) {
            return null;
        }

        String schemaJson = matcher.group(1).trim();

        try {
            return gson.fromJson(schemaJson, SectionSchema.class);
        } catch (Exception e) {
            log.error("Failed to parse schema from string", e);
            return null;
        }
    }

    /**
     * 计算schema的settings哈希值（用于快速比较）
     * 
     * @param schema Section schema
     * @return MD5哈希字符串
     */
    public String calculateSettingsHash(SectionSchema schema) {
        if (schema == null || schema.getSettings() == null) {
            return "";
        }

        try {
            // 将settings序列化为JSON字符串
            String settingsJson = gson.toJson(schema.getSettings());

            // 计算MD5哈希
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(settingsJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // 转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (Exception e) {
            log.error("Failed to calculate settings hash", e);
            return "";
        }
    }

    /**
     * 验证schema是否有效
     * 
     * @param schema Section schema
     * @return true if valid, false otherwise
     */
    public boolean isValidSchema(SectionSchema schema) {
        if (schema == null) {
            return false;
        }

        // 至少需要有name
        if (schema.getName() == null || schema.getName().trim().isEmpty()) {
            log.warn("Schema missing name field");
            return false;
        }

        return true;
    }
}
