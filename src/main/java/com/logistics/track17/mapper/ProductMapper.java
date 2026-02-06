package com.logistics.track17.mapper;

import com.logistics.track17.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 产品主体数据访问层
 */
@Mapper
public interface ProductMapper {

        /**
         * 插入产品
         * 
         * @param product 产品对象
         * @return 影响行数
         */
        int insert(Product product);

        /**
         * 批量插入产品
         * 
         * @param products 产品列表
         * @return 影响行数
         */
        int batchInsert(@Param("products") List<Product> products);

        /**
         * 根据ID查询产品
         * 
         * @param id 产品ID
         * @return 产品对象
         */
        Product selectById(@Param("id") Long id);

        /**
         * 根据handle查询产品
         * 
         * @param handle 产品唯一标识
         * @return 产品对象
         */
        Product selectByHandle(@Param("handle") String handle);

        /**
         * 更新产品
         * 
         * @param product 产品对象
         * @return 影响行数
         */
        int update(Product product);

        /**
         * 批量更新产品状态
         * 
         * @param ids       产品ID列表
         * @param published 上架状态
         * @return 影响行数
         */
        int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("published") Integer published);

        /**
         * 批量更新产品标签
         * 
         * @param ids  产品ID列表
         * @param tags 标签
         * @return 影响行数
         */
        int batchUpdateTags(@Param("ids") List<Long> ids, @Param("tags") String tags);

        /**
         * 删除产品
         * 
         * @param id 产品ID
         * @return 影响行数
         */
        int deleteById(@Param("id") Long id);

        /**
         * 批量删除产品
         * 
         * @param ids 产品ID列表
         * @return 影响行数
         */
        int batchDeleteByIds(@Param("ids") List<Long> ids);

        /**
         * 分页查询产品列表
         * 
         * @param title     产品标题 (模糊查询)
         * @param tags      标签 (模糊查询)
         * @param published 上架状态
         * @param shopId    所属商店ID (通过product_shops表关联)
         * @param offset    偏移量
         * @param limit     每页数量
         * @return 产品列表
         */
        List<Product> selectByPage(@Param("title") String title,
                        @Param("tags") String tags,
                        @Param("published") Integer published,
                        @Param("shopId") Long shopId,
                        @Param("offset") Integer offset,
                        @Param("limit") Integer limit);

        /**
         * 统计产品总数
         * 
         * @param title     产品标题 (模糊查询)
         * @param tags      标签 (模糊查询)
         * @param published 上架状态
         * @param shopId    所属商店ID
         * @return 总数
         */
        int countByFilters(@Param("title") String title,
                        @Param("tags") String tags,
                        @Param("published") Integer published,
                        @Param("shopId") Long shopId);

        /**
         * 查询采购管理列表（分页+摘要）
         *
         * @param params 查询参数（包含keyword, status, offset, pageSize）
         * @return 产品摘要列表
         */
        List<com.logistics.track17.dto.ProductProcurementSummaryDTO> selectProcurementList(
                        java.util.Map<String, Object> params);

        /**
         * 统计采购管理列表总数
         *
         * @param params 查询参数（包含keyword, status）
         * @return 总数
         */
        long countProcurementList(java.util.Map<String, Object> params);

        /**
         * 统计采购管理各状态数量
         *
         * @param keyword 搜索关键词
         * @return 统计结果
         */
        com.logistics.track17.dto.ProductProcurementStatsDTO selectProcurementStats(@Param("keyword") String keyword);

        /**
         * 查询所有标签
         */
        List<String> selectAllTags();
}
