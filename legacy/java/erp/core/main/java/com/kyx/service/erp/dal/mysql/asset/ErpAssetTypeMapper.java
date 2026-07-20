package com.kyx.service.erp.dal.mysql.asset;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.erp.controller.admin.asset.vo.type.ErpAssetTypePageReqVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetTypeDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * ERP 资产类型 Mapper
 *
 * @author kyx
 */
@Mapper
public interface ErpAssetTypeMapper extends BaseMapperX<ErpAssetTypeDO> {

    default PageResult<ErpAssetTypeDO> selectPage(ErpAssetTypePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpAssetTypeDO>()
                .likeIfPresent(ErpAssetTypeDO::getName, reqVO.getName())
                .likeIfPresent(ErpAssetTypeDO::getCode, reqVO.getCode())
                .eqIfPresent(ErpAssetTypeDO::getParentId, reqVO.getParentId())
                .eqIfPresent(ErpAssetTypeDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(ErpAssetTypeDO::getCreateTime, reqVO.getCreateTime())
                .orderByAsc(ErpAssetTypeDO::getSort));
    }

    /**
     * 使用IPage分页查询资产类型
     */
    default IPage<ErpAssetTypeDO> selectPageWithIPage(IPage<ErpAssetTypeDO> page, @Param("ew") ErpAssetTypePageReqVO reqVO) {
        return selectPage(page, new LambdaQueryWrapperX<ErpAssetTypeDO>()
                .likeIfPresent(ErpAssetTypeDO::getName, reqVO.getName())
                .likeIfPresent(ErpAssetTypeDO::getCode, reqVO.getCode())
                .eqIfPresent(ErpAssetTypeDO::getParentId, reqVO.getParentId())
                .eqIfPresent(ErpAssetTypeDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(ErpAssetTypeDO::getCreateTime, reqVO.getCreateTime())
                .orderByAsc(ErpAssetTypeDO::getSort));
    }

    default ErpAssetTypeDO selectByCode(String code) {
        return selectOne(ErpAssetTypeDO::getCode, code);
    }

    default ErpAssetTypeDO selectByName(String name) {
        return selectOne(ErpAssetTypeDO::getName, name);
    }

    default ErpAssetTypeDO selectByParentIdAndName(Long parentId, String name) {
        return selectOne(ErpAssetTypeDO::getParentId, parentId, ErpAssetTypeDO::getName, name);
    }

    default List<ErpAssetTypeDO> selectListByParentId(Long parentId) {
        return selectList(ErpAssetTypeDO::getParentId, parentId);
    }

    default List<ErpAssetTypeDO> selectListByStatus(Integer status) {
        return selectList(ErpAssetTypeDO::getStatus, status);
    }

} 