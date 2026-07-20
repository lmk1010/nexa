package com.kyx.service.erp.service.purchase.listener;

import com.kyx.service.bpm.api.event.dto.BpmProcessInstanceStatusKafkaEvent;
import com.kyx.service.erp.config.KafkaTopicConfig;
import com.kyx.service.erp.enums.purchase.ErpPurchaseRequestBmpStatusEnum;
import com.kyx.service.erp.enums.purchase.ErpPurchaseRequestStatusEnum;
import com.kyx.service.erp.service.purchase.ErpPurchaseRequestService;
import com.kyx.service.erp.service.purchase.ErpPurchaseRequestServiceImpl;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 基于Kafka的采购申请BPM流程状态监听器
 *
 * @author MK
 */
@Slf4j
@Component
public class ErpPurchaseKafkaStatusListener {

    @Resource
    private ErpPurchaseRequestService purchaseRequestService;

    /**
     * 监听BPM流程状态变更事件
     */
    @KafkaListener(
            topics = KafkaTopicConfig.BPM_PROCESS_STATUS_TOPIC,
            groupId = "erp-purchase-request-group"
    )
    public void handleBpmProcessStatusChanged(BpmProcessInstanceStatusKafkaEvent event) {
        log.info("接收到BPM流程状态变更事件: processDefinitionKey={}, businessKey={}, status={}, tenantId={}, userId={}", 
                event.getProcessDefinitionKey(), event.getBusinessKey(), event.getStatus(), 
                event.getTenantId(), event.getUserId());

        // 只处理采购申请的流程
        if (!ErpPurchaseRequestServiceImpl.PROCESS_KEY.equals(event.getProcessDefinitionKey())) {
            log.debug("非采购申请流程，忽略处理: processDefinitionKey={}", event.getProcessDefinitionKey());
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
                    Long requestId = Long.parseLong(event.getBusinessKey());
                    Integer bmpStatus = event.getStatus();

                    // 根据BPM流程状态更新采购申请状态
                    Integer requestStatus = null;
                    Integer requestBmpStatus = null;

                    switch (bmpStatus) {
                        case 2: // 审批通过
                            requestStatus = ErpPurchaseRequestStatusEnum.APPROVED.getStatus();
                            requestBmpStatus = ErpPurchaseRequestBmpStatusEnum.COMPLETED.getStatus();
                            break;
                        case 3: // 审批不通过
                            requestStatus = ErpPurchaseRequestStatusEnum.REJECTED.getStatus();
                            requestBmpStatus = ErpPurchaseRequestBmpStatusEnum.REJECTED.getStatus();
                            break;
                        case 4: // 已取消
                            requestStatus = ErpPurchaseRequestStatusEnum.CANCELLED.getStatus();
                            requestBmpStatus = ErpPurchaseRequestBmpStatusEnum.CANCELLED.getStatus();
                            break;
                        default:
                            log.debug("流程进行中，保持原状态: requestId={}, status={}", requestId, bmpStatus);
                            return;
                    }

                    // 更新采购申请状态
                    purchaseRequestService.updatePurchaseRequestBmpStatus(requestId, requestStatus, requestBmpStatus);
                    log.info("成功更新采购申请状态: requestId={}, requestStatus={}, bmpStatus={}, tenantId={}", 
                            requestId, requestStatus, requestBmpStatus, tenantId);

                } catch (NumberFormatException e) {
                    log.error("解析businessKey失败: businessKey={}", event.getBusinessKey(), e);
                } catch (Exception e) {
                    log.error("更新采购申请状态失败: businessKey={}, tenantId={}", event.getBusinessKey(), tenantId, e);
                    throw new RuntimeException(e); // 重新抛出异常，让TenantUtils处理
                }
            });

        } catch (Exception e) {
            log.error("处理BPM流程状态变更事件失败: event={}", event, e);
        }
    }
} 