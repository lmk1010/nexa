package com.kyx.service.biz.dal.dataobject.work;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@TableName("business_work_requirement_comment_read")
@KeySequence("business_work_requirement_comment_read_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkRequirementCommentReadDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long commentId;
    private Long userId;
    private Date readTime;

}
