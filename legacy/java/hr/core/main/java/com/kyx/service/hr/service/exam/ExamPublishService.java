package com.kyx.service.hr.service.exam;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.exam.vo.ExamPublishPageReqVO;
import com.kyx.service.hr.controller.admin.exam.vo.ExamPublishRespVO;
import com.kyx.service.hr.controller.admin.exam.vo.ExamPublishSaveReqVO;

import javax.validation.Valid;

/**
 * HR 考试发布 Service 接口
 *
 * @author MK
 */
public interface ExamPublishService {

    Long createPublish(@Valid ExamPublishSaveReqVO createReqVO);

    void updatePublish(@Valid ExamPublishSaveReqVO updateReqVO);

    void deletePublish(Long id);

    ExamPublishRespVO getPublish(Long id);

    PageResult<ExamPublishRespVO> getPublishPage(ExamPublishPageReqVO pageReqVO);

    void pauseRecurring(Long publishId);

    void resumeRecurring(Long publishId);

    void closePublish(Long publishId);

    void processScheduledPublishes();

    void processRecurringPublishes();

    void closeExpiredBatches();

}
