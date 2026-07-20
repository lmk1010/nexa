package com.kyx.service.hr.dal.dataobject.administrative;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 请假申请 DO
 *
 * @author MK
 */
@TableName(value = "hr_administrative_leave", autoResultMap = true)
@KeySequence("hr_administrative_leave_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class HrAdministrativeLeaveDO extends BaseDO {

    /**
     * 请假申请ID
     */
    @TableId
    private Long id;
    /**
     * 申请人用户ID
     */
    private Long userId;
    /**
     * 请假/调休
     */
    private String leaveCategory;
    /**
     * 请假类型
     */
    private String leaveType;
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    /**
     * 时长
     */
    private BigDecimal duration;
    /**
     * 应急电话
     */
    private String emergencyPhone;
    /**
     * 工作交接
     */
    private String workHandover;
    /**
     * 备注
     */
    private String remark;
    /**
     * 附件（JSON数组）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> attachments;
    /**
     * 流程状态
     */
    private Integer status;
    /**
     * 流程实例ID
     */
    private String processInstanceId;

}
