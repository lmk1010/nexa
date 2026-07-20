package com.kyx.service.erp.dal.mysql.purchase;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.erp.controller.admin.purchase.vo.request.ErpPurchaseRequestPageReqVO;
import com.kyx.service.erp.dal.dataobject.purchase.ErpPurchaseRequestDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 采购申请 Mapper
 *
 * @author MK
 */
@Mapper
public interface ErpPurchaseRequestMapper extends BaseMapperX<ErpPurchaseRequestDO> {

    default PageResult<ErpPurchaseRequestDO> selectPage(ErpPurchaseRequestPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpPurchaseRequestDO>()
                .likeIfPresent(ErpPurchaseRequestDO::getRequestNo, reqVO.getRequestNo())
                .likeIfPresent(ErpPurchaseRequestDO::getTitle, reqVO.getTitle())
                .likeIfPresent(ErpPurchaseRequestDO::getApplicant, reqVO.getApplicant())
                .likeIfPresent(ErpPurchaseRequestDO::getDepartment, reqVO.getDepartment())
                .likeIfPresent(ErpPurchaseRequestDO::getContactPhone, reqVO.getContactPhone())
                .likeIfPresent(ErpPurchaseRequestDO::getBudgetAccount, reqVO.getBudgetAccount())
                .eqIfPresent(ErpPurchaseRequestDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpPurchaseRequestDO::getUrgentLevel, reqVO.getUrgentLevel())
                .betweenIfPresent(ErpPurchaseRequestDO::getApplyDate, reqVO.getApplyDate())
                .betweenIfPresent(ErpPurchaseRequestDO::getRequiredDate, reqVO.getRequiredDate())
                .geIfPresent(ErpPurchaseRequestDO::getTotalAmount, reqVO.getTotalAmountMin())
                .leIfPresent(ErpPurchaseRequestDO::getTotalAmount, reqVO.getTotalAmountMax())
                .betweenIfPresent(ErpPurchaseRequestDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ErpPurchaseRequestDO::getId));
    }

    default ErpPurchaseRequestDO selectByRequestNo(String requestNo) {
        return selectOne(ErpPurchaseRequestDO::getRequestNo, requestNo);
    }

    /**
     * 更新采购申请的流程实例ID和状态
     *
     * @param id 采购申请ID
     * @param processInstanceId 流程实例ID
     * @param status 业务状态
     * @param bmpStatus BMP流程状态
     * @return 更新行数
     */
    int updateProcessInstanceAndStatus(Long id, String processInstanceId, Integer status, Integer bmpStatus);
} 