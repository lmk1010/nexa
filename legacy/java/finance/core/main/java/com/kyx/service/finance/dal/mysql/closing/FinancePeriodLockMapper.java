package com.kyx.service.finance.dal.mysql.closing;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.finance.dal.dataobject.closing.FinancePeriodLockDO;
import com.kyx.service.finance.enums.FinancePeriodLockStatusEnum;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

/**
 * 期间锁定 Mapper
 * @author xyang
 */
@Mapper
public interface FinancePeriodLockMapper extends BaseMapperX<FinancePeriodLockDO> {

    default FinancePeriodLockDO selectByCompanyIdAndPeriod(Long companyId, String period) {
        if (companyId == null || !StringUtils.hasText(period)) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<FinancePeriodLockDO>()
                .eq(FinancePeriodLockDO::getCompanyId, companyId)
                .eq(FinancePeriodLockDO::getPeriod, StringUtils.trimWhitespace(period))
                .last("LIMIT 1"));
    }

    default boolean existsLocked(Long companyId, String period) {
        if (companyId == null || !StringUtils.hasText(period)) {
            return false;
        }
        return selectCount(new LambdaQueryWrapperX<FinancePeriodLockDO>()
                .eq(FinancePeriodLockDO::getCompanyId, companyId)
                .eq(FinancePeriodLockDO::getPeriod, StringUtils.trimWhitespace(period))
                .eq(FinancePeriodLockDO::getLockStatus, FinancePeriodLockStatusEnum.LOCKED.name())) > 0;
    }
}
