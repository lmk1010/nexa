package com.kyx.service.erp.service.asset.listener;

import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.bpm.api.event.dto.BpmProcessInstanceStatusKafkaEvent;
import com.kyx.service.erp.config.KafkaTopicConfig;
import com.kyx.service.erp.enums.asset.ErpAssetTransferBmpStatusEnum;
import com.kyx.service.erp.service.asset.ErpAssetTransferService;
import com.kyx.service.erp.service.asset.ErpAssetTransferServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 资产转移BPM流程状态监听器
 *
 * @author kyx
 */
@Slf4j
@Component
public class ErpAssetTransferKafkaStatusListener {

    @Resource
    private ErpAssetTransferService transferService;

    /**
     * 监听BPM流程状态变更事件
     */
    @KafkaListener(
            topics = KafkaTopicConfig.BPM_PROCESS_STATUS_TOPIC,
            groupId = "erp-asset-transfer-group"
    )
    public void handleBpmProcessStatusChanged(BpmProcessInstanceStatusKafkaEvent event) {
        log.info("接收到BPM流程状态变更事件: processDefinitionKey={}, businessKey={}, status={}, tenantId={}, userId={}", 
                event.getProcessDefinitionKey(), event.getBusinessKey(), event.getStatus(), 
                event.getTenantId(), event.getUserId());

        // 只处理资产转移的流程
        if (!ErpAssetTransferServiceImpl.PROCESS_KEY.equals(event.getProcessDefinitionKey())) {
            log.debug("非资产转移流程，忽略处理: processDefinitionKey={}", event.getProcessDefinitionKey());
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
                    Long transferId = Long.parseLong(event.getBusinessKey());
                    Integer bmpStatus = event.getStatus();

                    // 根据BPM流程状态更新资产转移状态
                    Integer transferApprovalStatus = null;
                    Integer transferBmpStatus = null;

                    switch (bmpStatus) {
                        case 2: // 审批通过
                            transferApprovalStatus = 2; // 审批通过
                            transferBmpStatus = ErpAssetTransferBmpStatusEnum.COMPLETED.getStatus();
                            break;
                        case 3: // 审批不通过
                            transferApprovalStatus = 3; // 审批拒绝
                            transferBmpStatus = ErpAssetTransferBmpStatusEnum.REJECTED.getStatus();
                            break;
                        case 4: // 已取消
                            transferApprovalStatus = 3; // 审批拒绝
                            transferBmpStatus = ErpAssetTransferBmpStatusEnum.CANCELLED.getStatus();
                            break;
                        default:
                            log.debug("流程进行中，保持原状态: transferId={}, status={}", transferId, bmpStatus);
                            return;
                    }

                    // 更新资产转移状态
                    transferService.updateAssetTransferBmpStatus(transferId, transferApprovalStatus, transferBmpStatus);
                    log.info("成功更新资产转移状态: transferId={}, approvalStatus={}, bmpStatus={}, tenantId={}", 
                            transferId, transferApprovalStatus, transferBmpStatus, tenantId);

                } catch (NumberFormatException e) {
                    log.error("解析businessKey失败: businessKey={}", event.getBusinessKey(), e);
                } catch (Exception e) {
                    log.error("更新资产转移状态失败: businessKey={}, tenantId={}", event.getBusinessKey(), tenantId, e);
                    throw new RuntimeException(e); // 重新抛出异常，让TenantUtils处理
                }
            });

        } catch (Exception e) {
            log.error("处理BPM流程状态变更事件失败: event={}", event, e);
        }
    }
} 