package com.logistics.track17.mapper;

import com.logistics.track17.entity.NotificationRecipient;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 通知接收人Mapper
 */
@Mapper
public interface NotificationRecipientMapper {

    @Select("SELECT * FROM notification_recipient WHERE is_enabled = 1")
    List<NotificationRecipient> findAllEnabled();

    @Select("SELECT * FROM notification_recipient ORDER BY created_time DESC")
    List<NotificationRecipient> findAll();

    @Select("SELECT * FROM notification_recipient WHERE id = #{id}")
    NotificationRecipient findById(@Param("id") Long id);

    @Insert("INSERT INTO notification_recipient (name, dingtalk_userid, alert_types, is_enabled, remark) " +
            "VALUES (#{name}, #{dingtalkUserid}, #{alertTypes}, #{isEnabled}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(NotificationRecipient recipient);

    @Update("UPDATE notification_recipient SET name = #{name}, dingtalk_userid = #{dingtalkUserid}, " +
            "alert_types = #{alertTypes}, is_enabled = #{isEnabled}, remark = #{remark} WHERE id = #{id}")
    int update(NotificationRecipient recipient);

    @Delete("DELETE FROM notification_recipient WHERE id = #{id}")
    int delete(@Param("id") Long id);
}
