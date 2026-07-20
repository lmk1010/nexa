package com.kyx.service.hr.integration.dingtalk.service;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.integration.vo.DingTalkSyncHistoryPageReqVO;
import com.kyx.service.hr.dal.dataobject.integration.DingTalkSyncHistoryDO;
import com.kyx.service.hr.dal.mysql.integration.DingTalkSyncHistoryMapper;
import lombok.Data;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Service
public class DingTalkSyncHistoryService {

    @Resource
    private DingTalkSyncHistoryMapper dingTalkSyncHistoryMapper;

    public void save(DingTalkSyncHistorySaveReq req) {
        if (req == null) {
            return;
        }
        DingTalkSyncHistoryDO entity = new DingTalkSyncHistoryDO();
        entity.setSyncType(req.getSyncType());
        entity.setSyncScope(req.getSyncScope());
        entity.setTriggerMode(req.getTriggerMode());
        entity.setTargetTenantId(req.getTargetTenantId());
        entity.setOperatorUserId(req.getOperatorUserId());
        entity.setLookbackMinutes(req.getLookbackMinutes());
        entity.setAutoCreateEnabled(req.getAutoCreateEnabled());
        entity.setAutoCreateDeptId(req.getAutoCreateDeptId());
        entity.setTotalCount(defaultInt(req.getTotalCount()));
        entity.setPulledCount(defaultInt(req.getPulledCount()));
        entity.setSyncedCount(defaultInt(req.getSyncedCount()));
        entity.setCreatedCount(defaultInt(req.getCreatedCount()));
        entity.setUpdatedCount(defaultInt(req.getUpdatedCount()));
        entity.setFailedCount(defaultInt(req.getFailedCount()));
        entity.setSkippedCount(defaultInt(req.getSkippedCount()));
        entity.setSyncStartTime(req.getSyncStartTime());
        entity.setSyncEndTime(req.getSyncEndTime());
        entity.setDurationMs(req.getDurationMs());
        entity.setSummary(req.getSummary());
        entity.setDetailJson(req.getDetailJson());
        dingTalkSyncHistoryMapper.insert(entity);
    }

    public PageResult<DingTalkSyncHistoryDO> getPage(DingTalkSyncHistoryPageReqVO reqVO) {
        return dingTalkSyncHistoryMapper.selectPage(reqVO);
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    @Data
    public static class DingTalkSyncHistorySaveReq {
        private String syncType;
        private String syncScope;
        private String triggerMode;
        private Long targetTenantId;
        private Long operatorUserId;
        private Long lookbackMinutes;
        private Boolean autoCreateEnabled;
        private Long autoCreateDeptId;
        private Integer totalCount;
        private Integer pulledCount;
        private Integer syncedCount;
        private Integer createdCount;
        private Integer updatedCount;
        private Integer failedCount;
        private Integer skippedCount;
        private LocalDateTime syncStartTime;
        private LocalDateTime syncEndTime;
        private Long durationMs;
        private String summary;
        private String detailJson;
    }
}
