package com.kyx.service.business.controller.admin.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.apilog.core.annotation.ApiAccessLog;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.enums.UserTypeEnum;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageParam;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.excel.core.util.ExcelUtils;
import com.kyx.foundation.tenant.core.aop.TenantIgnore;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.service.business.controller.admin.user.vo.user.*;
import com.kyx.service.business.convert.user.UserConvert;
import com.kyx.service.business.dal.dataobject.dept.DeptDO;
import com.kyx.service.business.dal.dataobject.permission.RoleDO;
import com.kyx.service.business.dal.dataobject.social.SocialUserBindDO;
import com.kyx.service.business.dal.dataobject.tenant.TenantDO;
import com.kyx.service.business.dal.dataobject.tenant.UserTenantRelationDO;
import com.kyx.service.business.dal.dataobject.oauth2.OAuth2OnlineUserSummaryDO;
import com.kyx.service.business.dal.dataobject.user.AdminUserDO;
import com.kyx.service.business.dal.mysql.oauth2.OAuth2AccessTokenMapper;
import com.kyx.service.business.dal.mysql.social.SocialUserBindMapper;
import com.kyx.service.business.enums.social.SocialTypeEnum;
import com.kyx.service.business.enums.common.SexEnum;
import com.kyx.service.business.service.dept.DeptService;
import com.kyx.service.business.service.permission.PermissionService;
import com.kyx.service.business.service.permission.RoleService;
import com.kyx.service.business.service.tenant.TenantService;
import com.kyx.service.business.service.tenant.UserTenantRelationService;
import com.kyx.service.business.service.user.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.kyx.foundation.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.kyx.foundation.common.pojo.CommonResult.success;
import static com.kyx.foundation.common.util.collection.CollectionUtils.convertList;
import static com.kyx.foundation.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - 用户")
@RestController
@RequestMapping("/system/user")
@Validated
public class UserController {

    @Resource
    private AdminUserService userService;
    @Resource
    private DeptService deptService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private RoleService roleService;
    @Resource
    private TenantService tenantService;
    @Resource
    private UserTenantRelationService userTenantRelationService;
    @Resource
    private SocialUserBindMapper socialUserBindMapper;
    @Resource
    private OAuth2AccessTokenMapper oauth2AccessTokenMapper;

    @PostMapping("/create")
    @Operation(summary = "新增用户")
    @PreAuthorize("@ss.hasPermission('system:user:create')")
    public CommonResult<Long> createUser(@Valid @RequestBody UserSaveReqVO reqVO) {
        Long id = userService.createUser(reqVO);
        return success(id);
    }

    @PostMapping("/create-onboarding-user")
    @Operation(summary = "创建入职用户账号")
    @PreAuthorize("@ss.hasPermission('system:user:create')")
    public CommonResult<UserOnboardingRespVO> createOnboardingUser(@Valid @RequestBody UserOnboardingCreateReqVO reqVO) {
        UserOnboardingRespVO respVO = userService.createOnboardingUser(reqVO);
        return success(respVO);
    }

    @PutMapping("update")
    @Operation(summary = "修改用户")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> updateUser(@Valid @RequestBody UserSaveReqVO reqVO) {
        userService.updateUser(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除用户")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:user:delete')")
    public CommonResult<Boolean> deleteUser(@RequestParam("id") Long id) {
        userService.deleteUser(id);
        return success(true);
    }

    @PutMapping("/update-password")
    @Operation(summary = "重置用户密码")
    @PreAuthorize("@ss.hasPermission('system:user:update-password')")
    public CommonResult<Boolean> updateUserPassword(@Valid @RequestBody UserUpdatePasswordReqVO reqVO) {
        userService.updateUserPassword(reqVO.getId(), reqVO.getPassword());
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "修改用户状态")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> updateUserStatus(@Valid @RequestBody UserUpdateStatusReqVO reqVO) {
        userService.updateUserStatus(reqVO.getId(), reqVO.getStatus());
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获得用户分页列表")
    @PreAuthorize("@ss.hasPermission('system:user:query')")
    public CommonResult<PageResult<UserRespVO>> getUserPage(@Valid UserPageReqVO pageReqVO) {
        // 获得用户分页列表
        PageResult<AdminUserDO> pageResult = userService.getUserPage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return success(new PageResult<>(pageResult.getTotal()));
        }
        // 拼接数据
        Map<Long, DeptDO> deptMap = deptService.getDeptMap(
                convertList(pageResult.getList(), AdminUserDO::getDeptId));
        List<UserRespVO> userRespVOList = UserConvert.INSTANCE.convertList(pageResult.getList(), deptMap);
        fillUserRoleNames(userRespVOList);
        fillDingTalkBindStatus(userRespVOList);
        fillUserTenantInfo(userRespVOList);
        fillUserOnlineStatus(userRespVOList);
        return success(new PageResult<>(userRespVOList, pageResult.getTotal()));
    }

    @GetMapping({"/list-all-simple", "/simple-list"})
    @Operation(summary = "获取用户精简信息列表", description = "只包含被开启的用户，主要用于前端的下拉选项")
    @TenantIgnore
    public CommonResult<List<UserSimpleRespVO>> getSimpleUserList() {
        List<AdminUserDO> list = userService.getUserListByStatus(CommonStatusEnum.ENABLE.getStatus());
        // 拼接数据
        Map<Long, DeptDO> deptMap = deptService.getDeptMap(
                convertList(list, AdminUserDO::getDeptId));
        List<UserSimpleRespVO> respVOList = UserConvert.INSTANCE.convertSimpleList(list, deptMap);
        fillSimpleUserTenantInfo(respVOList, resolveSimpleTenantIds(null));
        return success(respVOList);
    }

    @GetMapping("/simple-list-by-tenants")
    @Operation(summary = "根据租户ID列表获取用户精简信息列表", description = "支持跨租户查询用户，用于流程审批人选择")
    @Parameters({
            @Parameter(name = "tenantIds", description = "租户ID列表，逗号分隔。为空则查询当前租户", example = "1,2,3"),
            @Parameter(name = "modelId", description = "流程模型ID，用于兼容旧调用", example = "123"),
            @Parameter(name = "featureCode", description = "功能编码，用于判断是否允许跨租户范围", example = "work.requirement")
    })
    @TenantIgnore
    public CommonResult<List<UserSimpleRespVO>> getSimpleUserListByTenants(
            @RequestParam(value = "tenantIds", required = false) String tenantIds,
            @RequestParam(value = "modelId", required = false) Long modelId,
            @RequestParam(value = "featureCode", required = false) String featureCode) {
        List<AdminUserDO> list = userService.getUserListByTenants(tenantIds, modelId, featureCode);
        // 拼接数据
        Map<Long, DeptDO> deptMap = deptService.getDeptMap(
                convertList(list, AdminUserDO::getDeptId));
        List<UserSimpleRespVO> respVOList = UserConvert.INSTANCE.convertSimpleList(list, deptMap);
        fillSimpleUserTenantInfo(respVOList, resolveSimpleTenantIds(tenantIds));
        return success(respVOList);
    }

    @GetMapping("/get")
    @Operation(summary = "获得用户详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:user:query')")
    public CommonResult<UserRespVO> getUser(@RequestParam("id") Long id) {
        AdminUserDO user = userService.getManageableUser(id);
        if (user == null) {
            return success(null);
        }
        // 拼接数据
        DeptDO dept = deptService.getDept(user.getDeptId());
        UserRespVO userRespVO = UserConvert.INSTANCE.convert(user, dept);
        fillUserRoleNames(Collections.singletonList(userRespVO));
        fillDingTalkBindStatus(Collections.singletonList(userRespVO));
        fillUserTenantInfo(Collections.singletonList(userRespVO));
        fillUserOnlineStatus(Collections.singletonList(userRespVO));
        return success(userRespVO);
    }

    @GetMapping("/get-by-mobile")
    @Operation(summary = "通过手机号获得用户详情")
    @Parameter(name = "mobile", description = "手机号", required = true, example = "13800138000")
    @PreAuthorize("@ss.hasPermission('system:user:query')")
    public CommonResult<UserRespVO> getUserByMobile(@RequestParam("mobile") String mobile) {
        AdminUserDO user = userService.getUserByMobile(mobile);
        if (user != null) {
            user = userService.getManageableUser(user.getId());
        }
        if (user == null) {
            return success(null);
        }
        // 拼接数据
        DeptDO dept = deptService.getDept(user.getDeptId());
        UserRespVO userRespVO = UserConvert.INSTANCE.convert(user, dept);
        fillUserRoleNames(Collections.singletonList(userRespVO));
        fillDingTalkBindStatus(Collections.singletonList(userRespVO));
        fillUserTenantInfo(Collections.singletonList(userRespVO));
        fillUserOnlineStatus(Collections.singletonList(userRespVO));
        return success(userRespVO);
    }

    @GetMapping("/export")
    @Operation(summary = "导出用户")
    @PreAuthorize("@ss.hasPermission('system:user:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportUserList(@Validated UserPageReqVO exportReqVO,
                               HttpServletResponse response) throws IOException {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<AdminUserDO> list = userService.getUserPage(exportReqVO).getList();
        // 输出 Excel
        Map<Long, DeptDO> deptMap = deptService.getDeptMap(
                convertList(list, AdminUserDO::getDeptId));
        ExcelUtils.write(response, "用户数据.xls", "数据", UserRespVO.class,
                UserConvert.INSTANCE.convertList(list, deptMap));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得导入用户模板")
    public void importTemplate(HttpServletResponse response) throws IOException {
        // 手动创建导出 demo
        List<UserImportExcelVO> list = Arrays.asList(
                UserImportExcelVO.builder().username("用户名(删除)").deptId(1L).email("ceshi@shanchu.com(删除)").mobile("1898888888(删除)")
                        .nickname("测试名称(删除)").status(CommonStatusEnum.ENABLE.getStatus()).sex(SexEnum.MALE.getSex()).build()
        );
        // 输出
        ExcelUtils.write(response, "用户导入模板.xls", "用户列表", UserImportExcelVO.class, list);
    }

    @PostMapping("/import")
    @Operation(summary = "导入用户")
    @Parameters({
            @Parameter(name = "file", description = "Excel 文件", required = true),
            @Parameter(name = "updateSupport", description = "是否支持更新，默认为 false", example = "true")
    })
    @PreAuthorize("@ss.hasPermission('system:user:import')")
    public CommonResult<UserImportRespVO> importExcel(@RequestParam("file") MultipartFile file,
                                                      @RequestParam(value = "updateSupport", required = false, defaultValue = "false") Boolean updateSupport) throws Exception {
        List<UserImportExcelVO> list = ExcelUtils.read(file, UserImportExcelVO.class);
        UserImportRespVO result = userService.importUserList(list, updateSupport);
        return success(result);
    }

    private void fillUserRoleNames(List<UserRespVO> userRespVOList) {
        if (CollUtil.isEmpty(userRespVOList)) {
            return;
        }
        Map<Long, Set<Long>> userRoleIdsMap = new HashMap<>(userRespVOList.size());
        Set<Long> allRoleIds = new HashSet<>();
        for (UserRespVO userRespVO : userRespVOList) {
            Set<Long> roleIds = permissionService.getUserRoleIdListByUserIdFromCache(userRespVO.getId());
            userRoleIdsMap.put(userRespVO.getId(), roleIds);
            allRoleIds.addAll(roleIds);
        }
        Map<Long, String> roleNameMap = new HashMap<>();
        if (CollUtil.isNotEmpty(allRoleIds)) {
            for (RoleDO role : roleService.getRoleListFromCache(allRoleIds)) {
                roleNameMap.put(role.getId(), role.getName());
            }
        }
        for (UserRespVO userRespVO : userRespVOList) {
            Set<Long> roleIds = userRoleIdsMap.get(userRespVO.getId());
            if (CollUtil.isEmpty(roleIds)) {
                userRespVO.setRoleNames(Collections.emptyList());
                continue;
            }
            List<String> roleNames = roleIds.stream()
                    .map(roleNameMap::get)
                    .filter(StrUtil::isNotBlank)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
            userRespVO.setRoleNames(roleNames);
        }
    }

    private void fillDingTalkBindStatus(List<UserRespVO> userRespVOList) {
        if (CollUtil.isEmpty(userRespVOList)) {
            return;
        }
        List<SocialUserBindDO> bindings = socialUserBindMapper.selectListByUserIdsAndUserTypeAndSocialType(
                convertSet(userRespVOList, UserRespVO::getId),
                UserTypeEnum.ADMIN.getValue(),
                SocialTypeEnum.DINGTALK.getType()
        );
        Set<Long> boundUserIds = convertSet(bindings, SocialUserBindDO::getUserId);
        for (UserRespVO userRespVO : userRespVOList) {
            userRespVO.setDingTalkBound(boundUserIds.contains(userRespVO.getId()));
        }
    }

    private void fillUserOnlineStatus(List<UserRespVO> userRespVOList) {
        if (CollUtil.isEmpty(userRespVOList)) {
            return;
        }
        Set<Long> userIds = convertSet(userRespVOList, UserRespVO::getId);
        List<OAuth2OnlineUserSummaryDO> summaries = oauth2AccessTokenMapper.selectOnlineSummariesByUserIds(
                userIds,
                UserTypeEnum.ADMIN.getValue()
        );
        Map<Long, OAuth2OnlineUserSummaryDO> summaryMap = summaries.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        OAuth2OnlineUserSummaryDO::getUserId,
                        summary -> summary,
                        (left, right) -> left
                ));
        for (UserRespVO userRespVO : userRespVOList) {
            OAuth2OnlineUserSummaryDO summary = summaryMap.get(userRespVO.getId());
            long onlineSessionCount = summary == null || summary.getOnlineSessionCount() == null
                    ? 0L : summary.getOnlineSessionCount();
            long onlineDeviceCount = summary == null || summary.getOnlineDeviceCount() == null
                    ? 0L : summary.getOnlineDeviceCount();
            userRespVO.setOnline(onlineSessionCount > 0L);
            userRespVO.setOnlineSessionCount(onlineSessionCount);
            userRespVO.setOnlineDeviceCount(onlineDeviceCount);
            userRespVO.setLastActiveTime(summary == null ? null : summary.getLastActiveTime());
        }
    }

    private void fillUserTenantInfo(List<UserRespVO> userRespVOList) {
        if (CollUtil.isEmpty(userRespVOList)) {
            return;
        }
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            return;
        }
        TenantDO tenant = tenantService.getTenant(tenantId);
        String tenantName = tenant != null ? tenant.getName() : null;
        for (UserRespVO userRespVO : userRespVOList) {
            userRespVO.setTenantId(tenantId);
            userRespVO.setTenantName(tenantName);
        }
    }

    private List<Long> resolveSimpleTenantIds(String tenantIds) {
        LinkedHashSet<Long> targetTenantIds = new LinkedHashSet<>();
        if (StrUtil.isNotBlank(tenantIds)) {
            long[] tenantIdArray = StrUtil.splitToLong(tenantIds, ",");
            if (tenantIdArray != null) {
                for (long tenantId : tenantIdArray) {
                    targetTenantIds.add(tenantId);
                }
            }
        }
        if (targetTenantIds.isEmpty() && TenantContextHolder.getTenantId() != null) {
            targetTenantIds.add(TenantContextHolder.getTenantId());
        }
        return targetTenantIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void fillSimpleUserTenantInfo(List<UserSimpleRespVO> userRespVOList, List<Long> preferredTenantIds) {
        if (CollUtil.isEmpty(userRespVOList) || CollUtil.isEmpty(preferredTenantIds)) {
            return;
        }
        Set<Long> userIds = convertSet(userRespVOList, UserSimpleRespVO::getId);
        if (CollUtil.isEmpty(userIds)) {
            return;
        }
        Map<Long, Long> userTenantIdMap = new LinkedHashMap<>();
        for (Long tenantId : preferredTenantIds) {
            List<UserTenantRelationDO> relations = userTenantRelationService.getUserTenantRelationsByTenantId(tenantId);
            for (UserTenantRelationDO relation : relations) {
                if (relation == null
                        || !Objects.equals(relation.getStatus(), CommonStatusEnum.ENABLE.getStatus())
                        || !userIds.contains(relation.getUserId())) {
                    continue;
                }
                userTenantIdMap.putIfAbsent(relation.getUserId(), relation.getTenantId());
            }
        }
        Map<Long, String> tenantNameMap = tenantService.getTenantListByIds(preferredTenantIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(TenantDO::getId, TenantDO::getName, (left, right) -> left, LinkedHashMap::new));
        for (UserSimpleRespVO userRespVO : userRespVOList) {
            Long tenantId = userTenantIdMap.get(userRespVO.getId());
            if (tenantId == null) {
                continue;
            }
            userRespVO.setTenantId(tenantId);
            userRespVO.setTenantName(tenantNameMap.get(tenantId));
        }
    }

}
