package com.kyx.service.biz.service.work;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.biz.controller.admin.work.vo.WorkRequirementActionReqVO;
import com.kyx.service.biz.controller.admin.work.vo.WorkRequirementAssignReqVO;
import com.kyx.service.biz.controller.admin.work.vo.WorkRequirementPageReqVO;
import com.kyx.service.biz.controller.admin.work.vo.WorkRequirementOverviewRespVO;
import com.kyx.service.biz.controller.admin.work.vo.WorkRequirementRateSaveReqVO;
import com.kyx.service.biz.controller.admin.work.vo.WorkRequirementSaveReqVO;
import com.kyx.service.biz.controller.admin.work.vo.WorkRequirementCommentCreateReqVO;
import com.kyx.service.biz.dal.dataobject.work.WorkRequirementCommentDO;
import com.kyx.service.biz.dal.dataobject.work.WorkRequirementDO;
import com.kyx.service.biz.dal.dataobject.work.WorkRequirementLogDO;
import com.kyx.service.biz.dal.dataobject.work.WorkRequirementRateDO;

import javax.validation.Valid;
import java.util.List;

public interface WorkRequirementService {

    Long createRequirement(@Valid WorkRequirementSaveReqVO createReqVO, Long userId);

    void updateRequirement(@Valid WorkRequirementSaveReqVO updateReqVO, Long userId);

    void deleteRequirement(Long id, Long userId);

    WorkRequirementDO getRequirement(Long id, Long userId);

    List<WorkRequirementDO> getRequirementChildren(Long parentId, WorkRequirementPageReqVO reqVO, Long userId);

    PageResult<WorkRequirementDO> getRequirementPage(WorkRequirementPageReqVO pageReqVO, Long userId);

    WorkRequirementOverviewRespVO getRequirementOverview(WorkRequirementPageReqVO pageReqVO, Long userId);

    List<String> getTodoApprovalProcessInstanceIds(Long userId);

    List<WorkRequirementLogDO> getRequirementLogs(Long requirementId, Long userId);

    List<WorkRequirementCommentDO> getRequirementComments(Long requirementId, Long userId);

    Long createRequirementComment(@Valid WorkRequirementCommentCreateReqVO reqVO, Long userId);

    void readAllRequirementComments(Long requirementId, Long userId);

    void readAllMyRequirementComments(Long userId);

    List<WorkRequirementRateDO> getRequirementRates(Long requirementId, Long userId);

    Long saveRequirementRate(@Valid WorkRequirementRateSaveReqVO reqVO, Long userId);

    void assignRequirement(@Valid WorkRequirementAssignReqVO reqVO, Long userId);

    void transferAssignRequirement(@Valid WorkRequirementAssignReqVO reqVO, Long userId);

    void devReject(@Valid WorkRequirementActionReqVO reqVO, Long userId);

    void startDev(Long id, Long userId);

    void submitTest(Long id, Long userId);

    void testPass(Long id, Long userId);

    void testReject(@Valid WorkRequirementActionReqVO reqVO, Long userId);

    void acceptPass(@Valid WorkRequirementActionReqVO reqVO, Long userId);

    void acceptReject(@Valid WorkRequirementActionReqVO reqVO, Long userId);

    void cancelRequirement(@Valid WorkRequirementActionReqVO reqVO, Long userId);

    void suspendRequirement(@Valid WorkRequirementActionReqVO reqVO, Long userId);

    void reopenRequirement(@Valid WorkRequirementActionReqVO reqVO, Long userId);

    void submitRequirementApproval(@Valid WorkRequirementActionReqVO reqVO, Long userId);

    void updateApprovalStatusByBpmEvent(Long requirementId, String processInstanceId, Integer bpmStatus, Long operatorUserId);

}
