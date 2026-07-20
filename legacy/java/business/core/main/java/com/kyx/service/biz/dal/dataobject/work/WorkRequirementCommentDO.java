package com.kyx.service.biz.dal.dataobject.work;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@TableName(value = "business_work_requirement_comment", autoResultMap = true)
@KeySequence("business_work_requirement_comment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkRequirementCommentDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long requirementId;
    private String commentType;
    private String content;
    private Long fromUserId;
    private String fromUserName;
    private Long targetUserId;
    private String targetUserName;
    private String ip;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> attachmentUrls;

    @TableField(exist = false)
    private Boolean readStatus;

}
