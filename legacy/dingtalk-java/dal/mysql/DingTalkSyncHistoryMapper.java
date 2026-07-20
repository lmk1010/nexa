package com.kyx.service.hr.dal.mysql.integration;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.integration.vo.DingTalkSyncHistoryPageReqVO;
import com.kyx.service.hr.dal.dataobject.integration.DingTalkSyncHistoryDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DingTalkSyncHistoryMapper extends BaseMapperX<DingTalkSyncHistoryDO> {

    default PageResult<DingTalkSyncHistoryDO> selectPage(DingTalkSyncHistoryPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<DingTalkSyncHistoryDO>()
                .eqIfPresent(DingTalkSyncHistoryDO::getSyncType, reqVO.getSyncType())
                .eqIfPresent(DingTalkSyncHistoryDO::getSyncScope, reqVO.getSyncScope())
                .eqIfPresent(DingTalkSyncHistoryDO::getTriggerMode, reqVO.getTriggerMode())
                .eqIfPresent(DingTalkSyncHistoryDO::getTargetTenantId, reqVO.getTargetTenantId())
                .betweenIfPresent(DingTalkSyncHistoryDO::getSyncEndTime, reqVO.getSyncEndTime())
                .orderByDesc(DingTalkSyncHistoryDO::getSyncEndTime)
                .orderByDesc(DingTalkSyncHistoryDO::getId));
    }
}
