package com.kyx.service.hr.service.exam;

import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ExamViewScopeSupport {

    public boolean canViewAllData() {
        Set<Long> tenantIdList = TenantContextHolder.getTenantIdList();
        return TenantContextHolder.isIgnore()
                || (tenantIdList != null && !tenantIdList.isEmpty());
    }

}
