package com.kyx.service.hr.service.reminder;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.reminder.vo.HrReminderRecordPageReqVO;
import com.kyx.service.hr.controller.admin.reminder.vo.HrReminderRecordRespVO;

public interface HrReminderRecordService {

    PageResult<HrReminderRecordRespVO> getPage(HrReminderRecordPageReqVO pageReqVO);

    void read(Long id);

    Integer readAll(Boolean mineOnly);

    Integer refreshGeneratedRecords();

}
