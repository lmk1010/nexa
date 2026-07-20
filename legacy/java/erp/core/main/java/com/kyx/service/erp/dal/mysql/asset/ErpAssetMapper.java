package com.kyx.service.erp.dal.mysql.asset;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.erp.controller.admin.asset.vo.asset.ErpAssetPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.overview.ErpAssetExpiringPageReqVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * ERP 资产 Mapper
 *
 * @author kyx
 */
@Mapper
public interface ErpAssetMapper extends BaseMapperX<ErpAssetDO> {

    default PageResult<ErpAssetDO> selectPage(ErpAssetPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpAssetDO> query = new LambdaQueryWrapperX<ErpAssetDO>()
                .likeIfPresent(ErpAssetDO::getName, reqVO.getName())
                .likeIfPresent(ErpAssetDO::getAssetNo, reqVO.getAssetNo())
                .eqIfPresent(ErpAssetDO::getType, reqVO.getType())
                .eqIfPresent(ErpAssetDO::getCategoryId, reqVO.getCategoryId())
                .eqIfPresent(ErpAssetDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(ErpAssetDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(ErpAssetDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ErpAssetDO::getId);
        
        // 如果需要排除已领用的资产，则添加NOT EXISTS子查询
        if (Boolean.TRUE.equals(reqVO.getExcludeCheckedOut())) {
            query.notExists("SELECT 1 FROM erp_asset_checkout c WHERE c.asset_id = erp_asset.id AND (c.status = 0 OR c.status = 1) AND c.deleted = 0");
        }
        
        return selectPage(reqVO, query);
    }

    default Long selectCountByCategoryId(Long categoryId) {
        return selectCount(ErpAssetDO::getCategoryId, categoryId);
    }

    default Long selectCountByDeptId(Long deptId) {
        return selectCount(ErpAssetDO::getDeptId, deptId);
    }

    default List<ErpAssetDO> selectListByStatus(Integer status) {
        return selectList(ErpAssetDO::getStatus, status);
    }

    default ErpAssetDO selectByAssetNo(String assetNo) {
        return selectOne(ErpAssetDO::getAssetNo, assetNo);
    }

    // ========== 资产纵览统计相关方法 ==========

    /**
     * 根据条件统计资产数量
     */
    @Select("<script>" +
            "SELECT COUNT(*) " +
            "FROM erp_asset a " +
            "<if test='params.userId != null'>" +
            "LEFT JOIN erp_asset_ownership o ON a.id = o.asset_id AND o.status = 1 AND o.deleted = 0 " +
            "</if>" +
            "WHERE a.deleted = 0 " +
            "<if test='params.deptId != null'>AND a.dept_id = #{params.deptId}</if> " +
            "<if test='params.userId != null'>AND o.current_user_id = #{params.userId}</if> " +
            "<if test='params.categoryId != null'>AND a.category_id = #{params.categoryId}</if> " +
            "<if test='params.status != null'>AND a.status = #{params.status}</if> " +
            "<if test='params.warrantyDateStart != null'>AND a.warranty_date &gt;= #{params.warrantyDateStart}</if> " +
            "<if test='params.warrantyDateEnd != null'>AND a.warranty_date &lt;= #{params.warrantyDateEnd}</if> " +
            "</script>")
    Long selectCountByConditions(@Param("params") Map<String, Object> params);

    /**
     * 按分类统计资产数量
     */
    @Select("<script>" +
            "SELECT " +
            "a.category_id as categoryId, " +
            "COALESCE(c.name, '未分类') as categoryName, " +
            "COUNT(*) as count " +
            "FROM erp_asset a " +
            "LEFT JOIN erp_asset_category c ON a.category_id = c.id AND c.deleted = 0 " +
            "<if test='params.userId != null'>" +
            "LEFT JOIN erp_asset_ownership o ON a.id = o.asset_id AND o.status = 1 AND o.deleted = 0 " +
            "</if>" +
            "WHERE a.deleted = 0 " +
            "<if test='params.deptId != null'>AND a.dept_id = #{params.deptId}</if> " +
            "<if test='params.userId != null'>AND o.current_user_id = #{params.userId}</if> " +
            "GROUP BY a.category_id, c.name " +
            "ORDER BY count DESC" +
            "</script>")
    List<Map<String, Object>> selectCategoryStatistics(@Param("params") Map<String, Object> params);

    /**
     * 按部门统计资产数量
     */
    @Select("<script>" +
            "SELECT " +
            "dept_id as deptId, " +
            "COUNT(*) as count " +
            "FROM erp_asset " +
            "WHERE deleted = 0 " +
            "<if test='params.categoryId != null'>AND category_id = #{params.categoryId}</if> " +
            "GROUP BY dept_id " +
            "ORDER BY count DESC" +
            "</script>")
    List<Map<String, Object>> selectDeptStatistics(@Param("params") Map<String, Object> params);

    /**
     * 按用户统计资产数量
     */
    @Select("<script>" +
            "SELECT " +
            "o.current_user_id as userId, " +
            "COUNT(*) as count " +
            "FROM erp_asset a " +
            "LEFT JOIN erp_asset_ownership o ON a.id = o.asset_id AND o.status = 1 AND o.deleted = 0 " +
            "WHERE a.deleted = 0 " +
            "<if test='params.deptId != null'>AND a.dept_id = #{params.deptId}</if> " +
            "<if test='params.categoryId != null'>AND a.category_id = #{params.categoryId}</if> " +
            "GROUP BY o.current_user_id " +
            "ORDER BY count DESC" +
            "</script>")
    List<Map<String, Object>> selectUserStatistics(@Param("params") Map<String, Object> params);

    /**
     * 按状态统计资产数量
     */
    @Select("<script>" +
            "SELECT " +
            "a.status, " +
            "COUNT(*) as count " +
            "FROM erp_asset a " +
            "<if test='params.userId != null'>" +
            "LEFT JOIN erp_asset_ownership o ON a.id = o.asset_id AND o.status = 1 AND o.deleted = 0 " +
            "</if>" +
            "WHERE a.deleted = 0 " +
            "<if test='params.deptId != null'>AND a.dept_id = #{params.deptId}</if> " +
            "<if test='params.userId != null'>AND o.current_user_id = #{params.userId}</if> " +
            "<if test='params.categoryId != null'>AND a.category_id = #{params.categoryId}</if> " +
            "GROUP BY a.status " +
            "ORDER BY a.status" +
            "</script>")
    List<Map<String, Object>> selectStatusStatistics(@Param("params") Map<String, Object> params);

    /**
     * 分页查询快过期资产
     */
    @Select("<script>" +
            "SELECT a.* " +
            "FROM erp_asset a " +
            "<if test='params.userId != null'>" +
            "LEFT JOIN erp_asset_ownership o ON a.id = o.asset_id AND o.status = 1 AND o.deleted = 0 " +
            "</if>" +
            "WHERE a.deleted = 0 " +
            "AND a.warranty_date IS NOT NULL " +
            "<if test='params.deptId != null'>AND a.dept_id = #{params.deptId}</if> " +
            "<if test='params.userId != null'>AND o.current_user_id = #{params.userId}</if> " +
            "<if test='params.categoryId != null'>AND a.category_id = #{params.categoryId}</if> " +
            "<if test='params.warrantyDateEnd != null'>AND a.warranty_date &lt;= #{params.warrantyDateEnd}</if> " +
            "ORDER BY a.warranty_date ASC " +
            "LIMIT #{pageReqVO.pageSize} OFFSET #{offset}" +
            "</script>")
    List<ErpAssetDO> selectExpiringAssetsList(@Param("pageReqVO") ErpAssetExpiringPageReqVO pageReqVO, 
                                              @Param("params") Map<String, Object> params,
                                              @Param("offset") long offset);

    /**
     * 统计快过期资产总数
     */
    @Select("<script>" +
            "SELECT COUNT(*) " +
            "FROM erp_asset a " +
            "<if test='params.userId != null'>" +
            "LEFT JOIN erp_asset_ownership o ON a.id = o.asset_id AND o.status = 1 AND o.deleted = 0 " +
            "</if>" +
            "WHERE a.deleted = 0 " +
            "AND a.warranty_date IS NOT NULL " +
            "<if test='params.deptId != null'>AND a.dept_id = #{params.deptId}</if> " +
            "<if test='params.userId != null'>AND o.current_user_id = #{params.userId}</if> " +
            "<if test='params.categoryId != null'>AND a.category_id = #{params.categoryId}</if> " +
            "<if test='params.warrantyDateEnd != null'>AND a.warranty_date &lt;= #{params.warrantyDateEnd}</if> " +
            "</script>")
    Long selectExpiringAssetsCount(@Param("params") Map<String, Object> params);

    /**
     * 分页查询快过期资产（使用自定义SQL）
     */
    default PageResult<ErpAssetDO> selectExpiringAssetsPage(ErpAssetExpiringPageReqVO reqVO, Map<String, Object> params) {
        long offset = (long) (reqVO.getPageNo() - 1) * reqVO.getPageSize();
        List<ErpAssetDO> list = selectExpiringAssetsList(reqVO, params, offset);
        Long total = selectExpiringAssetsCount(params);
        return new PageResult<>(list, total);
    }

} 