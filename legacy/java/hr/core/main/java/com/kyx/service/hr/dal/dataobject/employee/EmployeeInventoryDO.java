package com.kyx.service.hr.dal.dataobject.employee;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 员工盘点信息表
 *
 * @author MK
 */
@TableName("hr_employee_inventory")
@KeySequence("hr_employee_inventory_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeInventoryDO extends TenantBaseDO {

    @TableId
    private Long id;

    /**
     * 员工档案ID
     */
    private Long profileId;

    /**
     * 盘点项目名称
     */
    private String itemName;

    /**
     * 盘点项目编号
     */
    private String itemCode;

    /**
     * 盘点状态
     */
    private String status;

    /**
     * 盘点日期
     */
    private LocalDate checkDate;

    /**
     * 备注
     */
    private String remark;
}
