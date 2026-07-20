package com.kyx.service.biz.controller.admin.hotel;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.biz.controller.admin.hotel.vo.HotelDashboardRespVO;
import com.kyx.service.biz.controller.admin.hotel.vo.HotelWorkOrderPageReqVO;
import com.kyx.service.biz.controller.admin.hotel.vo.HotelPermissionRespVO;
import com.kyx.service.biz.controller.admin.hotel.vo.HotelWorkOrderRespVO;
import com.kyx.service.biz.controller.admin.hotel.vo.HotelWorkOrderSaveReqVO;
import com.kyx.service.biz.controller.admin.hotel.vo.HotelWorkOrderStatusReqVO;
import com.kyx.service.biz.service.hotel.HotelWorkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static com.kyx.foundation.common.pojo.CommonResult.success;
import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "Admin - Hotel Work Order")
@RestController
@RequestMapping("/business/hotel/work-order")
@Validated
public class HotelWorkOrderController {
    private static final String HOTEL_ADMIN_ROLE_CHECK = "@ss.hasAnyRoles('super_admin','tenant_admin','system_admin','biz_boss')";
    private static final String HOTEL_DASHBOARD_PERMISSION = HOTEL_ADMIN_ROLE_CHECK + " || @ss.hasPermission('hotel:dashboard:query')";
    @Resource
    private HotelWorkOrderService hotelWorkOrderService;

    @PostMapping("/create")
    @Operation(summary = "创建酒店工单")
    public CommonResult<Long> create(@Valid @RequestBody HotelWorkOrderSaveReqVO reqVO) {
        return success(hotelWorkOrderService.create(reqVO, getLoginUserId()));
    }

    @GetMapping("/page")
    @Operation(summary = "酒店工单分页")
    public CommonResult<PageResult<HotelWorkOrderRespVO>> page(@Valid HotelWorkOrderPageReqVO reqVO) {
        return success(hotelWorkOrderService.page(reqVO, getLoginUserId(), false));
    }

    @GetMapping("/my-page")
    @Operation(summary = "我的酒店工单分页")
    public CommonResult<PageResult<HotelWorkOrderRespVO>> myPage(@Valid HotelWorkOrderPageReqVO reqVO) {
        return success(hotelWorkOrderService.page(reqVO, getLoginUserId(), true));
    }

    @GetMapping("/get")
    @Operation(summary = "酒店工单详情")
    public CommonResult<HotelWorkOrderRespVO> get(@RequestParam("id") Long id) {
        return success(hotelWorkOrderService.get(id, getLoginUserId()));
    }


    @PutMapping("/update")
    @Operation(summary = "编辑酒店工单")
    public CommonResult<Boolean> update(@Valid @RequestBody HotelWorkOrderSaveReqVO reqVO) {
        hotelWorkOrderService.update(reqVO, getLoginUserId());
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除酒店工单")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        hotelWorkOrderService.delete(id, getLoginUserId());
        return success(true);
    }

    @PutMapping("/status")
    @Operation(summary = "更新酒店工单状态")
    public CommonResult<Boolean> updateStatus(@Valid @RequestBody HotelWorkOrderStatusReqVO reqVO) {
        hotelWorkOrderService.updateStatus(reqVO, getLoginUserId());
        return success(true);
    }

    @PutMapping("/submit-acceptance")
    @Operation(summary = "提交酒店工单验收")
    public CommonResult<Boolean> submitAcceptance(@Valid @RequestBody HotelWorkOrderStatusReqVO reqVO) {
        hotelWorkOrderService.submitAcceptance(reqVO, getLoginUserId());
        return success(true);
    }

    @PutMapping("/accept-pass")
    @Operation(summary = "酒店工单验收通过")
    public CommonResult<Boolean> acceptPass(@Valid @RequestBody HotelWorkOrderStatusReqVO reqVO) {
        hotelWorkOrderService.acceptPass(reqVO, getLoginUserId());
        return success(true);
    }

    @PutMapping("/accept-reject")
    @Operation(summary = "酒店工单验收驳回")
    public CommonResult<Boolean> acceptReject(@Valid @RequestBody HotelWorkOrderStatusReqVO reqVO) {
        hotelWorkOrderService.acceptReject(reqVO, getLoginUserId());
        return success(true);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "酒店驾驶舱")
    @PreAuthorize(HOTEL_DASHBOARD_PERMISSION)
    public CommonResult<HotelDashboardRespVO> dashboard(@RequestParam(value = "store", required = false) String store) {
        return success(hotelWorkOrderService.dashboard(store, getLoginUserId()));
    }

    @GetMapping("/badge")
    @Operation(summary = "酒店工单角标")
    public CommonResult<Integer> badge() {
        return success(hotelWorkOrderService.badge(getLoginUserId()));
    }

    @GetMapping("/permission")
    @Operation(summary = "酒店模块权限")
    public CommonResult<HotelPermissionRespVO> permission() {
        return success(hotelWorkOrderService.permission(getLoginUserId()));
    }
}
