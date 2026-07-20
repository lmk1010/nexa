package com.kyx.service.hr.dal.dataobject.employee;

import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工成长记录表
 *
 * @author MK
 */
@TableName("hr_employee_growth_log")
@KeySequence("hr_employee_growth_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeGrowthLogDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 员工档案ID
     */
    private Long profileId;

    /**
     * 事件类型：1入职 2转正 3晋升 4降级 5调岗 6离职 7复职 8其他
     */
    private Integer eventType;

    /**
     * 事件日期
     */
    private LocalDate eventDate;

    /**
     * 事件标题
     */
    private String title;

    /**
     * 事件描述
     */
    private String content;

    /**
     * 变动前部门ID
     */
    private Long beforeDeptId;

    /**
     * 变动前部门名称
     */
    private String beforeDeptName;

    /**
     * 变动前职位
     */
    private String beforeJobTitle;

    /**
     * 变动前职级ID
     */
    private Long beforeJobLevelId;

    /**
     * 变动前职级名称
     */
    private String beforeJobLevelName;

    /**
     * 变动后部门ID
     */
    private Long afterDeptId;

    /**
     * 变动后部门名称
     */
    private String afterDeptName;

    /**
     * 变动后职位
     */
    private String afterJobTitle;

    /**
     * 变动后职级ID
     */
    private Long afterJobLevelId;

    /**
     * 变动后职级名称
     */
    private String afterJobLevelName;

    /**
     * 审批状态：0待审批 1已通过 2已拒绝
     */
    private Integer approvalStatus;

    /**
     * 审批人ID
     */
    private Long approverId;

    /**
     * 审批人姓名
     */
    private String approverName;

    /**
     * 审批时间
     */
    private LocalDateTime approvalTime;

    /**
     * 审批备注
     */
    private String approvalRemark;

    /**
     * 附件URL列表(JSON)
     */
    private String attachmentUrls;

}
