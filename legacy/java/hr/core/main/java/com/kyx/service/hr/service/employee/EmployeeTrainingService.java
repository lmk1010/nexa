package com.kyx.service.hr.service.employee;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeTrainingPageReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeTrainingRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeTrainingSaveReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeTrainingStatsRespVO;

import javax.validation.Valid;
import java.util.List;

public interface EmployeeTrainingService {

    List<EmployeeTrainingRespVO> getTrainingList(Long profileId);

    List<EmployeeTrainingRespVO> getMyTrainingList();

    PageResult<EmployeeTrainingRespVO> getTrainingPage(EmployeeTrainingPageReqVO pageReqVO);

    PageResult<EmployeeTrainingRespVO> getRetrainReminderPage(EmployeeTrainingPageReqVO pageReqVO);

    EmployeeTrainingStatsRespVO getTrainingStats(EmployeeTrainingPageReqVO pageReqVO);

    Long createTraining(@Valid EmployeeTrainingSaveReqVO createReqVO);

    void updateTraining(@Valid EmployeeTrainingSaveReqVO updateReqVO);

    void deleteTraining(Long id);
}
