package com.kyx.service.hr.dal.mysql.reminder;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.reminder.vo.HrReminderRecordPageReqVO;
import com.kyx.service.hr.dal.dataobject.reminder.HrReminderRecordDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

/**
 * HR reminder record mapper.
 */
@Mapper
public interface HrReminderRecordMapper extends BaseMapperX<HrReminderRecordDO> {

    default PageResult<HrReminderRecordDO> selectPage(HrReminderRecordPageReqVO reqVO, boolean manage, Long loginUserId) {
        LambdaQueryWrapperX<HrReminderRecordDO> wrapper = new LambdaQueryWrapperX<HrReminderRecordDO>()
                .eqIfPresent(HrReminderRecordDO::getSourceType, reqVO.getSourceType())
                .eqIfPresent(HrReminderRecordDO::getBusinessType, reqVO.getBusinessType())
                .eqIfPresent(HrReminderRecordDO::getReceiverUserId, reqVO.getReceiverUserId())
                .eqIfPresent(HrReminderRecordDO::getSeverity, reqVO.getSeverity())
                .eqIfPresent(HrReminderRecordDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(HrReminderRecordDO::getTriggerTime, reqVO.getTriggerTime());
        if (StringUtils.hasText(reqVO.getKeyword())) {
            String keyword = reqVO.getKeyword().trim();
            wrapper.and(query -> query.like(HrReminderRecordDO::getTitle, keyword)
                    .or()
                    .like(HrReminderRecordDO::getContent, keyword)
                    .or()
                    .like(HrReminderRecordDO::getRuleName, keyword)
                    .or()
                    .like(HrReminderRecordDO::getBusinessType, keyword));
        }
        if (!manage || Boolean.TRUE.equals(reqVO.getMine())) {
            wrapper.eq(HrReminderRecordDO::getReceiverUserId, loginUserId == null ? -1L : loginUserId);
        }
        wrapper.orderByAsc(HrReminderRecordDO::getStatus)
                .orderByDesc(HrReminderRecordDO::getTriggerTime)
                .orderByDesc(HrReminderRecordDO::getId);
        return selectPage(reqVO, wrapper);
    }

    default HrReminderRecordDO selectByRecordKey(String recordKey) {
        return selectOne(HrReminderRecordDO::getRecordKey, recordKey);
    }

}
