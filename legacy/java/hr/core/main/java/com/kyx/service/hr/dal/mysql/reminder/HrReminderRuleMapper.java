package com.kyx.service.hr.dal.mysql.reminder;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.reminder.vo.HrReminderRulePageReqVO;
import com.kyx.service.hr.dal.dataobject.reminder.HrReminderRuleDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

/**
 * HR reminder rule mapper.
 */
@Mapper
public interface HrReminderRuleMapper extends BaseMapperX<HrReminderRuleDO> {

    default PageResult<HrReminderRuleDO> selectPage(HrReminderRulePageReqVO reqVO) {
        LambdaQueryWrapperX<HrReminderRuleDO> wrapper = new LambdaQueryWrapperX<HrReminderRuleDO>()
                .eqIfPresent(HrReminderRuleDO::getBusinessType, reqVO.getBusinessType())
                .eqIfPresent(HrReminderRuleDO::getEnabled, reqVO.getEnabled());
        if (StringUtils.hasText(reqVO.getKeyword())) {
            String keyword = reqVO.getKeyword().trim();
            wrapper.and(query -> query.like(HrReminderRuleDO::getRuleName, keyword)
                    .or()
                    .like(HrReminderRuleDO::getRuleCode, keyword)
                    .or()
                    .like(HrReminderRuleDO::getRemark, keyword));
        }
        wrapper.orderByDesc(HrReminderRuleDO::getEnabled)
                .orderByAsc(HrReminderRuleDO::getBusinessType)
                .orderByDesc(HrReminderRuleDO::getId);
        return selectPage(reqVO, wrapper);
    }

    default HrReminderRuleDO selectByRuleCode(String ruleCode) {
        return selectOne(HrReminderRuleDO::getRuleCode, ruleCode);
    }

}
