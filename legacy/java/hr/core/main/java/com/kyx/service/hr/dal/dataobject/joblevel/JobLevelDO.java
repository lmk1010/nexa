package com.kyx.service.hr.dal.dataobject.joblevel;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 职级管理 DO
 *
 * @author MK
 */
@TableName("hr_job_level")
@KeySequence("hr_job_level_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class JobLevelDO extends TenantBaseDO {

    /**
     * 职级ID
     */
    @TableId
    private Long id;
    
    /**
     * 职级名称
     */
    private String levelName;
    
    /**
     * 职级编码
     */
    private String levelCode;
    
    /**
     * 职级描述
     */
    private String description;
    
    /**
     * 所属序列ID
     */
    private Long sequenceId;
    
    /**
     * 显示排序
     */
    private Integer sort;
    
    /**
     * 状态 - 0:启用 1:禁用
     */
    private Integer status;

} 