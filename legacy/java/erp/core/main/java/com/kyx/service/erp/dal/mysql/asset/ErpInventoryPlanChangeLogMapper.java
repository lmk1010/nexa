package com.kyx.service.erp.dal.mysql.asset;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.erp.dal.dataobject.asset.ErpInventoryPlanChangeLogDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 盘点计划变更日志 Mapper
 *
 * @author kyx
 */
@Mapper
public interface ErpInventoryPlanChangeLogMapper extends BaseMapperX<ErpInventoryPlanChangeLogDO> {

    /**
     * 根据盘点计划ID获取变更日志列表
     *
     * @param planId 盘点计划ID
     * @return 变更日志列表
     */
    default List<ErpInventoryPlanChangeLogDO> selectListByPlanId(Long planId) {
        return selectList(new LambdaQueryWrapperX<ErpInventoryPlanChangeLogDO>()
                .eq(ErpInventoryPlanChangeLogDO::getPlanId, planId)
                .orderByDesc(ErpInventoryPlanChangeLogDO::getOperationTime));
    }

    /**
     * 根据盘点计划编号获取变更日志列表
     *
     * @param planNo 盘点计划编号
     * @return 变更日志列表
     */
    default List<ErpInventoryPlanChangeLogDO> selectListByPlanNo(String planNo) {
        return selectList(new LambdaQueryWrapperX<ErpInventoryPlanChangeLogDO>()
                .eq(ErpInventoryPlanChangeLogDO::getPlanNo, planNo)
                .orderByDesc(ErpInventoryPlanChangeLogDO::getOperationTime));
    }

    /**
     * 根据盘点计划ID和变更类型获取变更日志列表
     *
     * @param planId     盘点计划ID
     * @param changeType 变更类型
     * @return 变更日志列表
     */
    default List<ErpInventoryPlanChangeLogDO> selectListByPlanIdAndChangeType(Long planId, String changeType) {
        return selectList(new LambdaQueryWrapperX<ErpInventoryPlanChangeLogDO>()
                .eq(ErpInventoryPlanChangeLogDO::getPlanId, planId)
                .eq(ErpInventoryPlanChangeLogDO::getChangeType, changeType)
                .orderByDesc(ErpInventoryPlanChangeLogDO::getOperationTime));
    }

} 