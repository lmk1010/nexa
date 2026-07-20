package com.kyx.service.hr.dal.dataobject.exam;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * HR 考试发布配置 DO
 *
 * @author MK
 */
@TableName("hr_exam_publish")
@KeySequence("hr_exam_publish_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ExamPublishDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long examId;

    /**
     * 发布类型(0一次性 1定期)
     */
    private Integer publishType;

    private String publishScopeJson;

    /**
     * 发送方式(0立即 1定时)
     */
    private Integer sendType;

    private LocalDateTime sendAt;

    /**
     * 定期规则JSON
     */
    private String repeatRuleJson;

    /**
     * 结束方式(0永不 1指定日期 2指定次数)
     */
    private Integer repeatEndType;

    private LocalDateTime repeatEndAt;

    private Integer repeatEndCount;

    private LocalDateTime nextPublishAt;

    private Integer generatedCount;

    /**
     * 父发布ID(子批次才有值)
     */
    private Long parentPublishId;

    private Integer batchNo;

    private String batchLabel;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private Integer durationMin;

    private Integer maxAttempts;

    private String gradeMode;

    private String remindRuleJson;

    /**
     * 状态(0未发布 1进行中 2已截止 3已暂停)
     */
    private Integer status;

}
