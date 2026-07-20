package com.kyx.service.hr.controller.rpc;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.hr.api.onboarding.dto.OnboardingPublicReqDTO;
import com.kyx.service.hr.api.onboarding.dto.OnboardingPublicRespDTO;
import com.kyx.service.hr.api.onboarding.dto.OnboardingRespDTO;
import com.kyx.service.hr.dal.dataobject.onboarding.OnboardingDO;
import com.kyx.service.hr.service.onboarding.OnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

import static com.kyx.foundation.common.pojo.CommonResult.success;

/**
 * RPC 服务 - 入职管理
 *
 * @author MK
 */
@Tag(name = "RPC 服务 - 入职管理")
@RestController
@RequestMapping("/rpc-api/hr/onboarding")
@Validated
public class OnboardingRpcController {

    @Resource
    private OnboardingService onboardingService;



} 