package com.kyx.service.hr.service.employee;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRecruitmentPageReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRecruitmentPublicInfoRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRecruitmentPublicLinkRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRecruitmentPublicLinkSaveReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRecruitmentConvertEntryReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRecruitmentPublicSubmitReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRecruitmentRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRecruitmentSaveReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRecruitmentStatsRespVO;

import javax.validation.Valid;
import java.util.List;

public interface EmployeeRecruitmentService {

    List<EmployeeRecruitmentRespVO> getRecruitmentList(Long profileId);

    PageResult<EmployeeRecruitmentRespVO> getRecruitmentPage(EmployeeRecruitmentPageReqVO pageReqVO);

    EmployeeRecruitmentStatsRespVO getRecruitmentStats(EmployeeRecruitmentPageReqVO pageReqVO);

    Long createRecruitment(@Valid EmployeeRecruitmentSaveReqVO createReqVO);

    void updateRecruitment(@Valid EmployeeRecruitmentSaveReqVO updateReqVO);

    void deleteRecruitment(Long id);

    EmployeeRecruitmentRespVO parseResume(Long id);

    EmployeeRecruitmentPublicLinkRespVO createPublicLink(
            @Valid EmployeeRecruitmentPublicLinkSaveReqVO createReqVO);

    EmployeeRecruitmentPublicInfoRespVO getPublicInfo(String token);

    Long submitPublicCandidate(@Valid EmployeeRecruitmentPublicSubmitReqVO submitReqVO);

    Long convertToEntry(@Valid EmployeeRecruitmentConvertEntryReqVO convertReqVO);

    String submitDemandApproval(Long id);

    String submitOfferApproval(Long id);

    void updateApprovalStatusByBpmEvent(Long id, String processDefinitionKey,
                                        String processInstanceId, Integer bpmStatus, Long operatorUserId);
}
