package com.logistics.track17.mapper;

import com.logistics.track17.entity.NotificationLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 通知日志Mapper
 */
@Mapper
public interface NotificationLogMapper {

    @Insert("INSERT INTO notification_log (shop_id, alert_type, severity, source, title, content, dedup_key, recipient_userid, send_status, error_message, sent_time) "
            +
            "VALUES (#{shopId}, #{alertType}, #{severity}, #{source}, #{title}, #{content}, #{dedupKey}, #{recipientUserid}, #{sendStatus}, #{errorMessage}, #{sentTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(NotificationLog log);

    @Update("UPDATE notification_log SET send_status = #{sendStatus}, error_message = #{errorMessage}, sent_time = #{sentTime} WHERE id = #{id}")
    int updateStatus(NotificationLog log);

    @Select("SELECT COUNT(*) FROM notification_log WHERE dedup_key = #{dedupKey}")
    int countByDedupKey(@Param("dedupKey") String dedupKey);

    @Select("SELECT * FROM notification_log ORDER BY created_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<NotificationLog> findRecent(@Param("offset") int offset, @Param("limit") int limit);
}
