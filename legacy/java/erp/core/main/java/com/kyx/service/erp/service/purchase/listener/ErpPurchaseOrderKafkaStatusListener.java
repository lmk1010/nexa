package com.kyx.service.erp.service.purchase.listener;

import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.bpm.api.event.dto.BpmProcessInstanceStatusKafkaEvent;
import com.kyx.service.erp.config.KafkaTopicConfig;
import com.kyx.service.erp.enums.purchase.ErpPurchaseOrderBmpStatusEnum;
import com.kyx.service.erp.enums.ErpAuditStatus;
import com.kyx.service.erp.service.purchase.ErpPurchaseOrderService;
import com.kyx.service.erp.service.purchase.ErpPurchaseOrderServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 采购订单BPM流程状态监听器
 *
 * @author MK
 */
@Slf4j
@Component
public class ErpPurchaseOrderKafkaStatusListener {

    @Resource
    private ErpPurchaseOrderService purchaseOrderService;

    /**
     * 监听BPM流程状态变更事件
     */
    @KafkaListener(
            topics = KafkaTopicConfig.BPM_PROCESS_STATUS_TOPIC,
            groupId = "erp-purchase-order-group"
    )
    public void handleBmpProcessStatusChanged(BpmProcessInstanceStatusKafkaEvent event) {
        log.info("接收到BPM流程状态变更事件: processDefinitionKey={}, businessKey={}, status={}, tenantId={}, userId={}", 
                event.getProcessDefinitionKey(), event.getBusinessKey(), event.getStatus(), 
                event.getTenantId(), event.getUserId());

        // 只处理采购订单的流程
        if (!ErpPurchaseOrderServiceImpl.PROCESS_KEY.equals(event.getProcessDefinitionKey())) {
            log.debug("非采购订单流程，忽略处理: processDefinitionKey={}", event.getProcessDefinitionKey());
            return;
        }

        // 使用事件中的租户信息执行业务逻辑
        final Long tenantId = event.getTenantId() != null ? event.getTenantId() : TenantContextHolder.getTenantId();
        if (event.getTenantId() == null) {
            log.warn("事件中缺少租户信息，使用当前上下文租户: businessKey={}, currentTenantId={}", 
                    event.getBusinessKey(), tenantId);
        }

        try {
            // 在指定租户上下文中执行业务逻辑
            TenantUtils.execute(tenantId, () -> {
                try {
                    Long orderId = Long.parseLong(event.getBusinessKey());
                    Integer bmpStatus = event.getStatus();

                    // 根据BPM流程状态更新采购订单状态
                    Integer orderStatus = null;
                    Integer orderBmpStatus = null;

                    switch (bmpStatus) {
                        case 2: // 审批通过
                            orderStatus = ErpAuditStatus.APPROVE.getStatus();
                            orderBmpStatus = ErpPurchaseOrderBmpStatusEnum.COMPLETED.getStatus();
                            break;
                        case 3: // 审批不通过
                            orderStatus = ErpAuditStatus.PROCESS.getStatus();
                            orderBmpStatus = ErpPurchaseOrderBmpStatusEnum.REJECTED.getStatus();
                            break;
                        case 4: // 已取消
                            orderStatus = ErpAuditStatus.PROCESS.getStatus();
                            orderBmpStatus = ErpPurchaseOrderBmpStatusEnum.CANCELLED.getStatus();
                            break;
                        default:
                            log.debug("流程进行中，保持原状态: orderId={}, status={}", orderId, bmpStatus);
                            return;
                    }

                    // 更新采购订单状态
                    purchaseOrderService.updatePurchaseOrderBmpStatus(orderId, orderStatus, orderBmpStatus);
                    log.info("成功更新采购订单状态: orderId={}, orderStatus={}, bmpStatus={}, tenantId={}", 
                            orderId, orderStatus, orderBmpStatus, tenantId);

                } catch (NumberFormatException e) {
                    log.error("解析业务Key失败: businessKey={}, tenantId={}", event.getBusinessKey(), tenantId, e);
                } catch (Exception e) {
                    log.error("更新采购订单状态失败: businessKey={}, tenantId={}", event.getBusinessKey(), tenantId, e);
                }
            });
        } catch (Exception e) {
            log.error("执行采购订单状态更新失败: processDefinitionKey={}, businessKey={}, tenantId={}", 
                    event.getProcessDefinitionKey(), event.getBusinessKey(), tenantId, e);
        }
    }
} 