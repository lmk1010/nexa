package com.kyx.service.hr.dal.dataobject.onboarding;

import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 入职申请附件表
 *
 * @author MK
 */
@TableName("hr_onboarding_attachment")
@KeySequence("hr_onboarding_attachment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class OnboardingAttachmentDO extends TenantBaseDO {

    /**
     * ID
     */
    @TableId
    private Long id;
    
    /**
     * 入职申请ID
     */
    private Long onboardingId;
    
    /**
     * 附件类型（1身份证 2学历证书 3资格证书 4其他）
     */
    private Integer attachmentType;
    
    /**
     * 附件名称
     */
    private String attachmentName;
    
    /**
     * 附件地址
     */
    private String attachmentUrl;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 文件类型
     */
    private String fileType;

} 