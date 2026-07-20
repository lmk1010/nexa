package com.kyx.service.erp.dal.dataobject.purchase;

import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * ERP 采购组织 DO
 *
 * @author MK
 */
@TableName("erp_pr_organization")
@KeySequence("erp_pr_organization_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPrOrganizationDO extends BaseDO {

    /**
     * 组织编号
     */
    @TableId
    private Long id;
    
    /**
     * 组织编码
     */
    private String code;
    
    /**
     * 组织名称
     */
    private String name;
    
    /**
     * 上级组织编号
     */
    private Long parentId;
    
    /**
     * 负责人
     */
    private String manager;
    
    /**
     * 联系电话
     */
    private String phone;
    
    /**
     * 电子邮箱
     */
    private String email;
    
    /**
     * 地址
     */
    private String address;
    
    /**
     * 排序
     */
    private Integer sort;
    
    /**
     * 状态
     *
     * 枚举 {@link com.kyx.foundation.common.enums.CommonStatusEnum}
     */
    private Integer status;
    
    /**
     * 备注
     */
    private String remark;

} 