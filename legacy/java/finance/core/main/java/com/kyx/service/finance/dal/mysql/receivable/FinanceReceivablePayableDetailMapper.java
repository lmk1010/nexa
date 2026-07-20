package com.kyx.service.finance.dal.mysql.receivable;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.finance.controller.admin.receivable.vo.FinanceReceivablePayableWriteOffPageReqVO;
import com.kyx.service.finance.dal.dataobject.receivable.FinanceReceivablePayableDetailDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * 往来账核销明细 Mapper
 *
 * @author xyang
 */
@Mapper
public interface FinanceReceivablePayableDetailMapper extends BaseMapperX<FinanceReceivablePayableDetailDO> {

    default PageResult<FinanceReceivablePayableDetailDO> selectPage(FinanceReceivablePayableWriteOffPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<FinanceReceivablePayableDetailDO>()
                .eqIfPresent(FinanceReceivablePayableDetailDO::getCompanyId, reqVO.getCompanyId())
                .eqIfPresent(FinanceReceivablePayableDetailDO::getArpId, reqVO.getArpId())
                .eqIfPresent(FinanceReceivablePayableDetailDO::getTransactionId, reqVO.getTransactionId())
                .betweenIfPresent(FinanceReceivablePayableDetailDO::getWriteOffDate, reqVO.getWriteOffDate())
                .orderByDesc(FinanceReceivablePayableDetailDO::getWriteOffDate)
                .orderByDesc(FinanceReceivablePayableDetailDO::getId)
                .orderByDesc(BaseDO::getCreateTime));
    }

    BigDecimal selectWriteOffAmountSumByTransactionId(@Param("companyId") Long companyId,
                                                      @Param("transactionId") Long transactionId);

    default boolean existsByWriteOffNo(Long companyId, String writeOffNo) {
        if (companyId == null || !StringUtils.hasText(writeOffNo)) {
            return false;
        }
        return selectCount(new LambdaQueryWrapperX<FinanceReceivablePayableDetailDO>()
                .eq(FinanceReceivablePayableDetailDO::getCompanyId, companyId)
                .eq(FinanceReceivablePayableDetailDO::getWriteOffNo, writeOffNo)) > 0;
    }

    default boolean existsByArpId(Long companyId, Long arpId) {
        if (companyId == null || arpId == null) {
            return false;
        }
        return selectCount(new LambdaQueryWrapperX<FinanceReceivablePayableDetailDO>()
                .eq(FinanceReceivablePayableDetailDO::getCompanyId, companyId)
                .eq(FinanceReceivablePayableDetailDO::getArpId, arpId)) > 0;
    }
}
