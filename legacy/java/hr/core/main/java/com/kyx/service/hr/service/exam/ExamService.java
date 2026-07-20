package com.kyx.service.hr.service.exam;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.exam.vo.ExamPageReqVO;
import com.kyx.service.hr.controller.admin.exam.vo.ExamRespVO;
import com.kyx.service.hr.controller.admin.exam.vo.ExamSaveReqVO;

import javax.validation.Valid;

/**
 * HR 考试管理 Service 接口
 *
 * @author MK
 */
public interface ExamService {

    Long createExam(@Valid ExamSaveReqVO createReqVO);

    void updateExam(@Valid ExamSaveReqVO updateReqVO);

    void deleteExam(Long id);

    ExamRespVO getExam(Long id);

    PageResult<ExamRespVO> getExamPage(ExamPageReqVO pageReqVO);

}
