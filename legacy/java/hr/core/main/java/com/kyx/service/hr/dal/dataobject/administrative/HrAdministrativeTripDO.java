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
 * 出差申请 DO
 *
 * @author MK
 */
@TableName(value = "hr_administrative_trip", autoResultMap = true)
@KeySequence("hr_administrative_trip_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class HrAdministrativeTripDO extends BaseDO {

    /**
     * 出差申请ID
     */
    @TableId
    private Long id;
    /**
     * 申请人用户ID
     */
    private Long userId;
    /**
     * 出差类型
     */
    private String tripType;
    /**
     * 出差城市
     */
    private String destinationCity;
    /**
     * 出差地址
     */
    private String destinationAddress;
    /**
     * 出差事由
     */
    private String purpose;
    /**
     * 交通方式
     */
    private String transportType;
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    /**
     * 出差时长(天)
     */
    private BigDecimal duration;
    /**
     * 预计费用
     */
    private BigDecimal costEstimate;
    /**
     * 应急电话
     */
    private String emergencyPhone;
    /**
     * 同行人
     */
    private String companions;
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
