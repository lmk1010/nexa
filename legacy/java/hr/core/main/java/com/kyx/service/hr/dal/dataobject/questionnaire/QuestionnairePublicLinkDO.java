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
 * 问卷公开链接 DO
 *
 * @author MK
 */
@TableName("hr_questionnaire_public_link")
@KeySequence("hr_questionnaire_public_link_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class QuestionnairePublicLinkDO extends TenantBaseDO {

    @TableId
    private Long id;

    /** 发布ID */
    private Long publishId;

    /** 问卷ID */
    private Long questionnaireId;

    /** 访问令牌 */
    private String token;

    /** 链接标题 */
    private String title;

    /** 是否启用 0-禁用 1-启用 */
    private Integer enabled;

    /** 过期时间 */
    private LocalDateTime expireTime;

    /** 最大提交数，0表示不限 */
    private Integer maxSubmit;

    /** 已提交数 */
    private Integer submitCount;

    /** 是否收集填写人信息 0-匿名 1-收集 */
    private Integer collectInfo;

    /** 访问密码 */
    private String password;

}
