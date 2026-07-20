package com.kyx.service.biz.dal.dataobject.work;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;
import java.math.BigDecimal;

@TableName(value = "business_work_requirement", autoResultMap = true)
@KeySequence("business_work_requirement_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkRequirementDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long parentId;
    private Long rootId;

    @TableField("requirement_level")
    private Integer level;

    @TableField("tree_path")
    private String path;

    private Integer childCount;

    private String title;
    private String description;
    private Integer priority;
    private Integer status;
    private String processInstanceId;
    private Integer approvalStatus;
    private String proposerDept;
    private String targetDept;
    private String proposerName;
    private Long proposerUserId;
    private Long assigneeUserId;
    private String assigneeName;
    private Date expectedFinishDate;
    private Integer estimatedUserCount;
    private Date submitTestTime;
    private Date testPassTime;
    private Date acceptedTime;
    private Date closeTime;
    private Integer previousStatus;
    private String lastRejectReason;
    private BigDecimal integral;
    private String useType;
    private String sourceIp;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> attachmentUrls;

    @TableField(exist = false)
    private Integer commentCount;

    @TableField(exist = false)
    private Integer commentUnreadCount;

    @TableField(exist = false)
    private List<WorkRequirementDeveloperDO> developerMembers;

    @TableField(exist = false)
    private List<Long> collaboratorUserIds;

    @TableField(exist = false)
    private String collaboratorNames;

    @TableField(exist = false)
    private String parentTitle;

    @TableField(exist = false)
    private Long parentProposerUserId;

}
