package com.kyx.service.finance.dal.mysql.receivable;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.finance.controller.admin.receivable.vo.FinanceReceivablePayablePageReqVO;
import com.kyx.service.finance.dal.dataobject.receivable.FinanceReceivablePayableDO;
import com.kyx.service.finance.service.dashboard.dto.FinanceDashboardArpBalanceSummaryDTO;
import com.kyx.service.finance.service.dashboard.dto.FinanceDashboardArpContactSummaryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 往来账 Mapper
 * @author xyang
 */
@Mapper
public interface FinanceReceivablePayableMapper extends BaseMapperX<FinanceReceivablePayableDO> {

    FinanceReceivablePayableDO selectByIdForUpdate(@Param("id") Long id);

    FinanceDashboardArpBalanceSummaryDTO selectDashboardArpBalanceSummary(@Param("companyId") Long companyId,
                                                                           @Param("receivableType") String receivableType,
                                                                           @Param("payableType") String payableType,
                                                                           @Param("cancelledStatus") String cancelledStatus);

    FinanceDashboardArpBalanceSummaryDTO selectDashboardArpBalanceSummaryByBillDateRange(@Param("companyId") Long companyId,
                                                                                          @Param("receivableType") String receivableType,
                                                                                          @Param("payableType") String payableType,
                                                                                          @Param("cancelledStatus") String cancelledStatus,
                                                                                          @Param("startTime") LocalDateTime startTime,
                                                                                          @Param("endTime") LocalDateTime endTime);

    List<FinanceDashboardArpContactSummaryDTO> selectDashboardArpContactSummaryByBillDateRange(@Param("companyId") Long companyId,
                                                                                                @Param("type") String type,
                                                                                                @Param("cancelledStatus") String cancelledStatus,
                                                                                                @Param("startTime") LocalDateTime startTime,
                                                                                                @Param("endTime") LocalDateTime endTime,
                                                                                                @Param("limit") Integer limit);

    BigDecimal selectDashboardAdvanceReceiptByBillDateRange(@Param("companyId") Long companyId,
                                                            @Param("type") String type,
                                                            @Param("cancelledStatus") String cancelledStatus,
                                                            @Param("startTime") LocalDateTime startTime,
                                                            @Param("endTime") LocalDateTime endTime);

    int writeOffAtomically(@Param("id") Long id,
                           @Param("amount") BigDecimal amount,
                           @Param("paidStatus") String paidStatus,
                           @Param("partialStatus") String partialStatus,
                           @Param("cancelledStatus") String cancelledStatus);

    default PageResult<FinanceReceivablePayableDO> selectPage(FinanceReceivablePayablePageReqVO reqVO) {
        LambdaQueryWrapperX<FinanceReceivablePayableDO> queryWrapper = new LambdaQueryWrapperX<FinanceReceivablePayableDO>()
                .eqIfPresent(FinanceReceivablePayableDO::getCompanyId, reqVO.getCompanyId())
                .likeIfPresent(FinanceReceivablePayableDO::getBillNo, StringUtils.trimWhitespace(reqVO.getBillNo()))
                .eqIfPresent(FinanceReceivablePayableDO::getContactId, reqVO.getContactId())
                .eqIfPresent(FinanceReceivablePayableDO::getType, reqVO.getType())
                .eqIfPresent(FinanceReceivablePayableDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(FinanceReceivablePayableDO::getBillDate, reqVO.getBillDate());
        queryWrapper.orderByDesc(FinanceReceivablePayableDO::getId)
                .orderByDesc(BaseDO::getCreateTime);
        return selectPage(reqVO, queryWrapper);
    }

    default boolean existsByBillNo(Long companyId, String billNo, Long excludeId) {
        if (!StringUtils.hasText(billNo) || companyId == null) {
            return false;
        }
        return selectCount(new LambdaQueryWrapperX<FinanceReceivablePayableDO>()
                .eq(FinanceReceivablePayableDO::getCompanyId, companyId)
                .eq(FinanceReceivablePayableDO::getBillNo, billNo)
                .neIfPresent(FinanceReceivablePayableDO::getId, excludeId)) > 0;
    }

    default boolean existsByContactId(Long contactId) {
        if (contactId == null) {
            return false;
        }
        return selectCount(new LambdaQueryWrapperX<FinanceReceivablePayableDO>()
                .eq(FinanceReceivablePayableDO::getContactId, contactId)) > 0;
    }
}
