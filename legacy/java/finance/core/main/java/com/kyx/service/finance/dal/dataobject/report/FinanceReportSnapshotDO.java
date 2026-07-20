package com.kyx.service.finance.dal.dataobject.report;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDate;

/**
 * 报表快照 DO
 *
 * @author xyang
 */
@TableName("finance_report_snapshot")
@KeySequence("finance_report_snapshot_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FinanceReportSnapshotDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long companyId;

    private String reportCode;

    private String snapshotPeriod;

    private LocalDate snapshotDate;

    private String dataJson;
}
