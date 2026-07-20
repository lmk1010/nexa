package com.kyx.service.hr.controller.admin.administrative.leave;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.kyx.service.hr.controller.admin.administrative.leave.vo.HrLeavePageReqVO;
import com.kyx.service.hr.controller.admin.administrative.leave.vo.HrLeaveRespVO;
import com.kyx.service.hr.controller.admin.administrative.leave.vo.HrLeaveSaveReqVO;
import com.kyx.service.hr.dal.dataobject.administrative.HrAdministrativeLeaveDO;
import com.kyx.service.hr.service.administrative.leave.HrAdministrativeLeaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.kyx.foundation.common.pojo.CommonResult.success;
import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 请假管理")
@RestController
@RequestMapping("/hr/administrative/leave")
@Validated
public class HrAdministrativeLeaveController {

    @Resource
    private HrAdministrativeLeaveService leaveService;
    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建请假申请")
    @PreAuthorize("@ss.hasPermission('hr:administrative-leave:create')")
    public CommonResult<Long> createLeave(@Valid @RequestBody HrLeaveSaveReqVO createReqVO) {
        return success(leaveService.createLeave(getLoginUserId(), createReqVO));
    }

    @PostMapping("/update")
    @Operation(summary = "更新请假申请")
    @PreAuthorize("@ss.hasPermission('hr:administrative-leave:update')")
    public CommonResult<Boolean> updateLeave(@Valid @RequestBody HrLeaveSaveReqVO updateReqVO) {
        leaveService.updateLeave(updateReqVO);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得请假申请")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasAnyPermissions('hr:administrative-leave:query','hr:administrative-leave:query-all')")
    public CommonResult<HrLeaveRespVO> getLeave(@RequestParam("id") Long id) {
        HrAdministrativeLeaveDO leave = leaveService.getLeave(id);
        HrLeaveRespVO respVO = BeanUtils.toBean(leave, HrLeaveRespVO.class);
        fillUserInfo(Collections.singletonList(respVO));
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得请假申请分页")
    @PreAuthorize("@ss.hasAnyPermissions('hr:administrative-leave:query','hr:administrative-leave:query-all')")
    public CommonResult<PageResult<HrLeaveRespVO>> getLeavePage(@Valid HrLeavePageReqVO pageReqVO) {
        PageResult<HrAdministrativeLeaveDO> pageResult = leaveService.getLeavePage(pageReqVO);
        PageResult<HrLeaveRespVO> result = BeanUtils.toBean(pageResult, HrLeaveRespVO.class);
        fillUserInfo(result.getList());
        return success(result);
    }

    @GetMapping("/my-page")
    @Operation(summary = "获得我的请假申请分页")
    @PreAuthorize("@ss.hasPermission('hr:administrative-leave:query')")
    public CommonResult<PageResult<HrLeaveRespVO>> getMyLeavePage(@Valid HrLeavePageReqVO pageReqVO) {
        pageReqVO.setUserId(getLoginUserId());
        PageResult<HrAdministrativeLeaveDO> pageResult = leaveService.getLeavePage(pageReqVO);
        PageResult<HrLeaveRespVO> result = BeanUtils.toBean(pageResult, HrLeaveRespVO.class);
        fillUserInfo(result.getList());
        return success(result);
    }

    private void fillUserInfo(List<HrLeaveRespVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<Long> userIds = records.stream()
                .filter(Objects::nonNull)
                .map(HrLeaveRespVO::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, AdminUserRespDTO> userMap;
        try {
            userMap = adminUserApi.getUserMap(userIds);
        } catch (Exception ignored) {
            return;
        }
        records.stream().filter(Objects::nonNull).forEach(record -> {
            AdminUserRespDTO user = userMap.get(record.getUserId());
            if (user == null) {
                return;
            }
            record.setUserName(user.getNickname());
            record.setUserMobile(user.getMobile());
        });
    }
}
