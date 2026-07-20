package com.kyx.service.hr.dal.dataobject.employee;

import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 员工附件信息表
 *
 * @author MK
 */
@TableName("hr_employee_attachment")
@KeySequence("hr_employee_attachment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeAttachmentDO extends TenantBaseDO {

    /**
     * ID
     */
    @TableId
    private Long id;
    
    /**
     * 员工档案ID
     */
    private Long profileId;
    
    /**
     * 附件类型
     */
    private String attachmentType;
    
    /**
     * 附件名称
     */
    private String attachmentName;
    
    /**
     * 文件地址
     */
    private String fileUrl;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 文件类型
     */
    private String fileType;
    
    /**
     * 备注
     */
    private String remark;
    
} 