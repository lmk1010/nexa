package com.kyx.service.hr.dal.dataobject.exam;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * HR 考试试卷题目 DO
 *
 * @author MK
 */
@TableName("hr_exam_paper_item")
@KeySequence("hr_exam_paper_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ExamPaperItemDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long paperId;

    private Integer sortNo;

    private String title;

    /**
     * 题型(single/multi/judge/blank/short)
     */
    private String itemType;

    private String optionsJson;

    private String answerJson;

    private Integer score;

    private Boolean required;

}
