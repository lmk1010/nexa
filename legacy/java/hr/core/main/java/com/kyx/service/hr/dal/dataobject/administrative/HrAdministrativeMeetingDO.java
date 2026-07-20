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
 * 会议室预约 DO
 *
 * @author MK
 */
@TableName(value = "hr_administrative_meeting", autoResultMap = true)
@KeySequence("hr_administrative_meeting_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class HrAdministrativeMeetingDO extends BaseDO {

    /**
     * 会议预约ID
     */
    @TableId
    private Long id;
    /**
     * 申请人用户ID
     */
    private Long userId;
    /**
     * 会议室编号
     */
    private String roomId;
    /**
     * 会议室名称
     */
    private String roomName;
    /**
     * 会议主题
     */
    private String meetingTitle;
    /**
     * 会议类型
     */
    private String meetingType;
    /**
     * 会议组织人
     */
    private String organizer;
    /**
     * 参会人数
     */
    private Integer attendees;
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    /**
     * 会议时长(小时)
     */
    private BigDecimal duration;
    /**
     * 设备需求（JSON数组）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> equipment;
    /**
     * 备注
     */
    private String remark;
    /**
     * 流程状态
     */
    private Integer status;
    /**
     * 流程实例ID
     */
    private String processInstanceId;
}
