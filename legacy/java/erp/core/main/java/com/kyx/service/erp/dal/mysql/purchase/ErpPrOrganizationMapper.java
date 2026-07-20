package com.kyx.service.erp.dal.mysql.purchase;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.erp.controller.admin.purchase.vo.prorganization.ErpPrOrganizationPageReqVO;
import com.kyx.service.erp.dal.dataobject.purchase.ErpPrOrganizationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 采购组织 Mapper
 *
 * @author MK
 */
@Mapper
public interface ErpPrOrganizationMapper extends BaseMapperX<ErpPrOrganizationDO> {

    default PageResult<ErpPrOrganizationDO> selectPage(ErpPrOrganizationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpPrOrganizationDO>()
                .likeIfPresent(ErpPrOrganizationDO::getName, reqVO.getName())
                .likeIfPresent(ErpPrOrganizationDO::getCode, reqVO.getCode())
                .eqIfPresent(ErpPrOrganizationDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpPrOrganizationDO::getParentId, reqVO.getParentId())
                .betweenIfPresent(ErpPrOrganizationDO::getCreateTime, reqVO.getCreateTime())
                .orderByAsc(ErpPrOrganizationDO::getSort)
                .orderByDesc(ErpPrOrganizationDO::getId));
    }

    default List<ErpPrOrganizationDO> selectListByStatus(Integer status) {
        return selectList(ErpPrOrganizationDO::getStatus, status);
    }

    default List<ErpPrOrganizationDO> selectList() {
        return selectList(new LambdaQueryWrapperX<ErpPrOrganizationDO>()
                .orderByAsc(ErpPrOrganizationDO::getSort)
                .orderByDesc(ErpPrOrganizationDO::getId));
    }

    default List<ErpPrOrganizationDO> selectListByParentId(Long parentId) {
        return selectList(ErpPrOrganizationDO::getParentId, parentId);
    }

    default ErpPrOrganizationDO selectByCode(String code) {
        return selectOne(ErpPrOrganizationDO::getCode, code);
    }

} 