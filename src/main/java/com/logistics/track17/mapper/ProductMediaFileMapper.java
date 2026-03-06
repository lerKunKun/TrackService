package com.logistics.track17.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.logistics.track17.entity.ProductMediaFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProductMediaFileMapper extends BaseMapper<ProductMediaFile> {

    /**
     * 按产品批量统计各分类文件数量，避免 N+1
     * 返回 List of {product_id, category, cnt}
     */
    @Select("<script>" +
            "SELECT product_id, category, COUNT(*) AS cnt " +
            "FROM product_media_files " +
            "WHERE product_id IN " +
            "<foreach item='id' collection='productIds' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach> " +
            "GROUP BY product_id, category" +
            "</script>")
    List<Map<String, Object>> countByProductIds(@Param("productIds") List<Long> productIds);

    /**
     * 获取某产品某分类下的最大 sort_order
     */
    @Select("SELECT COALESCE(MAX(sort_order), 0) FROM product_media_files " +
            "WHERE product_id = #{productId} AND category = #{category}")
    int getMaxSortOrder(@Param("productId") Long productId, @Param("category") String category);
}
