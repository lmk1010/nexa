package com.kyx.service.finance.dal.mysql.init;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.finance.controller.admin.init.vo.account.FinanceAccountPageReqVO;
import com.kyx.service.finance.dal.dataobject.init.FinanceAccountDO;
import com.kyx.service.finance.service.dashboard.dto.FinanceDashboardChannelBalanceSummaryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/**
 * 资金账户表 Mapper
 *
 * @author xyang
 */
@Mapper
public interface FinanceAccountMapper extends BaseMapperX<FinanceAccountDO> {

    BigDecimal selectDashboardAccountBalanceByCompanyUsageScopeAndPeriod(@Param("companyId") Long companyId,
                                                                         @Param("period") String period,
                                                                         @Param("successStatus") String successStatus,
                                                                         @Param("incomeType") String incomeType,
                                                                         @Param("expenseType") String expenseType);

    List<FinanceDashboardChannelBalanceSummaryDTO> selectDashboardChannelBalanceByCompanyUsageScopeAndPeriod(
            @Param("companyId") Long companyId,
            @Param("period") String period,
            @Param("successStatus") String successStatus,
            @Param("incomeType") String incomeType,
            @Param("expenseType") String expenseType,
            @Param("transferType") String transferType,
            @Param("bankType") String bankType,
            @Param("alipayType") String alipayType,
            @Param("wechatType") String wechatType);

    default PageResult<FinanceAccountDO> selectPage(FinanceAccountPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<FinanceAccountDO>()
                .eqIfPresent(FinanceAccountDO::getAccountType, reqVO.getAccountType())
                .eqIfPresent(FinanceAccountDO::getStatus, reqVO.getStatus())
                .likeIfPresent(FinanceAccountDO::getAccountAlias, StringUtils.trimWhitespace(reqVO.getAccountAlias()))
                .likeIfPresent(FinanceAccountDO::getBankName, StringUtils.trimWhitespace(reqVO.getBankName()))
                .likeIfPresent(FinanceAccountDO::getAccountTagText, StringUtils.trimWhitespace(reqVO.getAccountTagText()))
                .orderByDesc(BaseDO::getCreateTime));
    }

    default int increaseBalance(Long accountId, BigDecimal amount) {
        return update(null, new LambdaUpdateWrapper<FinanceAccountDO>()
                .eq(FinanceAccountDO::getId, accountId)
                .setSql("balance = balance + " + amount.toPlainString()));
    }

    default int decreaseBalance(Long accountId, BigDecimal amount) {
        return update(null, new LambdaUpdateWrapper<FinanceAccountDO>()
                .eq(FinanceAccountDO::getId, accountId)
                .ge(FinanceAccountDO::getBalance, amount)
                .setSql("balance = balance - " + amount.toPlainString()));
    }

    default int updateStatusByIds(Collection<Long> ids, Integer status) {
        return update(null, new LambdaUpdateWrapper<FinanceAccountDO>()
                .in(FinanceAccountDO::getId, ids)
                .set(FinanceAccountDO::getStatus, status));
    }
}
