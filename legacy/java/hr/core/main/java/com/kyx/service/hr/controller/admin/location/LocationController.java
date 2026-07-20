package com.kyx.service.hr.controller.admin.location;

import com.kyx.foundation.apilog.core.annotation.ApiAccessLog;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.hr.controller.admin.location.vo.LocationPageReqVO;
import com.kyx.service.hr.controller.admin.location.vo.LocationRespVO;
import com.kyx.service.hr.controller.admin.location.vo.LocationSaveReqVO;
import com.kyx.service.hr.dal.dataobject.location.LocationDO;
import com.kyx.service.hr.service.location.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static com.kyx.foundation.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.kyx.foundation.common.pojo.CommonResult.success;

/**
 * 公司地点管理 Controller
 *
 * @author MK
 */
@Tag(name = "管理后台 - 公司地点管理")
@RestController
@RequestMapping("/hr/location")
@Validated
public class LocationController {

    @Resource
    private LocationService locationService;

    @PostMapping("/create")
    @Operation(summary = "创建地点")
    @PreAuthorize("@ss.hasPermission('hr:location:create')")
    public CommonResult<Long> createLocation(@Valid @RequestBody LocationSaveReqVO createReqVO) {
        return success(locationService.createLocation(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新地点")
    @PreAuthorize("@ss.hasPermission('hr:location:update')")
    public CommonResult<Boolean> updateLocation(@Valid @RequestBody LocationSaveReqVO updateReqVO) {
        locationService.updateLocation(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除地点")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:location:delete')")
    public CommonResult<Boolean> deleteLocation(@RequestParam("id") Long id) {
        locationService.deleteLocation(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得地点")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:location:query')")
    public CommonResult<LocationRespVO> getLocation(@RequestParam("id") Long id) {
        LocationDO location = locationService.getLocation(id);
        return success(BeanUtils.toBean(location, LocationRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得地点分页")
    @PreAuthorize("@ss.hasPermission('hr:location:query')")
    public CommonResult<PageResult<LocationRespVO>> getLocationPage(@Valid LocationPageReqVO pageVO) {
        PageResult<LocationRespVO> pageResult = locationService.getLocationPage(pageVO);
        return success(pageResult);
    }

} 