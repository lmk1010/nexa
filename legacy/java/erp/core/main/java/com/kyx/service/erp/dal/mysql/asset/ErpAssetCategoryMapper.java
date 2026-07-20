package com.kyx.service.erp.dal.mysql.asset;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.erp.controller.admin.asset.vo.category.ErpAssetCategoryPageReqVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetCategoryDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * ERP 资产分类 Mapper
 *
 * @author kyx
 */
@Mapper
public interface ErpAssetCategoryMapper extends BaseMapperX<ErpAssetCategoryDO> {

    default PageResult<ErpAssetCategoryDO> selectPage(ErpAssetCategoryPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpAssetCategoryDO>()
                .likeIfPresent(ErpAssetCategoryDO::getName, reqVO.getName())
                .likeIfPresent(ErpAssetCategoryDO::getCode, reqVO.getCode())
                .eqIfPresent(ErpAssetCategoryDO::getParentId, reqVO.getParentId())
                .eqIfPresent(ErpAssetCategoryDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(ErpAssetCategoryDO::getCreateTime, reqVO.getCreateTime())
                .orderByAsc(ErpAssetCategoryDO::getSort));
    }

    /**
     * 使用IPage分页查询资产分类
     */
    default IPage<ErpAssetCategoryDO> selectPageWithIPage(IPage<ErpAssetCategoryDO> page, @Param("ew") ErpAssetCategoryPageReqVO reqVO) {
        return selectPage(page, new LambdaQueryWrapperX<ErpAssetCategoryDO>()
                .likeIfPresent(ErpAssetCategoryDO::getName, reqVO.getName())
                .likeIfPresent(ErpAssetCategoryDO::getCode, reqVO.getCode())
                .eqIfPresent(ErpAssetCategoryDO::getParentId, reqVO.getParentId())
                .eqIfPresent(ErpAssetCategoryDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(ErpAssetCategoryDO::getCreateTime, reqVO.getCreateTime())
                .orderByAsc(ErpAssetCategoryDO::getSort));
    }

    default ErpAssetCategoryDO selectByCode(String code) {
        return selectOne(ErpAssetCategoryDO::getCode, code);
    }

    default ErpAssetCategoryDO selectByName(String name) {
        return selectOne(ErpAssetCategoryDO::getName, name);
    }

    default ErpAssetCategoryDO selectByParentIdAndName(Long parentId, String name) {
        return selectOne(ErpAssetCategoryDO::getParentId, parentId, ErpAssetCategoryDO::getName, name);
    }

    default List<ErpAssetCategoryDO> selectListByParentId(Long parentId) {
        return selectList(ErpAssetCategoryDO::getParentId, parentId);
    }

    default List<ErpAssetCategoryDO> selectListByStatus(Integer status) {
        return selectList(ErpAssetCategoryDO::getStatus, status);
    }

} 