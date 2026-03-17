package com.logistics.track17.mapper;

import com.logistics.track17.entity.EmailMonitorConfig;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 邮箱监控配置Mapper
 */
@Mapper
public interface EmailMonitorConfigMapper {

    @Select("SELECT * FROM email_monitor_config WHERE is_enabled = 1")
    List<EmailMonitorConfig> findAllEnabled();

    @Select("SELECT * FROM email_monitor_config ORDER BY created_time DESC")
    List<EmailMonitorConfig> findAll();

    @Select("SELECT * FROM email_monitor_config WHERE id = #{id}")
    EmailMonitorConfig findById(@Param("id") Long id);

    @Insert("INSERT INTO email_monitor_config (name, host, port, protocol, username, password, sender_filter, check_interval, is_enabled, created_by, auth_type) "
            + "VALUES (#{name}, #{host}, #{port}, #{protocol}, #{username}, #{password}, #{senderFilter}, #{checkInterval}, #{isEnabled}, #{createdBy}, #{authType})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(EmailMonitorConfig config);

    @Update("UPDATE email_monitor_config SET name = #{name}, host = #{host}, port = #{port}, protocol = #{protocol}, " +
            "username = #{username}, password = #{password}, sender_filter = #{senderFilter}, check_interval = #{checkInterval}, " +
            "is_enabled = #{isEnabled}, auth_type = #{authType} WHERE id = #{id}")
    int update(EmailMonitorConfig config);

    @Update("UPDATE email_monitor_config SET access_token = #{accessToken}, refresh_token = #{refreshToken}, " +
            "token_expiry = #{tokenExpiry}, auth_type = 'OAUTH2_MICROSOFT' WHERE id = #{id}")
    int updateTokens(@Param("id") Long id, @Param("accessToken") String accessToken,
            @Param("refreshToken") String refreshToken, @Param("tokenExpiry") LocalDateTime tokenExpiry);

    @Update("UPDATE email_monitor_config SET last_check_time = #{lastCheckTime}, last_check_status = #{status}, " +
            "last_error_message = #{errorMessage} WHERE id = #{id}")
    int updateCheckStatus(@Param("id") Long id, @Param("lastCheckTime") LocalDateTime lastCheckTime,
            @Param("status") String status, @Param("errorMessage") String errorMessage);

    @Delete("DELETE FROM email_monitor_config WHERE id = #{id}")
    int delete(@Param("id") Long id);
}
