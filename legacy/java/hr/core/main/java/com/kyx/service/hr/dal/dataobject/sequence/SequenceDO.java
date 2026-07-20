package com.kyx.service.hr.dal.dataobject.sequence;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 序列管理 DO
 *
 * @author MK
 */
@TableName("hr_sequence")
@KeySequence("hr_sequence_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SequenceDO extends BaseDO {

    /**
     * 序列ID
     */
    @TableId
    private Long id;
    
    /**
     * 序列名称
     */
    private String sequenceName;
    
    /**
     * 序列描述
     */
    private String description;
    
    /**
     * 上级序列ID
     */
    private Long parentId;
    
    /**
     * 序列层级
     */
    private Integer level;
    
    /**
     * 显示排序
     */
    private Integer sort;
    
    /**
     * 状态 - 0:启用 1:禁用
     */
    private Integer status;

} 