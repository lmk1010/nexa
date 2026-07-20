package com.kyx.service.bpm.framework.flowable.core.event;

import com.kyx.service.bpm.api.event.BpmProcessInstanceStatusEvent;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.validation.annotation.Validated;

/**
 * {@link BpmProcessInstanceStatusEvent} 的生产者
 *
 * @author MK
 */
@Slf4j
@AllArgsConstructor
@Validated
public class BpmProcessInstanceEventPublisher {

    private final ApplicationEventPublisher publisher;

    public void sendProcessInstanceResultEvent(@Valid BpmProcessInstanceStatusEvent event) {
        log.error("发送流程实力通知！");
        publisher.publishEvent(event);
    }

}
