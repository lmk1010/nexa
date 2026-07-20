package com.kyx.service.bpm.api.event;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;

/**
 * {@link BpmProcessInstanceStatusEvent} 的监听器
 *
 * @author MK
 */
@Slf4j
public abstract class BpmProcessInstanceStatusEventListener
        implements ApplicationListener<BpmProcessInstanceStatusEvent> {

    @Override
    public final void onApplicationEvent(BpmProcessInstanceStatusEvent event) {
        if (!StrUtil.equals(event.getProcessDefinitionKey(), getProcessDefinitionKey())) {
            return;
        }
        log.error("接受到通知!");
        onEvent(event);
    }

    /**
     * @return 返回监听的流程定义 Key
     */
    protected abstract String getProcessDefinitionKey();

    /**
     * 处理事件
     *
     * @param event 事件
     */
    protected abstract void onEvent(BpmProcessInstanceStatusEvent event);

}
