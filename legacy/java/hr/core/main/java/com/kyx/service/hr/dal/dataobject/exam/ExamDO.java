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
 * HR 考试 DO
 *
 * @author MK
 */
@TableName("hr_exam")
@KeySequence("hr_exam_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ExamDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String code;

    private String name;

    /**
     * 考试类型(0一次性 1定期考核)
     */
    private Integer examType;

    /**
     * 状态(0草稿 1已发布 2已关闭)
     */
    private Integer status;

    /**
     * 考试模式(fixed/bank)
     */
    private String examMode;

    /**
     * 考试时长(分钟)
     */
    private Integer durationMin;

    /**
     * 及格分
     */
    private Integer passScore;

    /**
     * 最大次数
     */
    private Integer maxAttempts;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    /**
     * 发布范围JSON（deptIds/roleIds/userIds）
     */
    private String publishScopeJson;

}
