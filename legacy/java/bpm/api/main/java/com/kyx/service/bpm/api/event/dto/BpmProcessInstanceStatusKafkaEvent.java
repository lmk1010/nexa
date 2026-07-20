package com.kyx.service.bpm.api.event.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * BPM流程实例状态变更的Kafka事件
 *
 * @author MK
 */
@Data
@Accessors(chain = true)
public class BpmProcessInstanceStatusKafkaEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流程实例的编号
     */
    @NotNull(message = "流程实例的编号不能为空")
    private String processInstanceId;
    
    /**
     * 流程实例的 key
     */
    @NotNull(message = "流程实例的 key 不能为空")
    private String processDefinitionKey;
    
    /**
     * 流程实例的结果状态
     */
    @NotNull(message = "流程实例的状态不能为空")
    private Integer status;
    
    /**
     * 流程实例对应的业务标识
     * 例如说，采购申请的ID
     */
    private String businessKey;
    
    /**
     * 事件时间戳
     */
    private Long timestamp;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    /**
     * 操作用户ID（流程发起人或审批人）
     */
    private Long userId;
} 