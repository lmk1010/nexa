package com.kyx.service.erp.service.asset.listener;

import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.bpm.api.event.dto.BpmProcessInstanceStatusKafkaEvent;
import com.kyx.service.erp.config.KafkaTopicConfig;
import com.kyx.service.erp.enums.asset.ErpAssetScrappedBmpStatusEnum;
import com.kyx.service.erp.service.asset.ErpAssetScrappedService;
import com.kyx.service.erp.service.asset.ErpAssetScrappedServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * ERP 资产报废 BMP 状态 Kafka 监听器
 * 
 * 监听BMP工作流状态变化，同步更新资产报废记录状态
 *
 * @author kyx
 */
@Slf4j
@Component
public class ErpAssetScrappedKafkaStatusListener {

    @Resource
    private ErpAssetScrappedService scrappedService;

    /**
     * 监听BPM流程状态变更事件
     */
    @KafkaListener(
            topics = KafkaTopicConfig.BPM_PROCESS_STATUS_TOPIC,
            groupId = "erp-asset-scrapped-group"
    )
    public void handleBmpProcessStatusChanged(BpmProcessInstanceStatusKafkaEvent event) {
        log.info("接收到BPM流程状态变更事件: processDefinitionKey={}, businessKey={}, status={}, tenantId={}, userId={}", 
                event.getProcessDefinitionKey(), event.getBusinessKey(), event.getStatus(), 
                event.getTenantId(), event.getUserId());

        // 只处理资产报废的流程
        if (!ErpAssetScrappedServiceImpl.PROCESS_KEY.equals(event.getProcessDefinitionKey())) {
            log.debug("非资产报废流程，忽略处理: processDefinitionKey={}", event.getProcessDefinitionKey());
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
                    Long scrappedId = Long.parseLong(event.getBusinessKey());
                    Integer bmpStatus = event.getStatus();

                    // 根据BPM流程状态更新资产报废状态
                    Integer approvalStatus = null;
                    Integer scrappedBmpStatus = null;

                    switch (bmpStatus) {
                        case 2: // 审批通过
                            approvalStatus = 2; // 审批通过
                            scrappedBmpStatus = ErpAssetScrappedBmpStatusEnum.COMPLETED.getStatus();
                            break;
                        case 3: // 审批不通过
                            approvalStatus = 3; // 审批拒绝
                            scrappedBmpStatus = ErpAssetScrappedBmpStatusEnum.CANCELLED.getStatus();
                            break;
                        case 4: // 已取消
                            approvalStatus = 3; // 审批拒绝
                            scrappedBmpStatus = ErpAssetScrappedBmpStatusEnum.CANCELLED.getStatus();
                            break;
                        default:
                            log.debug("流程进行中，保持原状态: scrappedId={}, status={}", scrappedId, bmpStatus);
                            return;
                    }

                    // 更新资产报废状态
                    scrappedService.updateScrappedBmpStatus(scrappedId, approvalStatus, scrappedBmpStatus);
                    log.info("成功更新资产报废状态: scrappedId={}, approvalStatus={}, bmpStatus={}, tenantId={}", 
                            scrappedId, approvalStatus, scrappedBmpStatus, tenantId);

                } catch (NumberFormatException e) {
                    log.error("BusinessKey格式错误，无法解析为Long: {}", event.getBusinessKey(), e);
                } catch (Exception e) {
                    log.error("处理资产报废BMP状态变化异常: processInstanceId={}, businessKey={}, tenantId={}", 
                            event.getProcessInstanceId(), event.getBusinessKey(), tenantId, e);
                }
            });
        } catch (Exception e) {
            log.error("租户上下文执行异常: tenantId={}, processInstanceId={}, businessKey={}", 
                    tenantId, event.getProcessInstanceId(), event.getBusinessKey(), e);
        }
    }
} 