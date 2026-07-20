package com.kyx.service.hr.dal.dataobject.exam;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * HR 考试试卷 DO
 *
 * @author MK
 */
@TableName("hr_exam_paper")
@KeySequence("hr_exam_paper_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ExamPaperDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long examId;

    private String name;

    private String version;

    private Integer totalScore;

    private String ruleJson;

}
