package com.kyx.service.business.api.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.datapermission.core.util.DataPermissionUtils;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.kyx.service.business.api.user.dto.UserFunctionSyncReqDTO;
import com.kyx.service.business.api.user.dto.UserFunctionSyncRespDTO;
import com.kyx.service.business.api.user.dto.UserSyncUpsertReqDTO;
import com.kyx.service.business.api.user.dto.UserOnboardingCreateReqDTO;
import com.kyx.service.business.api.user.dto.UserOnboardingRespDTO;
import com.kyx.service.business.controller.admin.user.vo.user.UserOnboardingCreateReqVO;
import com.kyx.service.business.controller.admin.user.vo.user.UserOnboardingRespVO;
import com.kyx.service.business.dal.dataobject.dept.PostDO;
import com.kyx.service.business.dal.dataobject.dept.UserPostDO;
import com.kyx.service.business.dal.dataobject.dept.DeptDO;
import com.kyx.service.business.dal.dataobject.migration.UserSyncDO;
import com.kyx.service.business.dal.dataobject.permission.RoleDO;
import com.kyx.service.business.dal.dataobject.permission.UserRoleDO;
import com.kyx.service.business.dal.dataobject.tenant.UserTenantRelationDO;
import com.kyx.service.business.dal.dataobject.user.AdminUserDO;
import com.kyx.service.business.dal.mysql.dept.PostMapper;
import com.kyx.service.business.dal.mysql.dept.UserPostMapper;
import com.kyx.service.business.dal.mysql.migration.UserSyncMapper;
import com.kyx.service.business.dal.mysql.permission.RoleMapper;
import com.kyx.service.business.dal.mysql.permission.UserRoleMapper;
import com.kyx.service.business.dal.mysql.tenant.UserTenantRelationMapper;
import com.kyx.service.business.dal.mysql.user.AdminUserMapper;
import com.kyx.service.business.enums.permission.DataScopeEnum;
import com.kyx.service.business.enums.permission.RoleTypeEnum;
import com.kyx.service.business.service.dept.DeptService;
import com.kyx.service.business.service.permission.PermissionService;
import com.kyx.service.business.service.user.AdminUserService;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.time.LocalDateTime;
import java.util.zip.CRC32;

import static com.kyx.foundation.common.pojo.CommonResult.success;
import static com.kyx.foundation.common.util.collection.CollectionUtils.convertSet;

@RestController // 提供 RESTful API 接口，给 Feign 调用
@Validated
@Slf4j
public class AdminUserApiImpl implements AdminUserApi {

    @Resource
    private AdminUserService userService;
    @Resource
    private DeptService deptService;
    @Resource
    private UserSyncMapper userSyncMapper;
    @Resource
    private AdminUserMapper userMapper;
    @Resource
    private PostMapper postMapper;
    @Resource
    private UserPostMapper userPostMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private UserRoleMapper userRoleMapper;
    @Resource
    private UserTenantRelationMapper userTenantRelationMapper;
    @Resource
    private PermissionService permissionService;

    @Override
    public CommonResult<AdminUserRespDTO> getUser(Long id) {
        return DataPermissionUtils.executeIgnore(() -> {
            AdminUserDO user = userService.getUser(id);
            if (user != null) {
                log.info("AdminUserApiImpl.getUser: id={}, username={}, nickname={}",
                        user.getId(), user.getUsername(), user.getNickname());

                // 测试Bean转换
                AdminUserRespDTO dto = BeanUtils.toBean(user, AdminUserRespDTO.class);
                log.info("AdminUserApiImpl.getUser: 转换后 dto.username={}, dto.nickname={}",
                        dto.getUsername(), dto.getNickname());

                // 如果转换失败，手动设置字段
                if (dto.getUsername() == null && user.getUsername() != null) {
                    log.warn("BeanUtils转换失败，手动设置字段: username={}", user.getUsername());
                    dto.setUsername(user.getUsername());
                    dto.setNickname(user.getNickname());
                    dto.setId(user.getId());
                    dto.setMobile(user.getMobile());
                    dto.setStatus(user.getStatus());
                    dto.setDeptId(user.getDeptId());
                    dto.setAvatar(user.getAvatar());
                    dto.setPostIds(user.getPostIds());
                }

                return success(dto);
            }

            log.warn("AdminUserApiImpl.getUser: 未找到用户, id={}", id);
            return success(null);
        });
    }

    @Override
    public CommonResult<AdminUserRespDTO> getUserByMobile(String mobile) {
        return DataPermissionUtils.executeIgnore(() -> {
            AdminUserDO user = userService.getUserByMobile(mobile);
            if (user != null) {
                log.info("AdminUserApiImpl.getUserByMobile: mobile={}, username={}, nickname={}",
                        user.getMobile(), user.getUsername(), user.getNickname());

                // 测试Bean转换
                AdminUserRespDTO dto = BeanUtils.toBean(user, AdminUserRespDTO.class);
                log.info("AdminUserApiImpl.getUserByMobile: 转换后 dto.username={}, dto.nickname={}",
                        dto.getUsername(), dto.getNickname());

                // 如果转换失败，手动设置字段
                if (dto.getUsername() == null && user.getUsername() != null) {
                    log.warn("BeanUtils转换失败，手动设置字段: username={}", user.getUsername());
                    dto.setUsername(user.getUsername());
                    dto.setNickname(user.getNickname());
                    dto.setId(user.getId());
                    dto.setMobile(user.getMobile());
                    dto.setStatus(user.getStatus());
                    dto.setDeptId(user.getDeptId());
                    dto.setAvatar(user.getAvatar());
                    dto.setPostIds(user.getPostIds());
                }

                return success(dto);
            }

            log.warn("AdminUserApiImpl.getUserByMobile: 未找到用户, mobile={}", mobile);
            return success(null);
        });
    }

    @Override
    public CommonResult<List<AdminUserRespDTO>> getUserListBySubordinate(Long id) {
        // 1.1 获取用户负责的部门
        AdminUserDO user = userService.getUser(id);
        if (user == null) {
            return success(Collections.emptyList());
        }
        ArrayList<Long> deptIds = new ArrayList<>();
        DeptDO dept = deptService.getDept(user.getDeptId());
        if (dept == null) {
            return success(Collections.emptyList());
        }
        if (ObjUtil.notEqual(dept.getLeaderUserId(), id)) { // 校验为负责人
            return success(Collections.emptyList());
        }
        deptIds.add(dept.getId());
        // 1.2 获取所有子部门
        List<DeptDO> childDeptList = deptService.getChildDeptList(dept.getId());
        if (CollUtil.isNotEmpty(childDeptList)) {
            deptIds.addAll(convertSet(childDeptList, DeptDO::getId));
        }

        // 2. 获取部门对应的用户信息
        List<AdminUserDO> users = userService.getUserListByDeptIds(deptIds);
        users.removeIf(item -> ObjUtil.equal(item.getId(), id)); // 排除自己
        return success(BeanUtils.toBean(users, AdminUserRespDTO.class));
    }

    @Override
    public CommonResult<List<AdminUserRespDTO>> getUserList(Collection<Long> ids) {
        return DataPermissionUtils.executeIgnore(() -> { // 禁用数据权限。原因是，一般基于指定 id 的 API 查询，都是数据拼接为主
            List<AdminUserDO> users = userService.getUserList(ids);
            return success(BeanUtils.toBean(users, AdminUserRespDTO.class));
        });
    }

    @Override
    public CommonResult<List<AdminUserRespDTO>> getUserListByDeptIds(Collection<Long> deptIds) {
        List<AdminUserDO> users = userService.getUserListByDeptIds(deptIds);
        return success(BeanUtils.toBean(users, AdminUserRespDTO.class));
    }

    @Override
    public CommonResult<List<AdminUserRespDTO>> getUserListByPostIds(Collection<Long> postIds) {
        List<AdminUserDO> users = userService.getUserListByPostIds(postIds);
        return success(BeanUtils.toBean(users, AdminUserRespDTO.class));
    }

    @Override
    public CommonResult<List<AdminUserRespDTO>> getUserListByRoleIds(Collection<Long> roleIds) {
        List<AdminUserDO> users = userService.getUserListByRoleIds(roleIds);
        return success(BeanUtils.toBean(users, AdminUserRespDTO.class));
    }

    @Override
    public CommonResult<List<AdminUserRespDTO>> getUserListByStatus(Integer status) {
        List<AdminUserDO> users = userService.getUserListByStatus(status);
        return success(BeanUtils.toBean(users, AdminUserRespDTO.class));
    }

    @Override
    public CommonResult<Boolean> validateUserList(Collection<Long> ids) {
        userService.validateUserList(ids);
        return success(true);
    }

    @Override
    public CommonResult<UserOnboardingRespDTO> createOnboardingUser(UserOnboardingCreateReqDTO reqDTO) {
        // 转换为VO
        UserOnboardingCreateReqVO createReqVO = new UserOnboardingCreateReqVO();
        createReqVO.setEmployeeName(reqDTO.getEmployeeName());
        createReqVO.setMobile(reqDTO.getMobile());
        createReqVO.setEmail(reqDTO.getEmail());
        createReqVO.setDeptId(reqDTO.getDeptId());
        createReqVO.setDeptName(reqDTO.getDeptName());
        createReqVO.setPosition(reqDTO.getPosition());
        createReqVO.setSex(reqDTO.getSex());
        createReqVO.setOnboardingNo(reqDTO.getOnboardingNo());

        // 调用service创建用户
        UserOnboardingRespVO respVO = userService.createOnboardingUser(createReqVO);

        // 转换为DTO
        UserOnboardingRespDTO respDTO = new UserOnboardingRespDTO();
        respDTO.setUserId(respVO.getUserId());
        respDTO.setUsername(respVO.getUsername());
        respDTO.setNickname(respVO.getNickname());
        respDTO.setMobile(respVO.getMobile());
        respDTO.setEmail(respVO.getEmail());
        respDTO.setDeptId(respVO.getDeptId());
        respDTO.setDeptName(respVO.getDeptName());
        respDTO.setPosition(respVO.getPosition());
        respDTO.setDefaultPassword(respVO.getDefaultPassword());
        respDTO.setOnboardingNo(respVO.getOnboardingNo());

        return success(respDTO);
    }

    @Override
    public CommonResult<Boolean> updateUserStatus(Long id, Integer status) {
        userService.updateUserStatus(id, status);
        return success(true);
    }

    @Override
    public CommonResult<Boolean> updateUserDept(Long id, Long deptId) {
        userService.updateUserDeptBatch(Collections.singleton(id), deptId);
        return success(true);
    }

    @Override
    public CommonResult<Boolean> updateUserMobile(Long id, String mobile) {
        userService.updateUserMobile(id, mobile);
        return success(true);
    }

    @Override
    public CommonResult<Integer> upsertUserSyncBatch(List<UserSyncUpsertReqDTO> reqDTOList) {
        if (CollUtil.isEmpty(reqDTOList)) {
            return success(0);
        }
        int affected = 0;
        for (UserSyncUpsertReqDTO reqDTO : reqDTOList) {
            if (reqDTO == null) {
                continue;
            }
            String externalUserId = StrUtil.trimToNull(reqDTO.getExternalUserId());
            if (externalUserId == null) {
                continue;
            }
            UserSyncDO entity = userSyncMapper.selectByExternalUserId(externalUserId);
            if (entity == null) {
                entity = new UserSyncDO();
                entity.setExternalUserId(externalUserId);
            }
            String username = StrUtil.trimToNull(reqDTO.getUsername());
            if (username == null) {
                username = externalUserId;
            }
            String nickname = StrUtil.trimToNull(reqDTO.getNickname());
            if (nickname == null) {
                nickname = username;
            }
            entity.setUsername(username);
            entity.setNickname(nickname);
            entity.setEmail(StrUtil.trimToNull(reqDTO.getEmail()));
            entity.setMobile(StrUtil.trimToNull(reqDTO.getMobile()));
            entity.setDeptName(StrUtil.trimToNull(reqDTO.getDeptName()));
            entity.setPostName(StrUtil.trimToNull(reqDTO.getPostName()));
            entity.setRoleName(StrUtil.trimToNull(reqDTO.getRoleName()));
            entity.setUserType(StrUtil.trimToNull(reqDTO.getUserType()));
            entity.setLinkName(StrUtil.trimToNull(reqDTO.getLinkName()));
            entity.setWorked(reqDTO.getWorked());
            entity.setStatus(reqDTO.getStatus() == null ? 0 : reqDTO.getStatus());
            entity.setSyncStatus(reqDTO.getSyncStatus() == null
                    ? UserSyncDO.SyncStatus.PENDING.getCode()
                    : reqDTO.getSyncStatus());
            entity.setSyncError(StrUtil.trimToNull(reqDTO.getSyncError()));
            entity.setExternalData(StrUtil.trimToNull(reqDTO.getExternalData()));
            if (entity.getSyncStatus() != null
                    && !entity.getSyncStatus().equals(UserSyncDO.SyncStatus.PENDING.getCode())) {
                entity.setSyncTime(LocalDateTime.now());
            }
            if (entity.getId() == null) {
                userSyncMapper.insert(entity);
            } else {
                userSyncMapper.updateById(entity);
            }
            affected++;
        }
        return success(affected);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommonResult<List<UserFunctionSyncRespDTO>> syncUserFunctionsBatch(List<UserFunctionSyncReqDTO> reqDTOList) {
        if (CollUtil.isEmpty(reqDTOList)) {
            return success(Collections.emptyList());
        }
        List<UserFunctionSyncRespDTO> result = new ArrayList<>();
        for (UserFunctionSyncReqDTO reqDTO : reqDTOList) {
            if (reqDTO == null || reqDTO.getUserId() == null) {
                continue;
            }
            result.add(syncUserFunction(reqDTO));
        }
        return success(result);
    }

    private UserFunctionSyncRespDTO syncUserFunction(UserFunctionSyncReqDTO reqDTO) {
        UserFunctionSyncRespDTO respDTO = new UserFunctionSyncRespDTO();
        respDTO.setUserId(reqDTO.getUserId());
        respDTO.setCreatedRoleCount(0);
        respDTO.setAssignedRoleCount(0);
        respDTO.setRemovedRoleCount(0);
        respDTO.setPostCreated(false);

        AdminUserDO user = userService.getUser(reqDTO.getUserId());
        if (user == null) {
            return respDTO;
        }
        Long tenantId = resolveTenantId(user);
        if (tenantId != null) {
            return TenantUtils.execute(tenantId, () -> syncUserFunctionInCurrentTenant(reqDTO, respDTO, tenantId));
        }
        return syncUserFunctionInCurrentTenant(reqDTO, respDTO, tenantId);
    }

    private UserFunctionSyncRespDTO syncUserFunctionInCurrentTenant(UserFunctionSyncReqDTO reqDTO,
                                                                    UserFunctionSyncRespDTO respDTO,
                                                                    Long tenantId) {
        String postName = limitLength(StrUtil.trimToNull(reqDTO.getPostName()), 50);
        if (postName != null) {
            EnsurePostResult postResult = ensurePost(postName);
            respDTO.setPostId(postResult.postId);
            respDTO.setPostCreated(postResult.created);
            syncUserPost(reqDTO.getUserId(), tenantId, postResult.postId);
        }

        SyncRoleResult roleResult = syncUserRoles(reqDTO);
        respDTO.setRoleIds(roleResult.finalRoleIds);
        respDTO.setCreatedRoleCount(roleResult.createdRoleCount);
        respDTO.setAssignedRoleCount(roleResult.assignedRoleCount);
        respDTO.setRemovedRoleCount(roleResult.removedRoleCount);
        return respDTO;
    }

    private Long resolveTenantId(AdminUserDO user) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return tenantId;
        }
        return user == null ? null : user.getTenantId();
    }

    private EnsurePostResult ensurePost(String postName) {
        PostDO post = selectFirstPostByName(postName);
        if (post != null) {
            if (!CommonStatusEnum.ENABLE.getStatus().equals(post.getStatus())) {
                PostDO update = new PostDO();
                update.setId(post.getId());
                update.setStatus(CommonStatusEnum.ENABLE.getStatus());
                postMapper.updateById(update);
            }
            return new EnsurePostResult(post.getId(), false);
        }

        post = new PostDO();
        post.setName(postName);
        post.setCode(uniquePostCode(postName));
        post.setSort(nextPostSort());
        post.setStatus(CommonStatusEnum.ENABLE.getStatus());
        post.setRemark("钉钉同步自动创建");
        postMapper.insert(post);
        return new EnsurePostResult(post.getId(), true);
    }

    private void syncUserPost(Long userId, Long tenantId, Long postId) {
        if (userId == null || postId == null) {
            return;
        }
        Set<Long> postIds = Collections.singleton(postId);
        AdminUserDO updateUser = new AdminUserDO();
        updateUser.setId(userId);
        updateUser.setPostIds(postIds);
        userMapper.updateById(updateUser);

        if (tenantId != null) {
            UserTenantRelationDO relation = userTenantRelationMapper.selectByUserIdAndTenantId(userId, tenantId);
            if (relation != null) {
                UserTenantRelationDO updateRelation = new UserTenantRelationDO();
                updateRelation.setId(relation.getId());
                updateRelation.setPostIds(postIds);
                userTenantRelationMapper.updateById(updateRelation);
            }
        }

        userPostMapper.deleteByUserId(userId);
        UserPostDO userPost = new UserPostDO();
        userPost.setUserId(userId);
        userPost.setPostId(postId);
        userPostMapper.insert(userPost);
    }

    private SyncRoleResult syncUserRoles(UserFunctionSyncReqDTO reqDTO) {
        SyncRoleResult result = new SyncRoleResult();
        Set<Long> targetRoleIds = new LinkedHashSet<>();
        int createdRoleCount = 0;
        if (CollUtil.isNotEmpty(reqDTO.getRoles())) {
            for (UserFunctionSyncReqDTO.RoleItem roleItem : reqDTO.getRoles()) {
                EnsureRoleResult roleResult = ensureRole(roleItem);
                if (roleResult.roleId != null) {
                    targetRoleIds.add(roleResult.roleId);
                    if (roleResult.created) {
                        createdRoleCount++;
                    }
                }
            }
        }

        Set<Long> currentRoleIds = convertSet(userRoleMapper.selectListByUserId(reqDTO.getUserId()), UserRoleDO::getRoleId);
        Set<Long> managedRoleIds = resolveManagedRoleIds(reqDTO);
        Set<Long> finalRoleIds = new LinkedHashSet<>();
        for (Long roleId : currentRoleIds) {
            if (!managedRoleIds.contains(roleId)) {
                finalRoleIds.add(roleId);
            }
        }
        finalRoleIds.addAll(targetRoleIds);

        if (!Objects.equals(currentRoleIds, finalRoleIds)) {
            permissionService.assignUserRole(reqDTO.getUserId(), finalRoleIds);
        }

        Set<Long> added = new HashSet<>(finalRoleIds);
        added.removeAll(currentRoleIds);
        Set<Long> removed = new HashSet<>(currentRoleIds);
        removed.removeAll(finalRoleIds);

        result.finalRoleIds = finalRoleIds;
        result.createdRoleCount = createdRoleCount;
        result.assignedRoleCount = added.size();
        result.removedRoleCount = removed.size();
        return result;
    }

    private EnsureRoleResult ensureRole(UserFunctionSyncReqDTO.RoleItem roleItem) {
        if (roleItem == null) {
            return new EnsureRoleResult(null, false);
        }
        String name = limitLength(StrUtil.trimToNull(roleItem.getName()), 30);
        String code = limitLength(StrUtil.trimToNull(roleItem.getCode()), 100);
        if (name == null && code == null) {
            return new EnsureRoleResult(null, false);
        }
        if (name == null) {
            name = code;
        }
        if (code == null) {
            RoleDO byName = selectFirstRoleByName(name);
            if (byName != null) {
                updateRoleIfNeeded(byName, null, name);
                return new EnsureRoleResult(byName.getId(), false);
            }
            if (!Boolean.TRUE.equals(roleItem.getCreateIfMissing())) {
                return new EnsureRoleResult(null, false);
            }
            code = uniqueRoleCode("dingtalk_role_", name);
        }

        RoleDO role = selectFirstRoleByCode(code);
        if (role == null) {
            role = selectFirstRoleByName(name);
        }
        if (role != null) {
            updateRoleIfNeeded(role, code, name);
            return new EnsureRoleResult(role.getId(), false);
        }
        if (!Boolean.TRUE.equals(roleItem.getCreateIfMissing())) {
            return new EnsureRoleResult(null, false);
        }

        role = new RoleDO();
        role.setName(name);
        role.setCode(code);
        role.setSort(nextRoleSort());
        role.setStatus(CommonStatusEnum.ENABLE.getStatus());
        role.setType(RoleTypeEnum.CUSTOM.getType());
        role.setDataScope(DataScopeEnum.ALL.getScope());
        role.setRemark("钉钉同步自动创建");
        roleMapper.insert(role);
        return new EnsureRoleResult(role.getId(), true);
    }

    private void updateRoleIfNeeded(RoleDO role, String code, String name) {
        if (role == null) {
            return;
        }
        RoleDO update = new RoleDO();
        update.setId(role.getId());
        boolean changed = false;
        if (!CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus())) {
            update.setStatus(CommonStatusEnum.ENABLE.getStatus());
            changed = true;
        }
        if (StrUtil.isNotBlank(name) && !Objects.equals(role.getName(), name)) {
            update.setName(name);
            changed = true;
        }
        if (StrUtil.isNotBlank(code) && !Objects.equals(role.getCode(), code)) {
            RoleDO codeOwner = selectFirstRoleByCode(code);
            if (codeOwner == null || Objects.equals(codeOwner.getId(), role.getId())) {
                update.setCode(code);
                changed = true;
            }
        }
        if (!changed) {
            return;
        }
        roleMapper.updateById(update);
    }

    private Set<Long> resolveManagedRoleIds(UserFunctionSyncReqDTO reqDTO) {
        Set<Long> result = new HashSet<>();
        Set<String> managedCodes = reqDTO.getManagedRoleCodes() == null
                ? Collections.emptySet() : reqDTO.getManagedRoleCodes();
        List<RoleDO> roles = roleMapper.selectList();
        for (RoleDO role : roles) {
            String code = role.getCode();
            if (managedCodes.contains(code)
                    || (Boolean.TRUE.equals(reqDTO.getRemoveDingTalkGeneratedRoles())
                    && code != null && code.startsWith("dingtalk_role_"))) {
                result.add(role.getId());
            }
        }
        return result;
    }

    private String uniquePostCode(String postName) {
        return uniqueCode("dingtalk_post_", postName, 64, true);
    }

    private String uniqueRoleCode(String prefix, String roleName) {
        return uniqueCode(prefix, roleName, 100, false);
    }

    private String uniqueCode(String prefix, String value, int maxLength, boolean postCode) {
        String base = prefix + crc32Hex(value);
        if (base.length() > maxLength) {
            base = base.substring(0, maxLength);
        }
        String code = base;
        int index = 1;
        while ((postCode ? selectFirstPostByCode(code) : selectFirstRoleByCode(code)) != null) {
            String suffix = "_" + index++;
            code = base.length() + suffix.length() > maxLength
                    ? base.substring(0, maxLength - suffix.length()) + suffix
                    : base + suffix;
        }
        return code;
    }

    private PostDO selectFirstPostByName(String name) {
        if (StrUtil.isBlank(name)) {
            return null;
        }
        List<PostDO> posts = postMapper.selectList(new LambdaQueryWrapperX<PostDO>()
                .eq(PostDO::getName, name)
                .orderByAsc(PostDO::getId));
        return CollUtil.isEmpty(posts) ? null : posts.get(0);
    }

    private PostDO selectFirstPostByCode(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        List<PostDO> posts = postMapper.selectList(new LambdaQueryWrapperX<PostDO>()
                .eq(PostDO::getCode, code)
                .orderByAsc(PostDO::getId));
        return CollUtil.isEmpty(posts) ? null : posts.get(0);
    }

    private RoleDO selectFirstRoleByName(String name) {
        if (StrUtil.isBlank(name)) {
            return null;
        }
        List<RoleDO> roles = roleMapper.selectList(new LambdaQueryWrapperX<RoleDO>()
                .eq(RoleDO::getName, name)
                .orderByAsc(RoleDO::getId));
        return CollUtil.isEmpty(roles) ? null : roles.get(0);
    }

    private RoleDO selectFirstRoleByCode(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        List<RoleDO> roles = roleMapper.selectList(new LambdaQueryWrapperX<RoleDO>()
                .eq(RoleDO::getCode, code)
                .orderByAsc(RoleDO::getId));
        return CollUtil.isEmpty(roles) ? null : roles.get(0);
    }

    private String crc32Hex(String value) {
        CRC32 crc32 = new CRC32();
        crc32.update((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
        return Long.toHexString(crc32.getValue());
    }

    private int nextPostSort() {
        int maxSort = 0;
        for (PostDO post : postMapper.selectList()) {
            if (post.getSort() != null && post.getSort() > maxSort) {
                maxSort = post.getSort();
            }
        }
        return maxSort + 1;
    }

    private int nextRoleSort() {
        int maxSort = 0;
        for (RoleDO role : roleMapper.selectList()) {
            if (role.getSort() != null && role.getSort() > maxSort) {
                maxSort = role.getSort();
            }
        }
        return maxSort + 1;
    }

    private String limitLength(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private static final class EnsurePostResult {
        private final Long postId;
        private final boolean created;

        private EnsurePostResult(Long postId, boolean created) {
            this.postId = postId;
            this.created = created;
        }
    }

    private static final class EnsureRoleResult {
        private final Long roleId;
        private final boolean created;

        private EnsureRoleResult(Long roleId, boolean created) {
            this.roleId = roleId;
            this.created = created;
        }
    }

    private static final class SyncRoleResult {
        private Set<Long> finalRoleIds = Collections.emptySet();
        private int createdRoleCount;
        private int assignedRoleCount;
        private int removedRoleCount;
    }

    @Override
    public CommonResult<Integer> markUserSyncInactive(Collection<String> externalUserIds) {
        if (CollUtil.isEmpty(externalUserIds)) {
            return success(0);
        }
        List<UserSyncDO> syncRows = userSyncMapper.selectListByExternalUserIds(externalUserIds.stream()
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .distinct()
                .collect(java.util.stream.Collectors.toList()));
        if (CollUtil.isEmpty(syncRows)) {
            return success(0);
        }

        int affected = 0;
        LocalDateTime now = LocalDateTime.now();
        for (UserSyncDO syncRow : syncRows) {
            if (syncRow == null || syncRow.getId() == null) {
                continue;
            }
            boolean changed = false;
            UserSyncDO updateObj = new UserSyncDO();
            updateObj.setId(syncRow.getId());
            if (!ObjUtil.equal(syncRow.getWorked(), 0)) {
                updateObj.setWorked(0);
                changed = true;
            }
            if (!ObjUtil.equal(syncRow.getStatus(), 1)) {
                updateObj.setStatus(1);
                changed = true;
            }
            if (!changed) {
                continue;
            }
            updateObj.setSyncTime(now);
            updateObj.setSyncError(null);
            userSyncMapper.updateById(updateObj);
            affected++;
        }
        return success(affected);
    }

    @Override
    public CommonResult<Integer> markUserSyncSynced(Collection<String> externalUserIds) {
        if (CollUtil.isEmpty(externalUserIds)) {
            return success(0);
        }
        List<UserSyncDO> syncRows = userSyncMapper.selectListByExternalUserIds(externalUserIds.stream()
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .distinct()
                .collect(java.util.stream.Collectors.toList()));
        if (CollUtil.isEmpty(syncRows)) {
            return success(0);
        }

        int affected = 0;
        LocalDateTime now = LocalDateTime.now();
        for (UserSyncDO syncRow : syncRows) {
            if (syncRow == null || syncRow.getId() == null) {
                continue;
            }
            boolean changed = false;
            UserSyncDO updateObj = new UserSyncDO();
            updateObj.setId(syncRow.getId());
            if (!ObjUtil.equal(syncRow.getSyncStatus(), UserSyncDO.SyncStatus.SUCCESS.getCode())) {
                updateObj.setSyncStatus(UserSyncDO.SyncStatus.SUCCESS.getCode());
                changed = true;
            }
            if (syncRow.getSyncError() != null) {
                updateObj.setSyncError(null);
                changed = true;
            }
            if (!changed) {
                continue;
            }
            updateObj.setSyncTime(now);
            userSyncMapper.updateById(updateObj);
            affected++;
        }
        return success(affected);
    }

}
