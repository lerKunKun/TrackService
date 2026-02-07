package com.logistics.track17.mapper;

import com.logistics.track17.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper {
    /**
     * 根据ID查询用户
     */
    User selectById(@Param("id") Long id);

    /**
     * 根据用户名查询用户
     */
    User selectByUsername(@Param("username") String username);

    /**
     * 根据邮箱查询用户
     */
    User selectByEmail(@Param("email") String email);

    /**
     * 查询所有用户列表
     */
    List<User> selectAll();

    /**
     * 分页查询用户列表
     */
    List<User> selectByPage(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 统计用户总数
     */
    Long count();

    /**
     * 插入用户
     */
    int insert(User user);

    /**
     * 更新用户信息
     */
    int update(User user);

    /**
     * 更新密码
     */
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    /**
     * 更新最后登录信息
     */
    int updateLastLogin(@Param("id") Long id,
            @Param("lastLoginTime") LocalDateTime lastLoginTime,
            @Param("lastLoginIp") String lastLoginIp);

    /**
     * 更新用户状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 根据钉钉unionId查询用户
     */
    User selectByDingUnionId(@Param("unionId") String unionId);

    /**
     * 根据钉钉userId查询用户
     */
    User selectByDingUserId(@Param("dingUserId") String dingUserId);

    /**
     * 删除用户
     */
    int deleteById(@Param("id") Long id);

    /**
     * 查询所有钉钉用户
     */
    List<User> selectAllDingtalkUsers();

    /**
     * 批量插入用户
     */
    void batchInsert(@Param("users") List<User> users);

    /**
     * 批量更新用户
     */
    void batchUpdate(@Param("users") List<User> users);

    /**
     * 查询所有启用同步的用户
     */
    List<User> selectAllSyncEnabledUsers();
}
