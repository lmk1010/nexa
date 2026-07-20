package com.kyx.service.bpm.service.oa.listener;

import com.kyx.service.bpm.api.event.BpmProcessInstanceStatusEvent;
import com.kyx.service.bpm.api.event.BpmProcessInstanceStatusEventListener;
import com.kyx.service.bpm.service.oa.BpmOALeaveService;
import com.kyx.service.bpm.service.oa.BpmOALeaveServiceImpl;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * OA 请假单的结果的监听器实现类
 *
 * @author MK
 */
@Component
public class BpmOALeaveStatusListener extends BpmProcessInstanceStatusEventListener {

    @Resource
    private BpmOALeaveService leaveService;

    @Override
    protected String getProcessDefinitionKey() {
        return BpmOALeaveServiceImpl.PROCESS_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        leaveService.updateLeaveStatus(Long.parseLong(event.getBusinessKey()), event.getStatus());
    }

}
