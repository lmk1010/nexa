package com.kyx.service.hr.api.onboarding;

import cn.hutool.core.convert.Convert;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.util.collection.CollectionUtils;
import com.kyx.service.hr.api.onboarding.dto.OnboardingPublicReqDTO;
import com.kyx.service.hr.api.onboarding.dto.OnboardingPublicRespDTO;
import com.kyx.service.hr.api.onboarding.dto.OnboardingRespDTO;
import com.kyx.service.hr.enums.ApiConstants;
import com.fhs.core.trans.anno.AutoTrans;
import com.fhs.trans.service.AutoTransable;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;
import java.util.*;

import static com.kyx.service.hr.api.onboarding.OnboardingApi.PREFIX;

@FeignClient(name = ApiConstants.NAME) // TODO ：fallbackFactory =
@Tag(name = "RPC 服务 - 入职管理")
@AutoTrans(namespace = PREFIX, fields = {"applicantName"})
public interface OnboardingApi extends AutoTransable<OnboardingRespDTO> {

    String PREFIX = ApiConstants.PREFIX + "/onboarding";

    @GetMapping(PREFIX + "/get")
    @Operation(summary = "通过入职申请 ID 查询入职申请")
    @Parameter(name = "id", description = "入职申请编号", example = "1", required = true)
    CommonResult<OnboardingRespDTO> getOnboarding(@RequestParam("id") Long id);

    @GetMapping(PREFIX + "/list")
    @Operation(summary = "通过入职申请 ID 查询入职申请们")
    @Parameter(name = "ids", description = "入职申请编号数组", example = "1,2", required = true)
    CommonResult<List<OnboardingRespDTO>> getOnboardingList(@RequestParam("ids") Collection<Long> ids);

    @GetMapping(PREFIX + "/get-by-application-no")
    @Operation(summary = "通过申请编号查询入职申请")
    @Parameter(name = "applicationNo", description = "申请编号", example = "ON20250120001", required = true)
    CommonResult<OnboardingRespDTO> getOnboardingByApplicationNo(@RequestParam("applicationNo") String applicationNo);

    @GetMapping(PREFIX + "/list-by-status")
    @Operation(summary = "获得指定状态的入职申请数组")
    @Parameter(name = "status", description = "入职申请状态", example = "1", required = true)
    CommonResult<List<OnboardingRespDTO>> getOnboardingListByStatus(@RequestParam("status") Integer status);

    /**
     * 获得入职申请 Map
     *
     * @param ids 入职申请编号数组
     * @return 入职申请 Map
     */
    default Map<Long, OnboardingRespDTO> getOnboardingMap(Collection<Long> ids) {
        List<OnboardingRespDTO> onboardings = getOnboardingList(ids).getCheckedData();
        return CollectionUtils.convertMap(onboardings, OnboardingRespDTO::getId);
    }

    /**
     * 校验入职申请是否有效。如下情况，视为无效：
     * 1. 入职申请编号不存在
     * 2. 入职申请已取消或拒绝
     *
     * @param id 入职申请编号
     */
    default void validateOnboarding(Long id) {
        validateOnboardingList(Collections.singleton(id));
    }

    @GetMapping(PREFIX + "/valid")
    @Operation(summary = "校验入职申请们是否有效")
    @Parameter(name = "ids", description = "入职申请编号数组", example = "3,5", required = true)
    CommonResult<Boolean> validateOnboardingList(@RequestParam("ids") Collection<Long> ids);

    @PostMapping(PREFIX + "/public/create")
    @Operation(summary = "提交入职申请")
    CommonResult<OnboardingPublicRespDTO> submitOnboarding(@Valid @RequestBody OnboardingPublicReqDTO createReqDTO);



    @Override
    @GetMapping("select")
    default List<OnboardingRespDTO> selectByIds(List<?> ids) {
        return getOnboardingList(Convert.toList(Long.class, ids)).getCheckedData();
    }

    @Override
    @GetMapping("select-list")
    default OnboardingRespDTO selectById(Object id) {
        return getOnboarding(Convert.toLong(id)).getCheckedData();
    }

} 