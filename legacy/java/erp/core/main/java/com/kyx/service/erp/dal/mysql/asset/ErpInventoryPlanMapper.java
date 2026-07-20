package com.kyx.service.erp.dal.mysql.asset;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.erp.controller.admin.asset.vo.inventory.ErpInventoryPlanPageReqVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpInventoryPlanDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 盘点计划 Mapper
 *
 * @author kyx
 */
@Mapper
public interface ErpInventoryPlanMapper extends BaseMapperX<ErpInventoryPlanDO> {

    default PageResult<ErpInventoryPlanDO> selectPage(ErpInventoryPlanPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpInventoryPlanDO>()
                .likeIfPresent(ErpInventoryPlanDO::getPlanName, reqVO.getPlanName())
                .likeIfPresent(ErpInventoryPlanDO::getPlanNo, reqVO.getPlanNo())
                .eqIfPresent(ErpInventoryPlanDO::getPlanType, reqVO.getPlanType())
                .eqIfPresent(ErpInventoryPlanDO::getMethod, reqVO.getMethod())
                .eqIfPresent(ErpInventoryPlanDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpInventoryPlanDO::getResponsiblePersonId, reqVO.getResponsiblePersonId())
                .betweenIfPresent(ErpInventoryPlanDO::getStartTime, reqVO.getStartTime())
                .betweenIfPresent(ErpInventoryPlanDO::getEndTime, reqVO.getEndTime())
                .betweenIfPresent(ErpInventoryPlanDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ErpInventoryPlanDO::getId));
    }

    default List<ErpInventoryPlanDO> selectList(ErpInventoryPlanPageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<ErpInventoryPlanDO>()
                .likeIfPresent(ErpInventoryPlanDO::getPlanName, reqVO.getPlanName())
                .likeIfPresent(ErpInventoryPlanDO::getPlanNo, reqVO.getPlanNo())
                .eqIfPresent(ErpInventoryPlanDO::getPlanType, reqVO.getPlanType())
                .eqIfPresent(ErpInventoryPlanDO::getMethod, reqVO.getMethod())
                .eqIfPresent(ErpInventoryPlanDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpInventoryPlanDO::getResponsiblePersonId, reqVO.getResponsiblePersonId())
                .betweenIfPresent(ErpInventoryPlanDO::getStartTime, reqVO.getStartTime())
                .betweenIfPresent(ErpInventoryPlanDO::getEndTime, reqVO.getEndTime())
                .betweenIfPresent(ErpInventoryPlanDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ErpInventoryPlanDO::getId));
    }

    default List<ErpInventoryPlanDO> selectListByStatus(Integer status) {
        return selectList(ErpInventoryPlanDO::getStatus, status);
    }

    default List<ErpInventoryPlanDO> selectListByResponsiblePersonId(Long responsiblePersonId) {
        return selectList(ErpInventoryPlanDO::getResponsiblePersonId, responsiblePersonId);
    }

    default ErpInventoryPlanDO selectByPlanNo(String planNo) {
        return selectOne(ErpInventoryPlanDO::getPlanNo, planNo);
    }

    default Long selectCountByStatus(Integer status) {
        return selectCount(ErpInventoryPlanDO::getStatus, status);
    }

    default List<ErpInventoryPlanDO> selectListByPlanType(String planType) {
        return selectList(ErpInventoryPlanDO::getPlanType, planType);
    }

    default List<ErpInventoryPlanDO> selectListByTimeRange(ErpInventoryPlanPageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<ErpInventoryPlanDO>()
                .betweenIfPresent(ErpInventoryPlanDO::getStartTime, reqVO.getStartTime())
                .betweenIfPresent(ErpInventoryPlanDO::getEndTime, reqVO.getEndTime())
                .orderByDesc(ErpInventoryPlanDO::getStartTime));
    }

    default List<ErpInventoryPlanDO> selectActiveExecutionPlans() {
        return selectList(new LambdaQueryWrapperX<ErpInventoryPlanDO>()
                .in(ErpInventoryPlanDO::getStatus, 1, 2) // 已发布状态和进行中状态都可以扫码
                .orderByAsc(ErpInventoryPlanDO::getStartTime));
    }

} 