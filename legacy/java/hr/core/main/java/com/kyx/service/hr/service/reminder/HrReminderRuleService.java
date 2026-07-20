package com.kyx.service.hr.service.reminder;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.reminder.vo.HrReminderRulePageReqVO;
import com.kyx.service.hr.controller.admin.reminder.vo.HrReminderRuleRespVO;
import com.kyx.service.hr.controller.admin.reminder.vo.HrReminderRuleSaveReqVO;
import com.kyx.service.hr.dal.dataobject.reminder.HrReminderRuleDO;

public interface HrReminderRuleService {

    Long createRule(HrReminderRuleSaveReqVO reqVO);

    void updateRule(HrReminderRuleSaveReqVO reqVO);

    void deleteRule(Long id);

    void enableRule(Long id, Boolean enabled);

    HrReminderRuleDO getRule(Long id);

    PageResult<HrReminderRuleRespVO> getRulePage(HrReminderRulePageReqVO pageReqVO);

}
