package com.kyx.service.hr.service.onboarding;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.onboarding.vo.OnboardingLinkValidateRespVO;
import com.kyx.service.hr.controller.admin.onboarding.vo.OnboardingPageReqVO;
import com.kyx.service.hr.controller.admin.onboarding.vo.OnboardingRespVO;
import com.kyx.service.hr.controller.admin.onboarding.vo.OnboardingSaveReqVO;
import com.kyx.service.hr.dal.dataobject.onboarding.OnboardingDO;
import com.kyx.service.hr.api.onboarding.dto.OnboardingPublicReqDTO;
import com.kyx.service.hr.api.onboarding.dto.OnboardingPublicRespDTO;

import javax.validation.Valid;
import java.util.List;

/**
 * 入职管理 Service 接口
 *
 * @author MK
 */
public interface OnboardingService {

    /**
     * 审批入职申请
     *
     * @param id 入职申请编号
     * @param approved 是否通过
     * @param comment 审批意见
     */
    void approveOnboarding(Long id, Boolean approved, String comment);

    /**
     * 取消入职申请
     *
     * @param id 入职申请编号
     * @param cancelReason 取消原因
     * @param remark 备注
     */
    void cancelOnboarding(Long id, String cancelReason, String remark);

    /**
     * 恢复入职申请
     *
     * @param id 入职申请编号
     * @param remark 备注
     */
    void restoreOnboarding(Long id, String remark);

    void updateApprovalStatusByBpmEvent(Long id, String processInstanceId, Integer bpmStatus, Long operatorUserId);

    /**
     * 生成申请编号
     *
     * @return 申请编号
     */
    String generateApplicationNo();

    /**
     * 验证入职记录ID是否有效
     *
     * @param entryId 入职记录ID
     * @return 验证结果
     */
    OnboardingLinkValidateRespVO validateEntryId(Long entryId);

    /**
     * 提交入职申请（移动端表单提交）
     *
     * @param createReqDTO 创建信息
     * @return 入职申请响应信息
     */
    OnboardingPublicRespDTO submitOnboarding(@Valid OnboardingPublicReqDTO createReqDTO);

}
