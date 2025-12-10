package com.logistics.track17.mapper;

import com.logistics.track17.entity.AllowedCorpId;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 允许登录的企业CorpId Mapper
 */
@Mapper
public interface AllowedCorpIdMapper {

    /**
     * 查询所有允许的CorpId
     */
    List<AllowedCorpId> selectAll();

    /**
     * 根据CorpId查询
     */
    AllowedCorpId selectByCorpId(@Param("corpId") String corpId);

    /**
     * 检查CorpId是否允许登录（启用状态）
     */
    boolean isCorpIdAllowed(@Param("corpId") String corpId);

    /**
     * 插入新的允许CorpId
     */
    int insert(AllowedCorpId allowedCorpId);

    /**
     * 软删除（设置deleted_at时间戳）
     */
    int softDeleteByCorpId(@Param("corpId") String corpId);

    /**
     * 更新状态
     */
    int updateStatus(@Param("corpId") String corpId, @Param("status") Integer status);
}
