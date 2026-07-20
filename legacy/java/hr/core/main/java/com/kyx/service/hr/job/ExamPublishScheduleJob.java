package com.kyx.service.hr.job;

import com.kyx.foundation.tenant.core.service.TenantFrameworkService;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.hr.service.exam.ExamPublishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 考试发布定时任务
 *
 * @author MK
 */
@Component
@Slf4j
public class ExamPublishScheduleJob {

    @Resource
    private ExamPublishService publishService;
    @Resource
    private TenantFrameworkService tenantFrameworkService;

    @Scheduled(fixedDelay = 60000)
    public void processExamPublishes() {
        List<Long> tenantIds = tenantFrameworkService.getTenantIds();
        if (tenantIds == null || tenantIds.isEmpty()) {
            log.warn("考试发布定时任务未获取到可执行租户，跳过本轮执行");
            return;
        }
        for (Long tenantId : tenantIds) {
            if (tenantId == null) {
                continue;
            }
            TenantUtils.execute(tenantId, () -> processByTenant(tenantId));
        }
    }

    private void processByTenant(Long tenantId) {
        try {
            publishService.processScheduledPublishes();
        } catch (Exception e) {
            log.error("考试定时发布任务执行失败, tenantId={}", tenantId, e);
        }
        try {
            publishService.processRecurringPublishes();
        } catch (Exception e) {
            log.error("定期考核批次生成任务执行失败, tenantId={}", tenantId, e);
        }
        try {
            publishService.closeExpiredBatches();
        } catch (Exception e) {
            log.error("过期批次关闭任务执行失败, tenantId={}", tenantId, e);
        }
    }

}
