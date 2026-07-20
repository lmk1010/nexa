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

import java.util.List;

/**
 * 会议室管理 DO
 *
 * @author MK
 */
@TableName(value = "hr_administrative_meeting_room", autoResultMap = true)
@KeySequence("hr_administrative_meeting_room_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
public class HrAdministrativeMeetingRoomDO extends BaseDO {

    /**
     * 会议室ID
     */
    @TableId
    private Long id;
    /**
     * 会议室编号
     */
    private String roomCode;
    /**
     * 会议室名称
     */
    private String roomName;
    /**
     * 楼层
     */
    private String floor;
    /**
     * 位置
     */
    private String location;
    /**
     * 容纳人数
     */
    private Integer capacity;
    /**
     * 设备（JSON数组）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> equipment;
    /**
     * 状态（0启用 1停用）
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;
}
