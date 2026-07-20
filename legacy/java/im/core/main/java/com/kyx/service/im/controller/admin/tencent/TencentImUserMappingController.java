package com.kyx.service.im.controller.admin.tencent;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.im.controller.admin.tencent.vo.TencentImUserMappingCreateReqVO;
import com.kyx.service.im.controller.admin.tencent.vo.TencentImUserMappingPageReqVO;
import com.kyx.service.im.controller.admin.tencent.vo.TencentImUserMappingRespVO;
import com.kyx.service.im.controller.admin.tencent.vo.TencentImUserMappingUpdateReqVO;
import com.kyx.service.im.dal.dataobject.tencent.TencentImUserMappingDO;
import com.kyx.service.im.service.tencent.TencentImUserMappingService;
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

@Tag(name = "Admin - Tencent IM user mapping")
@RestController
@RequestMapping("/im/tencent-user-mapping")
@Validated
public class TencentImUserMappingController {

    @Resource
    private TencentImUserMappingService userMappingService;

    @PostMapping("/create")
    @Operation(summary = "Create Tencent IM user mapping")
    @PreAuthorize("@ss.hasPermission('im:tencent-user-mapping:create')")
    public CommonResult<Long> createUserMapping(@Valid @RequestBody TencentImUserMappingCreateReqVO createReqVO) {
        return success(userMappingService.createUserMapping(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "Update Tencent IM user mapping")
    @PreAuthorize("@ss.hasPermission('im:tencent-user-mapping:update')")
    public CommonResult<Boolean> updateUserMapping(@Valid @RequestBody TencentImUserMappingUpdateReqVO updateReqVO) {
        userMappingService.updateUserMapping(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Delete Tencent IM user mapping")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('im:tencent-user-mapping:delete')")
    public CommonResult<Boolean> deleteUserMapping(@RequestParam("id") Long id) {
        userMappingService.deleteUserMapping(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "Get Tencent IM user mapping")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('im:tencent-user-mapping:query')")
    public CommonResult<TencentImUserMappingRespVO> getUserMapping(@RequestParam("id") Long id) {
        TencentImUserMappingDO mapping = userMappingService.getUserMapping(id);
        return success(BeanUtils.toBean(mapping, TencentImUserMappingRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "Page Tencent IM user mappings")
    @PreAuthorize("@ss.hasPermission('im:tencent-user-mapping:query')")
    public CommonResult<PageResult<TencentImUserMappingRespVO>> getUserMappingPage(
            @Valid TencentImUserMappingPageReqVO pageReqVO) {
        PageResult<TencentImUserMappingDO> pageResult = userMappingService.getUserMappingPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, TencentImUserMappingRespVO.class));
    }
}
