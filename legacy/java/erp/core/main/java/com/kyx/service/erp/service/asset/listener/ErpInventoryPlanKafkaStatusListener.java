package com.kyx.service.erp.service.asset.listener;

import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.bpm.api.event.dto.BpmProcessInstanceStatusKafkaEvent;
import com.kyx.service.erp.config.KafkaTopicConfig;
import com.kyx.service.erp.service.asset.ErpInventoryPlanService;
import com.kyx.service.erp.service.asset.ErpInventoryPlanServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 盘点计划BPM流程状态监听器
 *
 * @author kyx
 */
@Slf4j
@Component
public class ErpInventoryPlanKafkaStatusListener {

    @Resource
    private ErpInventoryPlanService inventoryPlanService;

    /**
     * 监听BPM流程状态变更事件
     */
    @KafkaListener(
            topics = KafkaTopicConfig.BPM_PROCESS_STATUS_TOPIC,
            groupId = "erp-inventory-plan-group"
    )
    public void handleBpmProcessStatusChanged(BpmProcessInstanceStatusKafkaEvent event) {
        log.info("接收到BPM流程状态变更事件: processDefinitionKey={}, businessKey={}, status={}, tenantId={}, userId={}",
                event.getProcessDefinitionKey(), event.getBusinessKey(), event.getStatus(),
                event.getTenantId(), event.getUserId());

        if (!ErpInventoryPlanServiceImpl.PROCESS_KEY.equals(event.getProcessDefinitionKey())) {
            log.debug("非盘点计划流程，忽略处理: processDefinitionKey={}", event.getProcessDefinitionKey());
            return;
        }
        if (event.getBusinessKey() == null) {
            log.warn("盘点计划BPM事件缺少businessKey，忽略处理: processInstanceId={}, status={}",
                    event.getProcessInstanceId(), event.getStatus());
            return;
        }

        final Long tenantId = event.getTenantId() != null ? event.getTenantId() : TenantContextHolder.getTenantId();
        if (event.getTenantId() == null) {
            log.warn("事件中缺少租户信息，使用当前上下文租户: businessKey={}, currentTenantId={}",
                    event.getBusinessKey(), tenantId);
        }

        try {
            TenantUtils.execute(tenantId, () -> {
                try {
                    Long planId = Long.parseLong(event.getBusinessKey());
                    inventoryPlanService.updateInventoryPlanBpmStatus(planId, event.getStatus(), event.getUserId());
                    log.info("成功更新盘点计划BPM状态: planId={}, bpmStatus={}, tenantId={}",
                            planId, event.getStatus(), tenantId);
                } catch (NumberFormatException e) {
                    log.error("解析盘点计划businessKey失败: businessKey={}", event.getBusinessKey(), e);
                } catch (Exception e) {
                    log.error("更新盘点计划BPM状态失败: businessKey={}, tenantId={}", event.getBusinessKey(), tenantId, e);
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            log.error("处理盘点计划BPM流程状态变更事件失败: event={}", event, e);
        }
    }
}
