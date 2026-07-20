package com.kyx.service.hr.dal.dataobject.questionnaire;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * HR 问卷分配 DO
 *
 * @author MK
 */
@TableName("hr_questionnaire_assignment")
@KeySequence("hr_questionnaire_assignment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class QuestionnaireAssignmentDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long questionnaireId;

    private Long publishId;

    /**
     * 批次号
     */
    private Integer batchNo;

    /**
     * 批次标签
     */
    private String batchLabel;

    /**
     * 批次开始时间
     */
    private LocalDateTime batchStartAt;

    /**
     * 批次截止时间
     */
    private LocalDateTime batchEndAt;

    private Long evaluatorId;

    private String evaluatorName;

    private Long targetId;

    private String targetName;

    private String role;

    /**
     * 状态(0待填 1已提交)
     */
    private Integer status;

}
