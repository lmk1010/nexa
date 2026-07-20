package com.kyx.service.erp.service.asset.listener;

import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.bpm.api.event.dto.BpmProcessInstanceStatusKafkaEvent;
import com.kyx.service.erp.config.KafkaTopicConfig;
import com.kyx.service.erp.enums.asset.ErpAssetCheckoutBmpStatusEnum;
import com.kyx.service.erp.service.asset.ErpAssetCheckoutService;
import com.kyx.service.erp.service.asset.ErpAssetCheckoutServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 资产领用BPM流程状态监听器
 *
 * @author kyx
 */
@Slf4j
@Component
public class ErpAssetCheckoutKafkaStatusListener {

    @Resource
    private ErpAssetCheckoutService checkoutService;

    /**
     * 监听BPM流程状态变更事件
     */
    @KafkaListener(
            topics = KafkaTopicConfig.BPM_PROCESS_STATUS_TOPIC,
            groupId = "erp-asset-checkout-group"
    )
    public void handleBpmProcessStatusChanged(BpmProcessInstanceStatusKafkaEvent event) {
        log.info("接收到BPM流程状态变更事件: processDefinitionKey={}, businessKey={}, status={}, tenantId={}, userId={}", 
                event.getProcessDefinitionKey(), event.getBusinessKey(), event.getStatus(), 
                event.getTenantId(), event.getUserId());

        // 只处理资产领用的流程
        if (!ErpAssetCheckoutServiceImpl.PROCESS_KEY.equals(event.getProcessDefinitionKey())) {
            log.debug("非资产领用流程，忽略处理: processDefinitionKey={}", event.getProcessDefinitionKey());
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
                    Long checkoutId = Long.parseLong(event.getBusinessKey());
                    Integer bmpStatus = event.getStatus();

                    // 根据BPM流程状态更新资产领用状态
                    Integer checkoutApprovalStatus = null;
                    Integer checkoutBmpStatus = null;

                    switch (bmpStatus) {
                        case 2: // 审批通过
                            checkoutApprovalStatus = 2; // 审批通过
                            checkoutBmpStatus = ErpAssetCheckoutBmpStatusEnum.COMPLETED.getStatus();
                            break;
                        case 3: // 审批不通过
                            checkoutApprovalStatus = 3; // 审批拒绝
                            checkoutBmpStatus = ErpAssetCheckoutBmpStatusEnum.REJECTED.getStatus();
                            break;
                        case 4: // 已取消
                            checkoutApprovalStatus = 3; // 审批拒绝
                            checkoutBmpStatus = ErpAssetCheckoutBmpStatusEnum.CANCELLED.getStatus();
                            break;
                        default:
                            log.debug("流程进行中，保持原状态: checkoutId={}, status={}", checkoutId, bmpStatus);
                            return;
                    }

                    // 更新资产领用状态
                    checkoutService.updateCheckoutBmpStatus(checkoutId, checkoutApprovalStatus, checkoutBmpStatus);
                    log.info("成功更新资产领用状态: checkoutId={}, approvalStatus={}, bmpStatus={}, tenantId={}", 
                            checkoutId, checkoutApprovalStatus, checkoutBmpStatus, tenantId);

                } catch (NumberFormatException e) {
                    log.error("解析businessKey失败: businessKey={}", event.getBusinessKey(), e);
                } catch (Exception e) {
                    log.error("更新资产领用状态失败: businessKey={}, tenantId={}", event.getBusinessKey(), tenantId, e);
                    throw new RuntimeException(e); // 重新抛出异常，让TenantUtils处理
                }
            });

        } catch (Exception e) {
            log.error("处理BPM流程状态变更事件失败: event={}", event, e);
        }
    }
} 