package com.kyx.service.hr.dal.dataobject.location;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 公司地点管理 DO
 *
 * @author MK
 */
@TableName("hr_location")
@KeySequence("hr_location_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LocationDO extends BaseDO {

    /**
     * 地点ID
     */
    @TableId
    private Long id;
    
    /**
     * 地点名称
     */
    private String locationName;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 省份
     */
    private String province;
    
    /**
     * 市
     */
    private String city;
    
    /**
     * 县/区
     */
    private String district;

} 