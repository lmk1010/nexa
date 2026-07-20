package com.kyx.service.erp.service.asset.listener;

import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.bpm.api.event.dto.BpmProcessInstanceStatusKafkaEvent;
import com.kyx.service.erp.config.KafkaTopicConfig;
import com.kyx.service.erp.enums.asset.ErpAssetInputBmpStatusEnum;
import com.kyx.service.erp.service.asset.ErpAssetInputService;
import com.kyx.service.erp.service.asset.ErpAssetInputServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 资产录入申请BPM流程状态监听器
 *
 * @author kyx
 */
@Slf4j
@Component
public class ErpAssetInputKafkaStatusListener {

    @Resource
    private ErpAssetInputService assetInputService;

    /**
     * 监听BPM流程状态变更事件
     */
    @KafkaListener(
            topics = KafkaTopicConfig.BPM_PROCESS_STATUS_TOPIC,
            groupId = "erp-asset-input-group"
    )
    public void handleBmpProcessStatusChanged(BpmProcessInstanceStatusKafkaEvent event) {
        log.info("接收到BPM流程状态变更事件: processDefinitionKey={}, businessKey={}, status={}, tenantId={}, userId={}", 
                event.getProcessDefinitionKey(), event.getBusinessKey(), event.getStatus(), 
                event.getTenantId(), event.getUserId());

        // 只处理资产录入申请的流程
        if (!ErpAssetInputServiceImpl.ASSET_INPUT_PROCESS_KEY.equals(event.getProcessDefinitionKey())) {
            log.debug("非资产录入申请流程，忽略处理: processDefinitionKey={}", event.getProcessDefinitionKey());
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
                    Long assetInputId = Long.parseLong(event.getBusinessKey());
                    Integer bmpStatus = event.getStatus();

                    // 1. 首先更新BMP流程状态 - 无论什么状态变化都要记录
                    assetInputService.updateBmpStatus(assetInputId, bmpStatus);
                    log.info("更新资产录入申请BMP状态: assetInputId={}, bmpStatus={}, tenantId={}", assetInputId, bmpStatus, tenantId);

                    // 2. 根据BMP流程状态执行相应的业务逻辑
                    switch (bmpStatus) {
                        case 2: // 审批通过
                            // 通过Service层的approve方法处理，它会创建正式资产记录
                            assetInputService.approveAssetInput(assetInputId, event.getUserId(), 
                                    null, "工作流审批通过");
                            log.info("资产录入申请审批通过: assetInputId={}, userId={}, tenantId={}", assetInputId, event.getUserId(), tenantId);
                            break;
                        case 3: // 审批不通过
                            assetInputService.rejectAssetInput(assetInputId, event.getUserId(), 
                                    null, "工作流审批拒绝");
                            log.info("资产录入申请审批拒绝: assetInputId={}, userId={}, tenantId={}", assetInputId, event.getUserId(), tenantId);
                            break;
                        case 4: // 已取消
                            assetInputService.rejectAssetInput(assetInputId, event.getUserId(), 
                                    null, "工作流已取消");
                            log.info("资产录入申请已取消: assetInputId={}, userId={}, tenantId={}", assetInputId, event.getUserId(), tenantId);
                            break;
                        default:
                            log.debug("BMP流程进行中，已更新状态: assetInputId={}, bmpStatus={}", assetInputId, bmpStatus);
                            // 流程进行中的状态，已经更新了bmpStatus，不需要额外的业务逻辑
                            break;
                    }

                } catch (NumberFormatException e) {
                    log.error("解析businessKey失败: businessKey={}", event.getBusinessKey(), e);
                } catch (Exception e) {
                    log.error("处理资产录入申请BPM状态变更失败: businessKey={}, status={}, tenantId={}", 
                            event.getBusinessKey(), event.getStatus(), tenantId, e);
                    throw e;
                }
            });
        } catch (Exception e) {
            log.error("执行租户上下文失败: businessKey={}, tenantId={}", event.getBusinessKey(), tenantId, e);
            throw e;
        }
    }

} 