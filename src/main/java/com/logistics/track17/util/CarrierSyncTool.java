package com.logistics.track17.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 承运商映射表同步工具
 * 从 apicarrier.all.json 同步承运商数据到数据库
 */
@Slf4j
public class CarrierSyncTool {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/logistics_system?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "123456";
    private static final String JSON_FILE_PATH = "docs/apicarrier.all.json";

    public static void main(String[] args) {
        CarrierSyncTool tool = new CarrierSyncTool();
        try {
            log.info("开始同步承运商数据...");
            tool.syncCarriers();
            log.info("承运商数据同步完成！");
        } catch (Exception e) {
            log.error("同步失败: {}", e.getMessage(), e);
        }
    }

    public void syncCarriers() throws IOException, SQLException {
        // 读取JSON文件
        List<CarrierData> carriers = readCarriersFromJson();
        log.info("从JSON文件读取到 {} 条承运商数据", carriers.size());

        // 连接数据库并同步
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // 清空现有数据
            clearExistingData(conn);

            // 批量插入数据
            batchInsertCarriers(conn, carriers);

            log.info("成功同步 {} 条承运商数据到数据库", carriers.size());
        }
    }

    private List<CarrierData> readCarriersFromJson() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(new File(JSON_FILE_PATH));

        List<CarrierData> carriers = new ArrayList<>();
        for (JsonNode node : rootNode) {
            CarrierData carrier = new CarrierData();
            carrier.carrierId = node.get("key").asInt();
            carrier.carrierName = getStringValue(node, "_name");
            carrier.carrierNameCn = getStringValue(node, "_name_zh-cn");

            // 国家信息
            JsonNode countryNode = node.get("_country");
            carrier.countryId = countryNode != null && !countryNode.isNull() ? countryNode.asInt() : null;
            carrier.countryIso = getStringValue(node, "_country_iso");

            // 联系信息
            carrier.email = getStringValue(node, "_email");
            carrier.tel = getStringValue(node, "_tel");
            carrier.url = getStringValue(node, "_url");

            // 生成carrier_code (使用carrier名称的小写并替换空格为短横线)
            carrier.carrierCode = generateCarrierCode(carrier.carrierName, carrier.carrierId);

            carriers.add(carrier);
        }

        return carriers;
    }

    private String getStringValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asText() : null;
    }

    private String generateCarrierCode(String carrierName, Integer carrierId) {
        if (carrierName == null || carrierName.trim().isEmpty()) {
            return "carrier-" + carrierId;
        }

        // 移除特殊字符，转小写，空格替换为短横线
        String code = carrierName.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        // 如果处理后为空，使用carrier-{id}
        if (code.isEmpty()) {
            return "carrier-" + carrierId;
        }

        // 限制长度在50字符以内
        if (code.length() > 50) {
            code = code.substring(0, 50).replaceAll("-$", "");
        }

        return code;
    }

    private void clearExistingData(Connection conn) throws SQLException {
        String sql = "DELETE FROM carriers";
        try (Statement stmt = conn.createStatement()) {
            int deleted = stmt.executeUpdate(sql);
            log.info("清空现有数据，删除 {} 条记录", deleted);
        }
    }

    private void batchInsertCarriers(Connection conn, List<CarrierData> carriers) throws SQLException {
        String sql = "INSERT INTO carriers (carrier_id, carrier_code, carrier_name, carrier_name_cn, " +
                "country_id, country_iso, email, tel, url, is_active, sort_order) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);

            int count = 0;
            for (int i = 0; i < carriers.size(); i++) {
                CarrierData carrier = carriers.get(i);

                pstmt.setInt(1, carrier.carrierId);
                pstmt.setString(2, carrier.carrierCode);
                pstmt.setString(3, carrier.carrierName);
                pstmt.setString(4, carrier.carrierNameCn);

                if (carrier.countryId != null) {
                    pstmt.setInt(5, carrier.countryId);
                } else {
                    pstmt.setNull(5, Types.INTEGER);
                }

                pstmt.setString(6, carrier.countryIso);
                pstmt.setString(7, carrier.email);
                pstmt.setString(8, carrier.tel);
                pstmt.setString(9, carrier.url);
                pstmt.setBoolean(10, true); // 默认启用
                pstmt.setInt(11, i); // 使用序号作为排序

                pstmt.addBatch();
                count++;

                // 每1000条提交一次
                if (count % 1000 == 0) {
                    pstmt.executeBatch();
                    conn.commit();
                    log.info("已插入 {} 条记录", count);
                }
            }

            // 提交剩余的记录
            pstmt.executeBatch();
            conn.commit();
            log.info("批量插入完成，共 {} 条记录", count);
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /**
     * 承运商数据临时类
     */
    private static class CarrierData {
        Integer carrierId;
        String carrierCode;
        String carrierName;
        String carrierNameCn;
        Integer countryId;
        String countryIso;
        String email;
        String tel;
        String url;
    }
}
