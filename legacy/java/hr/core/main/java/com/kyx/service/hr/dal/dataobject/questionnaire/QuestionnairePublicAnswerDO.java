package com.kyx.service.hr.dal.dataobject.questionnaire;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 问卷公开提交答案 DO
 *
 * @author MK
 */
@TableName("hr_questionnaire_public_answer")
@KeySequence("hr_questionnaire_public_answer_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class QuestionnairePublicAnswerDO extends TenantBaseDO {

    @TableId
    private Long id;

    /** 公开链接ID */
    private Long linkId;

    /** 问卷ID */
    private Long questionnaireId;

    /** 发布ID */
    private Long publishId;

    /** 填写人姓名 */
    private String respondentName;

    /** 填写人手机 */
    private String respondentPhone;

    /** 填写人邮箱 */
    private String respondentEmail;

    /** IP地址 */
    private String ipAddress;

    /** 浏览器信息 */
    private String userAgent;

    /** 提交时间 */
    private LocalDateTime submitTime;

    /** 答案JSON */
    private String answersJson;

    /** 总分 */
    private BigDecimal totalScore;

}
