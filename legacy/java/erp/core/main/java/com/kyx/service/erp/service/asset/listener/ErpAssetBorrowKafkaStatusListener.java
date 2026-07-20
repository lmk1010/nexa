package com.kyx.service.erp.service.asset.listener;

import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.bpm.api.event.dto.BpmProcessInstanceStatusKafkaEvent;
import com.kyx.service.erp.config.KafkaTopicConfig;
import com.kyx.service.erp.enums.asset.ErpAssetBorrowBmpStatusEnum;
import com.kyx.service.erp.service.asset.ErpAssetBorrowService;
import com.kyx.service.erp.service.asset.ErpAssetBorrowServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 资产借用BPM流程状态监听器
 *
 * @author kyx
 */
@Slf4j
@Component
public class ErpAssetBorrowKafkaStatusListener {

    @Resource
    private ErpAssetBorrowService borrowService;

    /**
     * 监听BPM流程状态变更事件
     */
    @KafkaListener(
            topics = KafkaTopicConfig.BPM_PROCESS_STATUS_TOPIC,
            groupId = "erp-asset-borrow-group"
    )
    public void handleBmpProcessStatusChanged(BpmProcessInstanceStatusKafkaEvent event) {
        log.info("接收到BPM流程状态变更事件: processDefinitionKey={}, businessKey={}, status={}, tenantId={}, userId={}", 
                event.getProcessDefinitionKey(), event.getBusinessKey(), event.getStatus(), 
                event.getTenantId(), event.getUserId());

        // 只处理资产借用的流程
        if (!ErpAssetBorrowServiceImpl.PROCESS_KEY.equals(event.getProcessDefinitionKey())) {
            log.debug("非资产借用流程，忽略处理: processDefinitionKey={}", event.getProcessDefinitionKey());
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
                    Long borrowId = Long.parseLong(event.getBusinessKey());
                    Integer bmpStatus = event.getStatus();

                    // 根据BPM流程状态更新资产借用状态
                    Integer borrowApprovalStatus = null;
                    Integer borrowBmpStatus = null;

                    switch (bmpStatus) {
                        case 2: // 审批通过
                            borrowApprovalStatus = 2; // 审批通过
                            borrowBmpStatus = ErpAssetBorrowBmpStatusEnum.COMPLETED.getStatus();
                            break;
                        case 3: // 审批不通过
                            borrowApprovalStatus = 3; // 审批拒绝
                            borrowBmpStatus = ErpAssetBorrowBmpStatusEnum.REJECTED.getStatus();
                            break;
                        case 4: // 已取消
                            borrowApprovalStatus = 3; // 审批拒绝
                            borrowBmpStatus = ErpAssetBorrowBmpStatusEnum.CANCELLED.getStatus();
                            break;
                        default:
                            log.debug("流程进行中，保持原状态: borrowId={}, status={}", borrowId, bmpStatus);
                            return;
                    }

                    // 更新资产借用状态
                    borrowService.updateBorrowBmpStatus(borrowId, borrowApprovalStatus, borrowBmpStatus);
                    log.info("成功更新资产借用状态: borrowId={}, approvalStatus={}, bmpStatus={}, tenantId={}", 
                            borrowId, borrowApprovalStatus, borrowBmpStatus, tenantId);

                } catch (NumberFormatException e) {
                    log.error("解析businessKey失败: businessKey={}", event.getBusinessKey(), e);
                } catch (Exception e) {
                    log.error("更新资产借用状态失败: businessKey={}, tenantId={}", event.getBusinessKey(), tenantId, e);
                    throw new RuntimeException(e); // 重新抛出异常，让TenantUtils处理
                }
            });

        } catch (Exception e) {
            log.error("处理BPM流程状态变更事件失败: event={}", event, e);
        }
    }
} 