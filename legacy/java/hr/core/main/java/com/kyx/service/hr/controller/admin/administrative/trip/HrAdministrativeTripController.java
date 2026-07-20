package com.kyx.service.hr.controller.admin.administrative.trip;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.hr.controller.admin.administrative.trip.vo.HrTripPageReqVO;
import com.kyx.service.hr.controller.admin.administrative.trip.vo.HrTripRespVO;
import com.kyx.service.hr.controller.admin.administrative.trip.vo.HrTripSaveReqVO;
import com.kyx.service.hr.dal.dataobject.administrative.HrAdministrativeTripDO;
import com.kyx.service.hr.service.administrative.trip.HrAdministrativeTripService;
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

@Tag(name = "管理后台 - 出差管理")
@RestController
@RequestMapping("/hr/administrative/trip")
@Validated
public class HrAdministrativeTripController {

    @Resource
    private HrAdministrativeTripService tripService;

    @PostMapping("/create")
    @Operation(summary = "创建出差申请")
    @PreAuthorize("@ss.hasPermission('hr:administrative-trip:create')")
    public CommonResult<Long> createTrip(@Valid @RequestBody HrTripSaveReqVO createReqVO) {
        return success(tripService.createTrip(getLoginUserId(), createReqVO));
    }

    @PostMapping("/update")
    @Operation(summary = "更新出差申请")
    @PreAuthorize("@ss.hasPermission('hr:administrative-trip:update')")
    public CommonResult<Boolean> updateTrip(@Valid @RequestBody HrTripSaveReqVO updateReqVO) {
        tripService.updateTrip(updateReqVO);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得出差申请")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:administrative-trip:query')")
    public CommonResult<HrTripRespVO> getTrip(@RequestParam("id") Long id) {
        HrAdministrativeTripDO trip = tripService.getTrip(id);
        return success(BeanUtils.toBean(trip, HrTripRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得出差申请分页")
    @PreAuthorize("@ss.hasPermission('hr:administrative-trip:query')")
    public CommonResult<PageResult<HrTripRespVO>> getTripPage(@Valid HrTripPageReqVO pageReqVO) {
        PageResult<HrAdministrativeTripDO> pageResult = tripService.getTripPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HrTripRespVO.class));
    }
}
