package com.kyx.service.finance.dal.mysql.report;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.finance.dal.dataobject.report.FinanceReportSnapshotDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

/**
 * 报表快照 Mapper
 *
 * @author xyang
 */
@Mapper
public interface FinanceReportSnapshotMapper extends BaseMapperX<FinanceReportSnapshotDO> {

    default FinanceReportSnapshotDO selectByCompanyIdAndReportCodeAndPeriod(Long companyId, String reportCode, String snapshotPeriod) {
        if (companyId == null || !StringUtils.hasText(reportCode) || !StringUtils.hasText(snapshotPeriod)) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<FinanceReportSnapshotDO>()
                .eq(FinanceReportSnapshotDO::getCompanyId, companyId)
                .eq(FinanceReportSnapshotDO::getReportCode, StringUtils.trimWhitespace(reportCode))
                .eq(FinanceReportSnapshotDO::getSnapshotPeriod, StringUtils.trimWhitespace(snapshotPeriod))
                .orderByDesc(FinanceReportSnapshotDO::getId)
                .last("LIMIT 1"));
    }

    default int deleteByCompanyIdAndReportCodeAndPeriod(Long companyId, String reportCode, String snapshotPeriod) {
        if (companyId == null || !StringUtils.hasText(reportCode) || !StringUtils.hasText(snapshotPeriod)) {
            return 0;
        }
        return delete(new LambdaQueryWrapperX<FinanceReportSnapshotDO>()
                .eq(FinanceReportSnapshotDO::getCompanyId, companyId)
                .eq(FinanceReportSnapshotDO::getReportCode, StringUtils.trimWhitespace(reportCode))
                .eq(FinanceReportSnapshotDO::getSnapshotPeriod, StringUtils.trimWhitespace(snapshotPeriod)));
    }
}
