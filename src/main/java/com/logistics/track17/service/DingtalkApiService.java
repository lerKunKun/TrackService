package com.logistics.track17.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.track17.dto.DingtalkDepartmentDTO;
import com.logistics.track17.dto.DingtalkUserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 钉钉API服务
 * 封装钉钉开放平台API调用
 */
@Service
@Slf4j
public class DingtalkApiService {

    @Value("${dingtalk.app-key}")
    private String appKey;

    @Value("${dingtalk.app-secret}")
    private String appSecret;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Access Token缓存
    private String accessToken;
    private LocalDateTime tokenExpireTime;

    public DingtalkApiService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 获取Access Token
     * 有效期2小时，自动缓存
     */
    public String getAccessToken() {
        // 如果token未过期，直接返回
        if (accessToken != null && LocalDateTime.now().isBefore(tokenExpireTime)) {
            return accessToken;
        }

        try {
            String url = String.format(
                    "https://oapi.dingtalk.com/gettoken?appkey=%s&appsecret=%s",
                    appKey, appSecret);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            if (root.get("errcode").asInt() == 0) {
                accessToken = root.get("access_token").asText();
                // 设置过期时间为1小时50分钟后（提前10分钟刷新）
                tokenExpireTime = LocalDateTime.now().plusMinutes(110);
                log.info("钉钉Access Token获取成功，过期时间: {}", tokenExpireTime);
                return accessToken;
            } else {
                String errorMsg = root.get("errmsg").asText();
                log.error("获取钉钉Access Token失败: {}", errorMsg);
                throw new RuntimeException("获取钉钉Access Token失败: " + errorMsg);
            }
        } catch (Exception e) {
            log.error("获取钉钉Access Token异常", e);
            throw new RuntimeException("获取钉钉Access Token异常: " + e.getMessage());
        }
    }

    /**
     * 获取所有部门（递归）
     */
    public List<DingtalkDepartmentDTO> getAllDepartments() {
        List<DingtalkDepartmentDTO> allDepts = new ArrayList<>();
        fetchDepartmentsRecursively(1L, allDepts); // 从根部门开始
        return allDepts;
    }

    /**
     * 递归获取部门
     */
    private void fetchDepartmentsRecursively(Long deptId, List<DingtalkDepartmentDTO> result) {
        List<DingtalkDepartmentDTO> subDepts = getDepartmentList(deptId);
        result.addAll(subDepts);

        for (DingtalkDepartmentDTO dept : subDepts) {
            fetchDepartmentsRecursively(dept.getDeptId(), result);
        }
    }

    /**
     * 获取子部门列表
     * API: POST /topapi/v2/department/listsub
     */
    public List<DingtalkDepartmentDTO> getDepartmentList(Long parentDeptId) {
        try {
            String accessToken = getAccessToken();
            String url = "https://oapi.dingtalk.com/topapi/v2/department/listsub?access_token=" + accessToken;

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("dept_id", parentDeptId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            if (root.get("errcode").asInt() == 0) {
                List<DingtalkDepartmentDTO> departments = new ArrayList<>();
                JsonNode resultNode = root.get("result");

                if (resultNode != null && resultNode.isArray()) {
                    for (JsonNode node : resultNode) {
                        DingtalkDepartmentDTO dept = new DingtalkDepartmentDTO();
                        dept.setDeptId(node.get("dept_id").asLong());
                        dept.setName(node.get("name").asText());
                        dept.setParentId(node.has("parent_id") ? node.get("parent_id").asLong() : 0L);
                        dept.setOrder(node.has("order") ? node.get("order").asInt() : 0);
                        dept.setCreateDeptGroup(
                                node.has("create_dept_group") && node.get("create_dept_group").asBoolean());
                        dept.setAutoAddUser(node.has("auto_add_user") && node.get("auto_add_user").asBoolean());
                        departments.add(dept);
                    }
                }

                log.debug("获取部门列表成功，父部门ID: {}, 数量: {}", parentDeptId, departments.size());
                return departments;
            } else {
                String errorMsg = root.get("errmsg").asText();
                log.error("获取部门列表失败: {}", errorMsg);
                throw new RuntimeException("获取部门列表失败: " + errorMsg);
            }
        } catch (Exception e) {
            log.error("获取部门列表异常，父部门ID: {}", parentDeptId, e);
            throw new RuntimeException("获取部门列表异常: " + e.getMessage());
        }
    }

    /**
     * 获取部门用户列表
     * API: POST /topapi/v2/user/list
     */
    public List<DingtalkUserDTO> getDepartmentUsers(Long deptId) {
        List<DingtalkUserDTO> allUsers = new ArrayList<>();
        Long cursor = 0L;
        Integer size = 100; // 每页最多100条

        try {
            while (true) {
                String accessToken = getAccessToken();
                String url = "https://oapi.dingtalk.com/topapi/v2/user/list?access_token=" + accessToken;

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("dept_id", deptId);
                requestBody.put("cursor", cursor);
                requestBody.put("size", size);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
                JsonNode root = objectMapper.readTree(response.getBody());

                if (root.get("errcode").asInt() == 0) {
                    JsonNode resultNode = root.get("result");
                    JsonNode listNode = resultNode.get("list");

                    if (listNode != null && listNode.isArray()) {
                        for (JsonNode node : listNode) {
                            DingtalkUserDTO user = new DingtalkUserDTO();
                            user.setUserid(node.get("userid").asText());
                            user.setUnionid(node.has("unionid") ? node.get("unionid").asText() : null);
                            user.setName(node.get("name").asText());
                            user.setMobile(node.has("mobile") ? node.get("mobile").asText() : null);
                            user.setEmail(node.has("email") ? node.get("email").asText() : null);
                            user.setTitle(node.has("title") ? node.get("title").asText() : null);
                            user.setJobNumber(node.has("job_number") ? node.get("job_number").asText() : null);
                            user.setAvatar(node.has("avatar") ? node.get("avatar").asText() : null);
                            user.setActive(node.has("active") && node.get("active").asBoolean());
                            user.setAdmin(node.has("admin") && node.get("admin").asBoolean());
                            user.setBoss(node.has("boss") && node.get("boss").asBoolean());

                            // 部门ID列表
                            List<Long> deptIdList = new ArrayList<>();
                            if (node.has("dept_id_list")) {
                                for (JsonNode deptIdNode : node.get("dept_id_list")) {
                                    deptIdList.add(deptIdNode.asLong());
                                }
                            }
                            user.setDeptIdList(deptIdList);

                            allUsers.add(user);
                        }
                    }

                    // 检查是否还有下一页
                    if (resultNode.has("has_more") && resultNode.get("has_more").asBoolean()) {
                        cursor = resultNode.get("next_cursor").asLong();
                    } else {
                        break;
                    }
                } else {
                    String errorMsg = root.get("errmsg").asText();
                    log.error("获取部门用户列表失败: {}", errorMsg);
                    throw new RuntimeException("获取部门用户列表失败: " + errorMsg);
                }
            }

            log.info("获取部门用户列表成功，部门ID: {}, 用户数: {}", deptId, allUsers.size());
            return allUsers;
        } catch (Exception e) {
            log.error("获取部门用户列表异常，部门ID: {}", deptId, e);
            throw new RuntimeException("获取部门用户列表异常: " + e.getMessage());
        }
    }

    /**
     * 获取用户详情
     * API: POST /topapi/v2/user/get
     */
    public DingtalkUserDTO getUserDetail(String userid) {
        try {
            String accessToken = getAccessToken();
            String url = "https://oapi.dingtalk.com/topapi/v2/user/get?access_token=" + accessToken;

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("userid", userid);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            if (root.get("errcode").asInt() == 0) {
                JsonNode resultNode = root.get("result");
                DingtalkUserDTO user = new DingtalkUserDTO();
                user.setUserid(resultNode.get("userid").asText());
                user.setUnionid(resultNode.has("unionid") ? resultNode.get("unionid").asText() : null);
                user.setName(resultNode.get("name").asText());
                user.setMobile(resultNode.has("mobile") ? resultNode.get("mobile").asText() : null);
                user.setEmail(resultNode.has("email") ? resultNode.get("email").asText() : null);
                user.setTitle(resultNode.has("title") ? resultNode.get("title").asText() : null);
                user.setJobNumber(resultNode.has("job_number") ? resultNode.get("job_number").asText() : null);
                user.setAvatar(resultNode.has("avatar") ? resultNode.get("avatar").asText() : null);

                return user;
            } else {
                String errorMsg = root.get("errmsg").asText();
                log.error("获取用户详情失败: {}", errorMsg);
                throw new RuntimeException("获取用户详情失败: " + errorMsg);
            }
        } catch (Exception e) {
            log.error("获取用户详情异常，userid: {}", userid, e);
            throw new RuntimeException("获取用户详情异常: " + e.getMessage());
        }
    }
}
