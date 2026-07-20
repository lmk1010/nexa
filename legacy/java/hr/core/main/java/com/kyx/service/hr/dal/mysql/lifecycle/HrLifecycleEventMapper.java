package com.kyx.service.hr.dal.mysql.lifecycle;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.lifecycle.vo.HrLifecycleEventPageReqVO;
import com.kyx.service.hr.dal.dataobject.lifecycle.HrLifecycleEventDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Mapper
public interface HrLifecycleEventMapper extends BaseMapperX<HrLifecycleEventDO> {

    default PageResult<HrLifecycleEventDO> selectPage(HrLifecycleEventPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrLifecycleEventDO>()
                .eqIfPresent(HrLifecycleEventDO::getProfileId, reqVO.getProfileId())
                .likeIfPresent(HrLifecycleEventDO::getEmployeeName, reqVO.getEmployeeName())
                .eqIfPresent(HrLifecycleEventDO::getEventType, reqVO.getEventType())
                .eqIfPresent(HrLifecycleEventDO::getEventStatus, reqVO.getEventStatus())
                .eqIfPresent(HrLifecycleEventDO::getSourceType, reqVO.getSourceType())
                .geIfPresent(HrLifecycleEventDO::getEffectiveDate, reqVO.getEffectiveDateStart())
                .leIfPresent(HrLifecycleEventDO::getEffectiveDate, reqVO.getEffectiveDateEnd())
                .orderByDesc(HrLifecycleEventDO::getEffectiveDate)
                .orderByDesc(HrLifecycleEventDO::getId));
    }

    default List<HrLifecycleEventDO> selectListByProfileId(Long profileId) {
        return selectList(new LambdaQueryWrapperX<HrLifecycleEventDO>()
                .eq(HrLifecycleEventDO::getProfileId, profileId)
                .orderByDesc(HrLifecycleEventDO::getEffectiveDate)
                .orderByDesc(HrLifecycleEventDO::getId));
    }

    default HrLifecycleEventDO selectFirstBySource(String eventType, String sourceType, Long sourceId) {
        return selectOne(new LambdaQueryWrapperX<HrLifecycleEventDO>()
                .eq(HrLifecycleEventDO::getEventType, eventType)
                .eq(HrLifecycleEventDO::getSourceType, sourceType)
                .eq(HrLifecycleEventDO::getSourceId, sourceId)
                .last("LIMIT 1"));
    }

    default HrLifecycleEventDO selectActiveResignationByEntryId(Long entryId) {
        return selectOne(new LambdaQueryWrapperX<HrLifecycleEventDO>()
                .eq(HrLifecycleEventDO::getEntryId, entryId)
                .eq(HrLifecycleEventDO::getEventType, "RESIGN_REQUESTED")
                .in(HrLifecycleEventDO::getEventStatus, Arrays.asList("PENDING_APPROVAL", "PENDING_HANDOVER", "PENDING_EFFECTIVE"))
                .orderByDesc(HrLifecycleEventDO::getId)
                .last("LIMIT 1"));
    }

    default HrLifecycleEventDO selectActiveByEntryIdAndType(Long entryId, String eventType) {
        return selectOne(new LambdaQueryWrapperX<HrLifecycleEventDO>()
                .eq(HrLifecycleEventDO::getEntryId, entryId)
                .eq(HrLifecycleEventDO::getEventType, eventType)
                .in(HrLifecycleEventDO::getEventStatus, Arrays.asList("PENDING_APPROVAL", "PENDING_HANDOVER", "PENDING_EFFECTIVE"))
                .orderByDesc(HrLifecycleEventDO::getId)
                .last("LIMIT 1"));
    }

    default List<HrLifecycleEventDO> selectRecentList(Integer limit) {
        return selectList(new LambdaQueryWrapperX<HrLifecycleEventDO>()
                .orderByDesc(HrLifecycleEventDO::getId)
                .last("LIMIT " + limit));
    }

    default List<HrLifecycleEventDO> selectDueEffectiveList(LocalDate today, Integer limit) {
        return selectList(new LambdaQueryWrapperX<HrLifecycleEventDO>()
                .eq(HrLifecycleEventDO::getEventStatus, "PENDING_EFFECTIVE")
                .le(HrLifecycleEventDO::getEffectiveDate, today)
                .orderByAsc(HrLifecycleEventDO::getEffectiveDate)
                .orderByAsc(HrLifecycleEventDO::getId)
                .last("LIMIT " + limit));
    }

    default List<HrLifecycleEventDO> selectListByEffectiveDateRange(LocalDate startDate, LocalDate endDate) {
        return selectList(new LambdaQueryWrapperX<HrLifecycleEventDO>()
                .geIfPresent(HrLifecycleEventDO::getEffectiveDate, startDate)
                .leIfPresent(HrLifecycleEventDO::getEffectiveDate, endDate)
                .orderByAsc(HrLifecycleEventDO::getEffectiveDate)
                .orderByDesc(HrLifecycleEventDO::getId));
    }

}
