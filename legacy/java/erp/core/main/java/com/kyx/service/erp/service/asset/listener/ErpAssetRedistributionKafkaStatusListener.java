package com.kyx.service.erp.service.asset.listener;

import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.bpm.api.event.dto.BpmProcessInstanceStatusKafkaEvent;
import com.kyx.service.erp.config.KafkaTopicConfig;
import com.kyx.service.erp.enums.asset.ErpAssetRedistributionBmpStatusEnum;
import com.kyx.service.erp.service.asset.ErpAssetRedistributionService;
import com.kyx.service.erp.service.asset.ErpAssetRedistributionServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 资产调拨BPM流程状态监听器
 *
 * @author kyx
 */
@Slf4j
@Component
public class ErpAssetRedistributionKafkaStatusListener {

    @Resource
    private ErpAssetRedistributionService redistributionService;

    /**
     * 监听BPM流程状态变更事件
     */
    @KafkaListener(
            topics = KafkaTopicConfig.BPM_PROCESS_STATUS_TOPIC,
            groupId = "erp-asset-redistribution-group"
    )
    public void handleBpmProcessStatusChanged(BpmProcessInstanceStatusKafkaEvent event) {
        log.info("接收到BPM流程状态变更事件: processDefinitionKey={}, businessKey={}, status={}, tenantId={}, userId={}", 
                event.getProcessDefinitionKey(), event.getBusinessKey(), event.getStatus(), 
                event.getTenantId(), event.getUserId());

        // 只处理资产调拨的流程
        if (!ErpAssetRedistributionServiceImpl.PROCESS_KEY.equals(event.getProcessDefinitionKey())) {
            log.debug("非资产调拨流程，忽略处理: processDefinitionKey={}", event.getProcessDefinitionKey());
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
                    Long redistributionId = Long.parseLong(event.getBusinessKey());
                    Integer bmpStatus = event.getStatus();

                    // 根据BPM流程状态更新资产调拨状态
                    Integer redistributionApprovalStatus = null;
                    Integer redistributionBmpStatus = null;

                    switch (bmpStatus) {
                        case 2: // 审批通过
                            redistributionApprovalStatus = 3; // 审批通过
                            redistributionBmpStatus = ErpAssetRedistributionBmpStatusEnum.COMPLETED.getStatus();
                            break;
                        case 3: // 审批不通过
                            redistributionApprovalStatus = 4; // 审批拒绝
                            redistributionBmpStatus = ErpAssetRedistributionBmpStatusEnum.REJECTED.getStatus();
                            break;
                        case 4: // 已取消
                            redistributionApprovalStatus = 4; // 审批拒绝
                            redistributionBmpStatus = ErpAssetRedistributionBmpStatusEnum.CANCELLED.getStatus();
                            break;
                        default:
                            log.debug("流程进行中，保持原状态: redistributionId={}, status={}", redistributionId, bmpStatus);
                            return;
                    }

                    // 更新资产调拨状态，包含审批人信息
                    redistributionService.updateAssetRedistributionBmpStatus(redistributionId, redistributionApprovalStatus, redistributionBmpStatus, event.getUserId());
                    log.info("成功更新资产调拨状态: redistributionId={}, approvalStatus={}, bmpStatus={}, approverUserId={}, tenantId={}", 
                            redistributionId, redistributionApprovalStatus, redistributionBmpStatus, event.getUserId(), tenantId);

                } catch (NumberFormatException e) {
                    log.error("无法解析业务Key为Long类型: businessKey={}, tenantId={}", event.getBusinessKey(), tenantId, e);
                } catch (Exception e) {
                    log.error("处理资产调拨流程状态变更失败: businessKey={}, status={}, tenantId={}", 
                            event.getBusinessKey(), event.getStatus(), tenantId, e);
                    throw e; // 重新抛出异常以触发Kafka重试机制
                }
            });
        } catch (Exception e) {
            log.error("执行租户上下文操作失败: businessKey={}, tenantId={}", event.getBusinessKey(), tenantId, e);
            // 这里可以根据业务需要决定是否抛出异常触发重试
        }
    }
} 