package com.kyx.service.business.api.user;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.collection.CollUtil;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.util.collection.CollectionUtils;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.kyx.service.business.api.user.dto.UserFunctionSyncReqDTO;
import com.kyx.service.business.api.user.dto.UserFunctionSyncRespDTO;
import com.kyx.service.business.api.user.dto.UserSyncUpsertReqDTO;
import com.kyx.service.business.api.user.dto.UserOnboardingCreateReqDTO;
import com.kyx.service.business.api.user.dto.UserOnboardingRespDTO;
import com.kyx.service.business.enums.ApiConstants;
import com.fhs.core.trans.anno.AutoTrans;
import com.fhs.trans.service.AutoTransable;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.Valid;

import java.util.*;

import static com.kyx.service.business.api.user.AdminUserApi.PREFIX;

@FeignClient(name = ApiConstants.NAME, contextId = "adminUserApi") // TODO ：fallbackFactory =
@Tag(name = "RPC 服务 - 管理员用户")
@AutoTrans(namespace = PREFIX, fields = {"nickname"})
public interface AdminUserApi extends AutoTransable<AdminUserRespDTO> {

    String PREFIX = ApiConstants.PREFIX + "/user";

    @GetMapping(PREFIX + "/get")
    @Operation(summary = "通过用户 ID 查询用户")
    @Parameter(name = "id", description = "用户编号", example = "1", required = true)
    CommonResult<AdminUserRespDTO> getUser(@RequestParam("id") Long id);

    @GetMapping(PREFIX + "/get-by-mobile")
    @Operation(summary = "通过手机号查询用户")
    @Parameter(name = "mobile", description = "手机号", example = "13800138000", required = true)
    CommonResult<AdminUserRespDTO> getUserByMobile(@RequestParam("mobile") String mobile);

    @GetMapping(PREFIX + "/list-by-subordinate")
    @Operation(summary = "通过用户 ID 查询用户下属")
    @Parameter(name = "id", description = "用户编号", example = "1", required = true)
    CommonResult<List<AdminUserRespDTO>> getUserListBySubordinate(@RequestParam("id") Long id);

    @GetMapping(PREFIX + "/list")
    @Operation(summary = "通过用户 ID 查询用户们")
    @Parameter(name = "ids", description = "部门编号数组", example = "1,2", required = true)
    CommonResult<List<AdminUserRespDTO>> getUserList(@RequestParam("ids") Collection<Long> ids);

    @GetMapping(PREFIX + "/list-by-dept-id")
    @Operation(summary = "获得指定部门的用户数组")
    @Parameter(name = "deptIds", description = "部门编号数组", example = "1,2", required = true)
    CommonResult<List<AdminUserRespDTO>> getUserListByDeptIds(@RequestParam("deptIds") Collection<Long> deptIds);

    @GetMapping(PREFIX + "/list-by-post-id")
    @Operation(summary = "获得指定岗位的用户数组")
    @Parameter(name = "postIds", description = "岗位编号数组", example = "2,3", required = true)
    CommonResult<List<AdminUserRespDTO>> getUserListByPostIds(@RequestParam("postIds") Collection<Long> postIds);

    @GetMapping(PREFIX + "/list-by-role-id")
    @Operation(summary = "获得指定角色的用户数组")
    @Parameter(name = "roleIds", description = "角色编号数组", example = "1,2", required = true)
    CommonResult<List<AdminUserRespDTO>> getUserListByRoleIds(@RequestParam("roleIds") Collection<Long> roleIds);

    @GetMapping(PREFIX + "/list-by-status")
    @Operation(summary = "获得指定状态的用户数组")
    @Parameter(name = "status", description = "用户状态", example = "0", required = true)
    CommonResult<List<AdminUserRespDTO>> getUserListByStatus(@RequestParam("status") Integer status);

    @PostMapping(PREFIX + "/create-onboarding-user")
    @Operation(summary = "创建入职用户账号")
    CommonResult<UserOnboardingRespDTO> createOnboardingUser(@Valid @RequestBody UserOnboardingCreateReqDTO reqDTO);

    @PostMapping(PREFIX + "/update-status")
    CommonResult<Boolean> updateUserStatus(@RequestParam("id") Long id,
                                           @RequestParam("status") Integer status);

    @PostMapping(PREFIX + "/update-dept")
    CommonResult<Boolean> updateUserDept(@RequestParam("id") Long id,
                                         @RequestParam("deptId") Long deptId);

    @PostMapping(PREFIX + "/update-mobile")
    CommonResult<Boolean> updateUserMobile(@RequestParam("id") Long id,
                                           @RequestParam("mobile") String mobile);

    @PostMapping(PREFIX + "/sync-upsert-batch")
    CommonResult<Integer> upsertUserSyncBatch(@RequestBody List<UserSyncUpsertReqDTO> reqDTOList);

    @PostMapping(PREFIX + "/sync-functions-batch")
    CommonResult<List<UserFunctionSyncRespDTO>> syncUserFunctionsBatch(@RequestBody List<UserFunctionSyncReqDTO> reqDTOList);

    @PostMapping(PREFIX + "/sync-mark-inactive")
    CommonResult<Integer> markUserSyncInactive(@RequestBody Collection<String> externalUserIds);

    @PostMapping(PREFIX + "/sync-mark-synced")
    CommonResult<Integer> markUserSyncSynced(@RequestBody Collection<String> externalUserIds);

    /**
     * 获得用户 Map
     *
     * @param ids 用户编号数组
     * @return 用户 Map
     */
    default Map<Long, AdminUserRespDTO> getUserMap(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return new HashMap<>();
        }
        List<AdminUserRespDTO> list = getUserList(ids).getCheckedData();
        return CollectionUtils.convertMap(list, AdminUserRespDTO::getId);
    }

    /**
     * 校验用户是否有效。如下情况，视为无效：
     * 1. 用户编号不存在
     * 2. 用户被禁用
     *
     * @param id 用户编号
     */
    default void validateUser(Long id) {
        validateUserList(Collections.singleton(id));
    }

    @GetMapping(PREFIX + "/valid")
    @Operation(summary = "校验用户们是否有效")
    @Parameter(name = "ids", description = "用户编号数组", example = "3,5", required = true)
    CommonResult<Boolean> validateUserList(@RequestParam("ids") Collection<Long> ids);

    @Override
    @GetMapping("select")
    default List<AdminUserRespDTO> selectByIds(List<?> ids) {
        return getUserList(Convert.toList(Long.class, ids)).getCheckedData();
    }

    @Override
    @GetMapping("select-list")
    default AdminUserRespDTO selectById(Object id) {
        return getUser(Convert.toLong(id)).getCheckedData();
    }

}
