package com.kyx.service.hr.job;

import com.kyx.foundation.tenant.core.service.TenantFrameworkService;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.hr.service.reminder.HrReminderRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component
@Slf4j
public class HrReminderRecordScheduleJob {

    @Resource
    private TenantFrameworkService tenantFrameworkService;
    @Resource
    private HrReminderRecordService reminderRecordService;

    @Scheduled(fixedDelay = 60000)
    public void refreshReminderRecords() {
        List<Long> tenantIds = tenantFrameworkService.getTenantIds();
        if (tenantIds == null || tenantIds.isEmpty()) {
            log.warn("HR reminder schedule job has no tenant to process");
            return;
        }
        for (Long tenantId : tenantIds) {
            if (tenantId == null || tenantId <= 0) {
                continue;
            }
            TenantUtils.execute(tenantId, () -> processByTenant(tenantId));
        }
    }

    private void processByTenant(Long tenantId) {
        try {
            reminderRecordService.refreshGeneratedRecords();
        } catch (Exception ex) {
            log.error("Refresh HR reminder records failed, tenantId={}", tenantId, ex);
        }
    }

}
