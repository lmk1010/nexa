package com.kyx.service.biz.dal.dataobject.todo;

import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@TableName("business_todo")
@KeySequence("business_todo_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class TodoDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long userId;

    private String title;

    private String description;

    private Date dueDate;

    private Integer priority;

    private String tags;

    private Boolean generatedFlag;

    private String businessType;

    private Long businessId;

    private String taskType;

    private String routePath;

    private Integer status;

}
