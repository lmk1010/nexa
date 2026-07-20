package com.kyx.service.hr.service.administrative.leave;

import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.security.core.service.SecurityFrameworkService;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.service.bpm.api.task.BpmProcessInstanceApi;
import com.kyx.service.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.kyx.service.hr.controller.admin.administrative.leave.vo.HrLeavePageReqVO;
import com.kyx.service.hr.controller.admin.administrative.leave.vo.HrLeaveSaveReqVO;
import com.kyx.service.hr.dal.dataobject.administrative.HrAdministrativeLeaveDO;
import com.kyx.service.hr.dal.mysql.administrative.HrAdministrativeLeaveMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN;
import static com.kyx.service.hr.enums.ErrorCodeConstants.HR_LEAVE_NOT_EXISTS;

/**
 * 请假申请 Service 实现
 *
 * @author MK
 */
@Service
@Validated
@Slf4j
public class HrAdministrativeLeaveServiceImpl implements HrAdministrativeLeaveService {

    /**
     * HR 请假流程定义 KEY
     */
    public static final String PROCESS_KEY = "hr_administrative_leave";

    private static final Integer STATUS_RUNNING = 1;
    private static final String PERMISSION_QUERY_ALL = "hr:administrative-leave:query-all";

    @Resource
    private HrAdministrativeLeaveMapper leaveMapper;
    @Resource
    private HrLeaveBalanceService leaveBalanceService;
    @Resource
    private BpmProcessInstanceApi processInstanceApi;
    @Resource
    private SecurityFrameworkService securityFrameworkService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createLeave(Long userId, HrLeaveSaveReqVO createReqVO) {
        HrAdministrativeLeaveDO leave = BeanUtils.toBean(createReqVO, HrAdministrativeLeaveDO.class)
                .setUserId(userId)
                .setStatus(STATUS_RUNNING);
        fillDefaultDuration(leave);
        leaveMapper.insert(leave);
        leaveBalanceService.handleLeaveStatusChange(leave, null, STATUS_RUNNING);

        Map<String, Object> variables = new HashMap<>();
        variables.put("leaveType", leave.getLeaveType());
        variables.put("leaveCategory", leave.getLeaveCategory());
        variables.put("duration", leave.getDuration());
        variables.put("startTime", leave.getStartTime());
        variables.put("endTime", leave.getEndTime());

        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO()
                        .setProcessDefinitionKey(PROCESS_KEY)
                        .setBusinessKey(String.valueOf(leave.getId()))
                        .setVariables(variables)
                        .setStartUserSelectAssignees(createReqVO.getStartUserSelectAssignees()))
                .getCheckedData();

        leaveMapper.updateById(new HrAdministrativeLeaveDO()
                .setId(leave.getId())
                .setProcessInstanceId(processInstanceId));
        return leave.getId();
    }

    @Override
    public void updateLeave(HrLeaveSaveReqVO updateReqVO) {
        if (updateReqVO.getId() == null) {
            throw ServiceExceptionUtil.exception(HR_LEAVE_NOT_EXISTS);
        }
        validateLeaveExists(updateReqVO.getId());
        HrAdministrativeLeaveDO updateObj = BeanUtils.toBean(updateReqVO, HrAdministrativeLeaveDO.class);
        fillDefaultDuration(updateObj);
        leaveMapper.updateById(updateObj);
    }

    @Override
    public void updateLeaveStatus(Long id, Integer status) {
        HrAdministrativeLeaveDO leave = validateLeaveExists(id);
        leaveMapper.updateById(new HrAdministrativeLeaveDO().setId(id).setStatus(status));
        leaveBalanceService.handleLeaveStatusChange(leave, leave.getStatus(), status);
    }

    @Override
    public HrAdministrativeLeaveDO getLeave(Long id) {
        HrAdministrativeLeaveDO leave = leaveMapper.selectById(id);
        if (leave != null && !canQueryAllLeaves()
                && !Objects.equals(leave.getUserId(), SecurityFrameworkUtils.getLoginUserId())) {
            throw ServiceExceptionUtil.exception(FORBIDDEN, "无权访问该请假记录");
        }
        return leave;
    }

    @Override
    public PageResult<HrAdministrativeLeaveDO> getLeavePage(HrLeavePageReqVO pageReqVO) {
        if (!canQueryAllLeaves()) {
            Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
            if (loginUserId == null) {
                return new PageResult<>(Collections.emptyList(), 0L);
            }
            pageReqVO.setUserId(loginUserId);
        }
        return leaveMapper.selectPage(pageReqVO);
    }

    private HrAdministrativeLeaveDO validateLeaveExists(Long id) {
        HrAdministrativeLeaveDO leave = leaveMapper.selectById(id);
        if (leave == null) {
            throw ServiceExceptionUtil.exception(HR_LEAVE_NOT_EXISTS);
        }
        return leave;
    }

    private void fillDefaultDuration(HrAdministrativeLeaveDO leave) {
        if (leave.getDuration() != null || leave.getStartTime() == null || leave.getEndTime() == null) {
            return;
        }
        long minutes = Duration.between(leave.getStartTime(), leave.getEndTime()).toMinutes();
        if (minutes <= 0) {
            return;
        }
        BigDecimal hours = BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        leave.setDuration(hours);
    }

    private boolean canQueryAllLeaves() {
        try {
            return securityFrameworkService.hasPermission(PERMISSION_QUERY_ALL);
        } catch (Exception ex) {
            log.warn("check leave query-all permission failed: {}", ex.getMessage());
            return false;
        }
    }
}
