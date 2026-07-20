package com.kyx.service.hr.controller.admin.administrative.meeting;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.hr.controller.admin.administrative.meeting.vo.HrMeetingPageReqVO;
import com.kyx.service.hr.controller.admin.administrative.meeting.vo.HrMeetingRespVO;
import com.kyx.service.hr.controller.admin.administrative.meeting.vo.HrMeetingSaveReqVO;
import com.kyx.service.hr.dal.dataobject.administrative.HrAdministrativeMeetingDO;
import com.kyx.service.hr.service.administrative.meeting.HrAdministrativeMeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static com.kyx.foundation.common.pojo.CommonResult.success;
import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 会议室预约")
@RestController
@RequestMapping("/hr/administrative/meeting")
@Validated
public class HrAdministrativeMeetingController {

    @Resource
    private HrAdministrativeMeetingService meetingService;

    @PostMapping("/create")
    @Operation(summary = "创建会议预约")
    @PreAuthorize("@ss.hasPermission('hr:administrative-meeting:create')")
    public CommonResult<Long> createMeeting(@Valid @RequestBody HrMeetingSaveReqVO createReqVO) {
        return success(meetingService.createMeeting(getLoginUserId(), createReqVO));
    }

    @PostMapping("/update")
    @Operation(summary = "更新会议预约")
    @PreAuthorize("@ss.hasPermission('hr:administrative-meeting:update')")
    public CommonResult<Boolean> updateMeeting(@Valid @RequestBody HrMeetingSaveReqVO updateReqVO) {
        meetingService.updateMeeting(updateReqVO);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得会议预约")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:administrative-meeting:query')")
    public CommonResult<HrMeetingRespVO> getMeeting(@RequestParam("id") Long id) {
        HrAdministrativeMeetingDO meeting = meetingService.getMeeting(id);
        return success(BeanUtils.toBean(meeting, HrMeetingRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得会议预约分页")
    @PreAuthorize("@ss.hasPermission('hr:administrative-meeting:query')")
    public CommonResult<PageResult<HrMeetingRespVO>> getMeetingPage(@Valid HrMeetingPageReqVO pageReqVO) {
        PageResult<HrAdministrativeMeetingDO> pageResult = meetingService.getMeetingPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HrMeetingRespVO.class));
    }
}
