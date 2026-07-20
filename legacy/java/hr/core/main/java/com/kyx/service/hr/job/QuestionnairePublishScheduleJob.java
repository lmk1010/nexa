package com.kyx.service.hr.job;

import com.kyx.service.hr.service.questionnaire.QuestionnairePublishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 问卷定时发布任务
 *
 * @author MK
 */
@Component
@Slf4j
public class QuestionnairePublishScheduleJob {

    @Resource
    private QuestionnairePublishService publishService;

    @Scheduled(fixedDelay = 60000)
    public void publishDueSchedules() {
        try {
            publishService.publishScheduled();
        } catch (Exception e) {
            log.error("问卷定时发布任务执行失败", e);
        }
        try {
            publishService.processDueDeadlines();
        } catch (Exception e) {
            log.error("问卷截止状态处理任务执行失败", e);
        }
        try {
            publishService.sendDueReminders();
        } catch (Exception e) {
            log.error("问卷提醒任务执行失败", e);
        }
    }
}
