package com.kyx.service.hr.controller.admin.administrative.meetingroom;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.hr.controller.admin.administrative.meetingroom.vo.HrMeetingRoomPageReqVO;
import com.kyx.service.hr.controller.admin.administrative.meetingroom.vo.HrMeetingRoomRespVO;
import com.kyx.service.hr.controller.admin.administrative.meetingroom.vo.HrMeetingRoomSaveReqVO;
import com.kyx.service.hr.dal.dataobject.administrative.HrAdministrativeMeetingRoomDO;
import com.kyx.service.hr.service.administrative.meetingroom.HrAdministrativeMeetingRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 会议室管理")
@RestController
@RequestMapping("/hr/administrative/meeting-room")
@Validated
public class HrAdministrativeMeetingRoomController {

    @Resource
    private HrAdministrativeMeetingRoomService meetingRoomService;

    @PostMapping("/create")
    @Operation(summary = "创建会议室")
    @PreAuthorize("@ss.hasPermission('hr:administrative-meeting-room:create')")
    public CommonResult<Long> createMeetingRoom(@Valid @RequestBody HrMeetingRoomSaveReqVO createReqVO) {
        return success(meetingRoomService.createMeetingRoom(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新会议室")
    @PreAuthorize("@ss.hasPermission('hr:administrative-meeting-room:update')")
    public CommonResult<Boolean> updateMeetingRoom(@Valid @RequestBody HrMeetingRoomSaveReqVO updateReqVO) {
        meetingRoomService.updateMeetingRoom(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除会议室")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:administrative-meeting-room:delete')")
    public CommonResult<Boolean> deleteMeetingRoom(@RequestParam("id") Long id) {
        meetingRoomService.deleteMeetingRoom(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得会议室")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:administrative-meeting-room:query')")
    public CommonResult<HrMeetingRoomRespVO> getMeetingRoom(@RequestParam("id") Long id) {
        HrAdministrativeMeetingRoomDO meetingRoom = meetingRoomService.getMeetingRoom(id);
        return success(BeanUtils.toBean(meetingRoom, HrMeetingRoomRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得会议室分页")
    @PreAuthorize("@ss.hasPermission('hr:administrative-meeting-room:query')")
    public CommonResult<PageResult<HrMeetingRoomRespVO>> getMeetingRoomPage(
            @Valid HrMeetingRoomPageReqVO pageReqVO) {
        PageResult<HrMeetingRoomRespVO> pageResult = meetingRoomService.getMeetingRoomPage(pageReqVO);
        return success(pageResult);
    }
}
