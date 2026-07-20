package com.kyx.service.business.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.common.biz.system.tenant.TenantFeatureCodeConstants;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.exception.ServiceException;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.foundation.common.util.collection.CollectionUtils;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.common.util.validation.ValidationUtils;
import com.kyx.foundation.datapermission.core.util.DataPermissionUtils;
import com.kyx.foundation.security.core.service.SecurityFrameworkService;
import com.kyx.foundation.tenant.core.aop.TenantIgnore;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.op.api.config.ConfigApi;
import com.kyx.service.business.controller.admin.auth.vo.AuthRegisterReqVO;
import com.kyx.service.business.controller.admin.user.vo.profile.UserProfileUpdatePasswordReqVO;
import com.kyx.service.business.controller.admin.user.vo.profile.UserProfileUpdateReqVO;
import com.kyx.service.business.controller.admin.user.vo.user.UserImportExcelVO;
import com.kyx.service.business.controller.admin.user.vo.user.UserImportRespVO;
import com.kyx.service.business.controller.admin.user.vo.user.UserOnboardingCreateReqVO;
import com.kyx.service.business.controller.admin.user.vo.user.UserOnboardingRespVO;
import com.kyx.service.business.controller.admin.user.vo.user.UserPageReqVO;
import com.kyx.service.business.controller.admin.user.vo.user.UserSaveReqVO;
import com.kyx.service.business.dal.dataobject.dept.DeptDO;
import com.kyx.service.business.dal.dataobject.dept.PostDO;
import com.kyx.service.business.dal.dataobject.dept.UserPostDO;
import com.kyx.service.business.dal.dataobject.permission.RoleDO;
import com.kyx.service.business.dal.dataobject.tenant.UserTenantRelationDO;
import com.kyx.service.business.dal.dataobject.user.AdminUserDO;
import com.kyx.service.business.dal.mysql.dept.PostMapper;
import com.kyx.service.business.dal.mysql.dept.UserPostMapper;
import com.kyx.service.business.dal.mysql.permission.RoleMapper;
import com.kyx.service.business.dal.mysql.permission.UserRoleMapper;
import com.kyx.service.business.dal.dataobject.permission.UserRoleDO;
import com.kyx.service.business.dal.mysql.user.AdminUserMapper;
import com.kyx.service.business.service.dept.DeptService;
import com.kyx.service.business.service.dept.PostService;
import com.kyx.service.business.service.permission.PermissionService;
import com.kyx.service.business.service.tenant.TenantFeatureConfigService;
import com.kyx.service.business.service.tenant.TenantService;
import com.kyx.service.business.service.tenant.UserTenantRelationService;
import com.google.common.annotations.VisibleForTesting;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.service.impl.DiffParseFunction;
import com.mzt.logapi.starter.annotation.LogRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;

import javax.annotation.Resource;
import javax.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.*;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.foundation.common.util.collection.CollectionUtils.*;
import static com.kyx.service.business.enums.ErrorCodeConstants.*;
import static com.kyx.service.business.enums.LogRecordConstants.*;

/**
 * 后台用户 Service 实现类
 *
 * @author MK
 */
@Service("adminUserService")
@Slf4j
public class AdminUserServiceImpl implements AdminUserService {

    static final String USER_INIT_PASSWORD_KEY = "system.user.init-password";

    static final String USER_REGISTER_ENABLED_KEY = "system.user.register-enabled";

    private static final String DEFAULT_MEMBER_ROLE_CODE = "biz_member";

    private static final String DEFAULT_MEMBER_ROLE_NAME = "普通员工";

    private static final String DEFAULT_MEMBER_POST_NAME = "普通员工";

    @Resource
    private AdminUserMapper userMapper;

    @Resource
    private DeptService deptService;
    @Resource
    private PostService postService;
    @Resource
    private PermissionService permissionService;
    @Resource
    @Lazy
    private SecurityFrameworkService securityFrameworkService;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    @Lazy // 延迟，避免循环依赖报错
    private TenantService tenantService;
    @Resource
    private TenantFeatureConfigService tenantFeatureConfigService;

    @Resource
    private UserPostMapper userPostMapper;

    @Resource
    private PostMapper postMapper;

    @Resource
    private UserRoleMapper userRoleMapper;

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private ConfigApi configApi;

    @Resource
    @Lazy // 延迟，避免循环依赖报错
    private UserTenantRelationService userTenantRelationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "dept:user:count", allEntries = true)
    @LogRecord(type = SYSTEM_USER_TYPE, subType = SYSTEM_USER_CREATE_SUB_TYPE, bizNo = "{{#user.id}}",
            success = SYSTEM_USER_CREATE_SUCCESS)
    public Long createUser(UserSaveReqVO createReqVO) {
        // 1.1 校验账户配合
        tenantService.handleTenantInfo(tenant -> {
            long count = userMapper.selectCount();
            if (count >= tenant.getAccountCount()) {
                throw exception(USER_COUNT_MAX, tenant.getAccountCount());
            }
        });
        // 1.2 校验正确性
        validateUserForCreateOrUpdate(null, createReqVO.getUsername(),
                createReqVO.getMobile(), createReqVO.getEmail(), createReqVO.getDeptId(), createReqVO.getPostIds());
        // 2.1 插入用户（不设置 tenantId，租户关系通过关联表管理）
        AdminUserDO user = BeanUtils.toBean(createReqVO, AdminUserDO.class);
        user.setStatus(CommonStatusEnum.ENABLE.getStatus()); // 默认开启
        user.setPassword(encodePassword(createReqVO.getPassword())); // 加密密码
        userMapper.insert(user);

        // 2.2 插入关联岗位
        if (CollectionUtil.isNotEmpty(user.getPostIds())) {
            userPostMapper.insertBatch(convertList(user.getPostIds(),
                    postId -> new UserPostDO().setUserId(user.getId()).setPostId(postId)));
        }

        // 2.3 创建用户-租户关联
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            userTenantRelationService.addUserToTenant(
                user.getId(),
                tenantId,
                null, // roleIds
                createReqVO.getDeptId(),
                createReqVO.getPostIds() != null ? new ArrayList<>(createReqVO.getPostIds()) : null,
                user.getNickname(),
                user.getAvatar(),
                true // 设为默认租户
            );
        }

        // 3. 记录操作日志上下文
        LogRecordContext.putVariable("user", user);
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "dept:user:count", allEntries = true)
    public Long registerUser(AuthRegisterReqVO registerReqVO) {
        // 1.1 校验是否开启注册
        if (ObjUtil.notEqual(configApi.getConfigValueByKey(USER_REGISTER_ENABLED_KEY).getCheckedData(), "true")) {
            throw exception(USER_REGISTER_DISABLED);
        }
        // 1.2 校验账户配合
        tenantService.handleTenantInfo(tenant -> {
            long count = userMapper.selectCount();
            if (count >= tenant.getAccountCount()) {
                throw exception(USER_COUNT_MAX, tenant.getAccountCount());
            }
        });
        // 1.3 校验正确性 - 传入手机号和邮箱进行验证
        validateUserForCreateOrUpdate(null, registerReqVO.getUsername(), registerReqVO.getMobile(), registerReqVO.getEmail(), null, null);

        // 2. 插入用户（不设置 tenantId，租户关系通过关联表管理）
        AdminUserDO user = BeanUtils.toBean(registerReqVO, AdminUserDO.class);
        user.setStatus(CommonStatusEnum.ENABLE.getStatus()); // 默认开启
        user.setPassword(encodePassword(registerReqVO.getPassword())); // 加密密码
        userMapper.insert(user);

        // 3. 创建用户-租户关联
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            userTenantRelationService.addUserToTenant(
                user.getId(),
                tenantId,
                null, // roleIds
                null, // deptId
                null, // postIds
                user.getNickname(),
                user.getAvatar(),
                true // 设为默认租户
            );
        }

        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "dept:user:count", allEntries = true)
    @LogRecord(type = SYSTEM_USER_TYPE, subType = SYSTEM_USER_UPDATE_SUB_TYPE, bizNo = "{{#updateReqVO.id}}",
            success = SYSTEM_USER_UPDATE_SUCCESS)
    public void updateUser(UserSaveReqVO updateReqVO) {
        updateReqVO.setPassword(null); // 特殊：此处不更新密码
        validateManageableUser(updateReqVO.getId());
        // 1. 校验正确性
        AdminUserDO oldUser = validateUserForCreateOrUpdate(updateReqVO.getId(), updateReqVO.getUsername(),
                updateReqVO.getMobile(), updateReqVO.getEmail(), updateReqVO.getDeptId(), updateReqVO.getPostIds());

        // 2.1 更新用户
        AdminUserDO updateObj = BeanUtils.toBean(updateReqVO, AdminUserDO.class);
        userMapper.updateById(updateObj);
        // 2.2 更新岗位
        updateUserPost(updateReqVO, updateObj);

        // 3. 记录操作日志上下文
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, BeanUtils.toBean(oldUser, UserSaveReqVO.class));
        LogRecordContext.putVariable("user", oldUser);
    }

    private void updateUserPost(UserSaveReqVO reqVO, AdminUserDO updateObj) {
        Long userId = reqVO.getId();
        Set<Long> dbPostIds = convertSet(userPostMapper.selectListByUserId(userId), UserPostDO::getPostId);
        // 计算新增和删除的岗位编号
        Set<Long> postIds = CollUtil.emptyIfNull(updateObj.getPostIds());
        Collection<Long> createPostIds = CollUtil.subtract(postIds, dbPostIds);
        Collection<Long> deletePostIds = CollUtil.subtract(dbPostIds, postIds);
        // 执行新增和删除。对于已经授权的岗位，不用做任何处理
        if (!CollectionUtil.isEmpty(createPostIds)) {
            userPostMapper.insertBatch(convertList(createPostIds,
                    postId -> new UserPostDO().setUserId(userId).setPostId(postId)));
        }
        if (!CollectionUtil.isEmpty(deletePostIds)) {
            userPostMapper.deleteByUserIdAndPostId(userId, deletePostIds);
        }
    }

    @Override
    @TenantIgnore // 用户表不按租户过滤，租户关系通过关联表查询
    public void updateUserLogin(Long id, String loginIp) {
        userMapper.updateById(new AdminUserDO().setId(id).setLoginIp(loginIp).setLoginDate(LocalDateTime.now()));
    }

    @Override
    public void updateUserProfile(Long id, UserProfileUpdateReqVO reqVO) {
        // 校验正确性
        validateUserExists(id);
        validateEmailUnique(id, reqVO.getEmail());
        validateMobileUnique(id, reqVO.getMobile());
        // 执行更新
        userMapper.updateById(BeanUtils.toBean(reqVO, AdminUserDO.class).setId(id));
    }

    @Override
    public void updateUserPassword(Long id, UserProfileUpdatePasswordReqVO reqVO) {
        // 校验旧密码密码
        validateOldPassword(id, reqVO.getOldPassword());
        // 执行更新
        AdminUserDO updateObj = new AdminUserDO().setId(id);
        updateObj.setPassword(encodePassword(reqVO.getNewPassword())); // 加密密码
        userMapper.updateById(updateObj);
    }

    @Override
    @LogRecord(type = SYSTEM_USER_TYPE, subType = SYSTEM_USER_UPDATE_PASSWORD_SUB_TYPE, bizNo = "{{#id}}",
            success = SYSTEM_USER_UPDATE_PASSWORD_SUCCESS)
    public void updateUserPassword(Long id, String password) {
        // 1. 校验用户存在
        AdminUserDO user = validateManageableUserExists(id);

        // 2. 更新密码
        AdminUserDO updateObj = new AdminUserDO();
        updateObj.setId(id);
        updateObj.setPassword(encodePassword(password)); // 加密密码
        userMapper.updateById(updateObj);

        // 3. 记录操作日志上下文
        LogRecordContext.putVariable("user", user);
        LogRecordContext.putVariable("newPassword", updateObj.getPassword());
    }

    @Override
    public void updateUserStatus(Long id, Integer status) {
        // 校验用户存在
        validateManageableUser(id);
        // 更新状态
        AdminUserDO updateObj = new AdminUserDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        userMapper.updateById(updateObj);
    }

    @Override
    public void updateUserMobile(Long id, String mobile) {
        String normalizedMobile = StrUtil.trimToNull(mobile);
        if (normalizedMobile == null) {
            return;
        }
        validateManageableUser(id);
        validateMobileUnique(id, normalizedMobile);
        userMapper.updateById(new AdminUserDO().setId(id).setMobile(normalizedMobile));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "dept:user:count", allEntries = true)
    @LogRecord(type = SYSTEM_USER_TYPE, subType = SYSTEM_USER_DELETE_SUB_TYPE, bizNo = "{{#id}}",
            success = SYSTEM_USER_DELETE_SUCCESS)
    public void deleteUser(Long id) {
        // 1. 校验用户存在
        AdminUserDO user = validateManageableUserExists(id);

        // 2.1 删除用户
        userMapper.deleteById(id);
        // 2.2 删除用户关联数据
        permissionService.processUserDeleted(id);
        // 2.2 删除用户岗位
        userPostMapper.deleteByUserId(id);

        // 3. 记录操作日志上下文
        LogRecordContext.putVariable("user", user);
    }

    @Override
    @TenantIgnore // 用户表不按租户过滤，租户关系通过关联表查询
    public AdminUserDO getUserByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    @Override
    @TenantIgnore // 用户表不按租户过滤，租户关系通过关联表查询
    public AdminUserDO getUserByMobile(String mobile) {
        return userMapper.selectByMobile(mobile);
    }

    @Override
    @TenantIgnore // 用户表不按租户过滤，租户关系通过关联表查询
    public AdminUserDO getUserByEmail(String email) {
        return userMapper.selectByEmail(email);
    }

    @Override
    public PageResult<AdminUserDO> getUserPage(UserPageReqVO reqVO) {
        // 1. 按当前请求租户过滤用户关联，避免跨租户查询
        Long tenantId = TenantContextHolder.getTenantId();
        Set<Long> tenantUserIds = null;
        if (tenantId != null) {
            List<UserTenantRelationDO> relations = userTenantRelationService.getUserTenantRelationsByTenantId(tenantId);
            tenantUserIds = CollectionUtils.convertSet(relations, UserTenantRelationDO::getUserId);
            if (CollUtil.isEmpty(tenantUserIds)) {
                return PageResult.empty();
            }
        }

        // 2. 如果有角色编号，查询角色对应的用户编号
        Set<Long> roleUserIds = reqVO.getRoleId() != null
                ? permissionService.getUserRoleIdListByRoleId(singleton(reqVO.getRoleId()))
                : null;

        // 3. 取交集：既要在当前租户中，又要满足角色条件
        Set<Long> finalUserIds = tenantUserIds;
        if (roleUserIds != null) {
            if (finalUserIds != null) {
                finalUserIds = new LinkedHashSet<>(finalUserIds);
                finalUserIds.retainAll(roleUserIds);
            } else {
                finalUserIds = roleUserIds;
            }
        }

        Set<Long> requestedUserIds = reqVO.getUserIds();
        if (CollUtil.isNotEmpty(requestedUserIds)) {
            requestedUserIds = new LinkedHashSet<>(requestedUserIds);
            requestedUserIds.removeIf(Objects::isNull);
            if (CollUtil.isEmpty(requestedUserIds)) {
                return PageResult.empty();
            }
            if (finalUserIds != null) {
                finalUserIds = new LinkedHashSet<>(finalUserIds);
                finalUserIds.retainAll(requestedUserIds);
            } else {
                finalUserIds = requestedUserIds;
            }
        }

        if (finalUserIds != null && finalUserIds.isEmpty()) {
            return PageResult.empty();
        }

        Set<Long> queryUserIds = finalUserIds;

        // 4. 查询用户表时临时忽略租户字段，仅依赖上面的租户关联结果做范围控制
        return TenantUtils.executeIgnore(
                () -> userMapper.selectPage(reqVO, getDeptCondition(reqVO.getDeptId()), queryUserIds)
        );
    }

    @Override
    @TenantIgnore // 用户表不按租户过滤，租户关系通过关联表查询
    public AdminUserDO getUser(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public AdminUserDO getManageableUser(Long id) {
        return validateManageableUserExists(id);
    }

    @Override
    public void validateManageableUser(Long id) {
        validateManageableUserExists(id);
    }

    @Override
    @TenantIgnore
    public AdminUserDO getUserIgnoreTenant(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public List<AdminUserDO> getUserListByDeptIds(Collection<Long> deptIds) {
        if (CollUtil.isEmpty(deptIds)) {
            return Collections.emptyList();
        }
        return userMapper.selectListByDeptIds(deptIds);
    }

    @Override
    public List<AdminUserDO> getUserListByPostIds(Collection<Long> postIds) {
        if (CollUtil.isEmpty(postIds)) {
            return Collections.emptyList();
        }
        Set<Long> userIds = convertSet(userPostMapper.selectListByPostIds(postIds), UserPostDO::getUserId);
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyList();
        }
        return userMapper.selectBatchIds(userIds);
    }

    @Override
    public List<AdminUserDO> getUserListByRoleIds(Collection<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        Set<Long> userIds = convertSet(userRoleMapper.selectListByRoleIds(roleIds), UserRoleDO::getUserId);
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyList();
        }
        return userMapper.selectBatchIds(userIds);
    }

    @Override
    @TenantIgnore // 用户表不按租户过滤，租户关系通过关联表查询
    public List<AdminUserDO> getUserList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return userMapper.selectBatchIds(ids);
    }

    @Override
    public void validateUserList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        // 获得岗位信息
        List<AdminUserDO> users = userMapper.selectBatchIds(ids);
        Map<Long, AdminUserDO> userMap = CollectionUtils.convertMap(users, AdminUserDO::getId);
        // 校验
        ids.forEach(id -> {
            AdminUserDO user = userMap.get(id);
            if (user == null) {
                throw exception(USER_NOT_EXISTS);
            }
            if (!CommonStatusEnum.ENABLE.getStatus().equals(user.getStatus())) {
                throw exception(USER_IS_DISABLE, user.getNickname());
            }
        });
    }

    @Override
    public List<AdminUserDO> getUserListByNickname(String nickname) {
        return userMapper.selectListByNickname(nickname);
    }

    /**
     * 获得部门条件：查询指定部门的子部门编号们，包括自身
     *
     * @param deptId 部门编号
     * @return 部门编号集合
     */
    private Set<Long> getDeptCondition(Long deptId) {
        if (deptId == null) {
            return Collections.emptySet();
        }
        Set<Long> deptIds = convertSet(deptService.getChildDeptList(deptId), DeptDO::getId);
        deptIds.add(deptId); // 包括自身
        return deptIds;
    }

    private AdminUserDO validateManageableUserExists(Long id) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null && !tenantService.checkUserTenantAccess(id, tenantId)) {
            throw exception(USER_NOT_EXISTS);
        }
        return validateUserExists(id);
    }

    private AdminUserDO validateUserForCreateOrUpdate(Long id, String username, String mobile, String email,
                                               Long deptId, Set<Long> postIds) {
        // 关闭数据权限，避免因为没有数据权限，查询不到数据，进而导致唯一校验不正确
        return DataPermissionUtils.executeIgnore(() -> {
            // 校验用户存在
            AdminUserDO user = validateUserExists(id);
            // 校验用户名唯一
            validateUsernameUnique(id, username);
            // 校验手机号唯一
            validateMobileUnique(id, mobile);
            // 校验邮箱唯一
            validateEmailUnique(id, email);
            // 校验部门处于开启状态（只有当deptId不为null时才校验）
            if (deptId != null) {
                deptService.validateDeptList(CollectionUtils.singleton(deptId));
            }
            // 校验岗位处于开启状态
            postService.validatePostList(postIds);
            return user;
        });
    }

    @VisibleForTesting
    @TenantIgnore // 用户表不按租户过滤，租户关系通过关联表查询
    AdminUserDO validateUserExists(Long id) {
        if (id == null) {
            return null;
        }
        AdminUserDO user = userMapper.selectById(id);
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }
        return user;
    }

    @VisibleForTesting
    @TenantIgnore // 用户表不按租户过滤，租户关系通过关联表查询
    void validateUsernameUnique(Long id, String username) {
        if (StrUtil.isBlank(username)) {
            return;
        }
        AdminUserDO user = userMapper.selectByUsername(username);
        if (user == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的用户
        if (id == null) {
            throw exception(USER_USERNAME_EXISTS);
        }
        if (!user.getId().equals(id)) {
            throw exception(USER_USERNAME_EXISTS);
        }
    }

    @VisibleForTesting
    @TenantIgnore // 用户表不按租户过滤，租户关系通过关联表查询
    void validateEmailUnique(Long id, String email) {
        if (StrUtil.isBlank(email)) {
            return;
        }
        AdminUserDO user = userMapper.selectByEmail(email);
        if (user == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的用户
        if (id == null) {
            throw exception(USER_EMAIL_EXISTS);
        }
        if (!user.getId().equals(id)) {
            throw exception(USER_EMAIL_EXISTS);
        }
    }

    @VisibleForTesting
    @TenantIgnore // 用户表不按租户过滤，租户关系通过关联表查询
    void validateMobileUnique(Long id, String mobile) {
        if (StrUtil.isBlank(mobile)) {
            return;
        }
        AdminUserDO user = userMapper.selectByMobile(mobile);
        if (user == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的用户
        if (id == null) {
            throw exception(USER_MOBILE_EXISTS);
        }
        if (!user.getId().equals(id)) {
            throw exception(USER_MOBILE_EXISTS);
        }
    }

    /**
     * 校验旧密码
     * @param id          用户 id
     * @param oldPassword 旧密码
     */
    @VisibleForTesting
    @TenantIgnore // 用户表不按租户过滤，租户关系通过关联表查询
    void validateOldPassword(Long id, String oldPassword) {
        AdminUserDO user = userMapper.selectById(id);
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }
        if (!isPasswordMatch(oldPassword, user.getPassword())) {
            throw exception(USER_PASSWORD_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 添加事务，异常则回滚所有导入
    public UserImportRespVO importUserList(List<UserImportExcelVO> importUsers, boolean isUpdateSupport) {
        // 1.1 参数校验
        if (CollUtil.isEmpty(importUsers)) {
            throw exception(USER_IMPORT_LIST_IS_EMPTY);
        }
        // 1.2 初始化密码不能为空
        String initPassword = configApi.getConfigValueByKey(USER_INIT_PASSWORD_KEY).getCheckedData();
        if (StrUtil.isEmpty(initPassword)) {
            throw exception(USER_IMPORT_INIT_PASSWORD);
        }

        // 2. 遍历，逐个创建 or 更新
        UserImportRespVO respVO = UserImportRespVO.builder().createUsernames(new ArrayList<>())
                .updateUsernames(new ArrayList<>()).failureUsernames(new LinkedHashMap<>()).build();
        importUsers.forEach(importUser -> {
            // 2.1.1 校验字段是否符合要求
            try {
                ValidationUtils.validate(BeanUtils.toBean(importUser, UserSaveReqVO.class).setPassword(initPassword));
            } catch (ConstraintViolationException ex){
                respVO.getFailureUsernames().put(importUser.getUsername(), ex.getMessage());
                return;
            }
            // 2.1.2 校验，判断是否有不符合的原因
            try {
                validateUserForCreateOrUpdate(null, null, importUser.getMobile(), importUser.getEmail(),
                        importUser.getDeptId(), null);
            } catch (ServiceException ex) {
                respVO.getFailureUsernames().put(importUser.getUsername(), ex.getMessage());
                return;
            }

            // 2.2.1 判断如果不存在，在进行插入
            AdminUserDO existUser = userMapper.selectByUsername(importUser.getUsername());
            if (existUser == null) {
                AdminUserDO newUser = BeanUtils.toBean(importUser, AdminUserDO.class)
                        .setPassword(encodePassword(initPassword)).setPostIds(new HashSet<>()); // 设置默认密码及空岗位编号数组
                userMapper.insert(newUser);

                // 创建用户-租户关联
                Long tenantId = TenantContextHolder.getTenantId();
                if (tenantId != null) {
                    userTenantRelationService.addUserToTenant(
                        newUser.getId(),
                        tenantId,
                        null, // roleIds
                        importUser.getDeptId(),
                        null, // postIds
                        newUser.getNickname(),
                        newUser.getAvatar(),
                        true // 设为默认租户
                    );
                }

                respVO.getCreateUsernames().add(importUser.getUsername());
                return;
            }
            // 2.2.2 如果存在，判断是否允许更新
            if (!isUpdateSupport) {
                respVO.getFailureUsernames().put(importUser.getUsername(), USER_USERNAME_EXISTS.getMsg());
                return;
            }
            AdminUserDO updateUser = BeanUtils.toBean(importUser, AdminUserDO.class);
            updateUser.setId(existUser.getId());
            userMapper.updateById(updateUser);
            respVO.getUpdateUsernames().add(importUser.getUsername());
        });
        return respVO;
    }

    @Override
    public List<AdminUserDO> getUserListByStatus(Integer status) {
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId == null) {
            return listUsersByIds(null, status);
        }
        return listUsersByTenantIds(Collections.singletonList(currentTenantId), status);
    }

    @Override
    @TenantIgnore
    public List<AdminUserDO> getUserListByTenants(String tenantIds) {
        List<Long> targetTenantIds = resolveTargetTenantIds(tenantIds, null);
        if (CollUtil.isEmpty(targetTenantIds)) {
            return Collections.emptyList();
        }
        return listUsersByTenantIds(targetTenantIds, CommonStatusEnum.ENABLE.getStatus());
    }

    @Override
    @TenantIgnore
    public List<AdminUserDO> getUserListByTenants(String tenantIds, Long modelId) {
        return getUserListByTenants(tenantIds, modelId, null);
    }

    @Override
    @TenantIgnore
    public List<AdminUserDO> getUserListByTenants(String tenantIds, Long modelId, String featureCode) {
        if (StrUtil.isNotBlank(tenantIds)) {
            List<Long> targetTenantIds = resolveTargetTenantIds(tenantIds, featureCode);
            if (CollUtil.isEmpty(targetTenantIds)) {
                return Collections.emptyList();
            }
            return listUsersByTenantIds(targetTenantIds, CommonStatusEnum.ENABLE.getStatus());
        }
        if (modelId != null) {
            return getUserListByTenants(null);
        }
        return getUserListByTenants(null);
    }

    private List<Long> resolveTargetTenantIds(String tenantIds, String featureCode) {
        List<Long> requestedTenantIds = parseTenantIds(tenantIds);
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId == null) {
            return CollUtil.isEmpty(requestedTenantIds) ? Collections.emptyList() : requestedTenantIds;
        }
        if (CollUtil.isEmpty(requestedTenantIds)) {
            return Collections.singletonList(currentTenantId);
        }
        List<Long> allowedTenantIds = getAccessibleTenantIdsForFeature(currentTenantId, featureCode);
        if (CollUtil.isEmpty(allowedTenantIds)) {
            return Collections.singletonList(currentTenantId);
        }
        List<Long> result = new ArrayList<>();
        for (Long tenantId : requestedTenantIds) {
            if (allowedTenantIds.contains(tenantId)) {
                result.add(tenantId);
            }
        }
        return result;
    }

    private List<Long> getAccessibleTenantIdsForFeature(Long currentTenantId, String featureCode) {
        if (currentTenantId == null) {
            return Collections.emptyList();
        }
        if (!isFeatureCrossTenantQueryEnabled(currentTenantId, featureCode)) {
            return tenantService.getAllowedTenantIds(currentTenantId);
        }
        List<Long> collaborationTenantIds = tenantService.getCollaborationTenantIds(currentTenantId);
        return CollUtil.isEmpty(collaborationTenantIds)
                ? Collections.singletonList(currentTenantId)
                : collaborationTenantIds;
    }

    private boolean isFeatureCrossTenantQueryEnabled(Long currentTenantId, String featureCode) {
        if (currentTenantId == null || StrUtil.isBlank(featureCode)) {
            return false;
        }
        String permission = resolveCrossTenantPermission(featureCode);
        if (StrUtil.isBlank(permission) || !securityFrameworkService.hasPermission(permission)) {
            return false;
        }
        return tenantFeatureConfigService.isCrossTenantEnabled(currentTenantId, featureCode.trim());
    }

    private String resolveCrossTenantPermission(String featureCode) {
        if (TenantFeatureCodeConstants.HR_EXAM_PUBLISH.equals(featureCode)) {
            return "hr:exam:publish:cross-tenant";
        }
        if (TenantFeatureCodeConstants.WORK_REQUIREMENT.equals(featureCode)) {
            return "work:requirement:cross-tenant";
        }
        return null;
    }

    private List<Long> parseTenantIds(String tenantIds) {
        if (StrUtil.isBlank(tenantIds)) {
            return Collections.emptyList();
        }
        long[] tenantIdArray = StrUtil.splitToLong(tenantIds, ",");
        if (tenantIdArray == null || tenantIdArray.length == 0) {
            return Collections.emptyList();
        }
        List<Long> tenantIdList = new ArrayList<>(tenantIdArray.length);
        for (long tenantId : tenantIdArray) {
            tenantIdList.add(tenantId);
        }
        return tenantIdList;
    }

    private List<AdminUserDO> listUsersByTenantIds(Collection<Long> tenantIds, Integer status) {
        if (CollUtil.isEmpty(tenantIds)) {
            return Collections.emptyList();
        }
        Set<Long> userIds = new LinkedHashSet<>();
        for (Long tenantId : tenantIds) {
            List<UserTenantRelationDO> relations = userTenantRelationService.getUserTenantRelationsByTenantId(tenantId);
            for (UserTenantRelationDO relation : relations) {
                if (relation != null && Integer.valueOf(1).equals(relation.getStatus())) {
                    userIds.add(relation.getUserId());
                }
            }
        }
        return listUsersByIds(userIds, status);
    }

    private List<AdminUserDO> listUsersByIds(Collection<Long> userIds, Integer status) {
        if (userIds != null && userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return TenantUtils.executeIgnore(() -> DataPermissionUtils.executeIgnore(() ->
                userMapper.selectList(new LambdaQueryWrapperX<AdminUserDO>()
                        .inIfPresent(AdminUserDO::getId, userIds)
                        .eqIfPresent(AdminUserDO::getStatus, status)
                        .orderByDesc(AdminUserDO::getId))));
    }

    @Override
    public boolean isPasswordMatch(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 对密码进行加密
     *
     * @param password 密码
     * @return 加密后的密码
     */
    private String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    @Override
    @CacheEvict(cacheNames = "dept:user:count", allEntries = true)
    public void updateUserDeptBatch(Collection<Long> userIds, Long newDeptId) {
        if (CollUtil.isEmpty(userIds)) {
            return;
        }
        // 批量更新用户的部门ID
        userMapper.updateBatchDeptId(userIds, newDeptId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserOnboardingRespVO createOnboardingUser(UserOnboardingCreateReqVO createReqVO) {
        // 生成唯一的用户名（使用手机号后6位 + 随机数）
        String username = generateUniqueOnboardingUsername(createReqVO.getMobile());
        
        // 创建用户
        AdminUserDO user = new AdminUserDO();
        user.setUsername(username);
        user.setNickname(createReqVO.getEmployeeName());
        user.setMobile(createReqVO.getMobile());
        user.setEmail(createReqVO.getEmail());
        user.setDeptId(createReqVO.getDeptId());
        user.setSex(createReqVO.getSex());
        user.setStatus(CommonStatusEnum.ENABLE.getStatus());
        user.setPassword(encodePassword("kyx123456")); // 加密默认密码
        user.setRemark("入职用户账号 - " + createReqVO.getOnboardingNo());
        Set<Long> postIds = resolveOnboardingPostIds(createReqVO.getPosition());
        user.setPostIds(postIds);
        
        userMapper.insert(user);

        // 为新用户分配当前租户的普通员工角色
        Set<Long> roleIds = resolveDefaultMemberRoleIds();
        permissionService.assignUserRole(user.getId(), roleIds);

        if (CollectionUtil.isNotEmpty(postIds)) {
            userPostMapper.insertBatch(convertList(postIds,
                    postId -> new UserPostDO().setUserId(user.getId()).setPostId(postId)));
        }

        // 创建用户-租户关联
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            userTenantRelationService.addUserToTenant(
                user.getId(),
                tenantId,
                new ArrayList<>(roleIds), // 使用上面分配的角色
                createReqVO.getDeptId(),
                new ArrayList<>(postIds), // 使用上面分配的岗位
                user.getNickname(),
                user.getAvatar(),
                true // 设为默认租户
            );
        }
        
        // 构建响应
        UserOnboardingRespVO respVO = new UserOnboardingRespVO();
        respVO.setUserId(user.getId());
        respVO.setUsername(user.getUsername());
        respVO.setNickname(user.getNickname());
        respVO.setMobile(user.getMobile());
        respVO.setEmail(user.getEmail());
        respVO.setDeptId(user.getDeptId());
        respVO.setDeptName(createReqVO.getDeptName());
        respVO.setPosition(createReqVO.getPosition());
        respVO.setDefaultPassword("kyx123456"); // 返回明文密码给前端显示
        respVO.setOnboardingNo(createReqVO.getOnboardingNo());
        
        return respVO;
    }

    private Set<Long> resolveDefaultMemberRoleIds() {
        RoleDO role = roleMapper.selectByCode(DEFAULT_MEMBER_ROLE_CODE);
        if (!isEnabled(role)) {
            role = roleMapper.selectByName(DEFAULT_MEMBER_ROLE_NAME);
        }
        if (!isEnabled(role)) {
            throw new IllegalStateException("当前租户未配置普通员工角色");
        }
        return Collections.singleton(role.getId());
    }

    private Set<Long> resolveOnboardingPostIds(String position) {
        PostDO post = null;
        String normalizedPosition = StrUtil.trimToNull(position);
        if (normalizedPosition != null) {
            post = postMapper.selectByName(normalizedPosition);
        }
        if (!isEnabled(post)) {
            post = postMapper.selectByName(DEFAULT_MEMBER_POST_NAME);
        }
        if (!isEnabled(post)) {
            log.warn("当前租户未配置普通员工岗位，入职用户将不绑定岗位");
            return Collections.emptySet();
        }
        return Collections.singleton(post.getId());
    }

    private boolean isEnabled(RoleDO role) {
        return role != null && CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus());
    }

    private boolean isEnabled(PostDO post) {
        return post != null && CommonStatusEnum.ENABLE.getStatus().equals(post.getStatus());
    }

    /**
     * 生成入职用户名
     * 登录账号要求：4-16位，不能有特殊符号
     */
    private String generateOnboardingUsername(String mobile) {
        // 取手机号后4位
        String suffix = mobile.substring(mobile.length() - 4);
        // 生成3位随机数
        String random = String.format("%03d", (int)(Math.random() * 1000));
        // 组合成用户名：on + 手机号后4位 + 3位随机数，总长度在4-16位之间
        return "User" + suffix + random;
    }

    /**
     * 生成唯一的入职用户名
     */
    private String generateUniqueOnboardingUsername(String mobile) {
        String username;
        int maxAttempts = 10; // 最大尝试次数
        int attempts = 0;
        
        do {
            username = generateOnboardingUsername(mobile);
            attempts++;
            // 如果尝试次数过多，使用时间戳后6位确保唯一性
            if (attempts > maxAttempts) {
                String timestamp = String.valueOf(System.currentTimeMillis());
                String timeSuffix = timestamp.substring(timestamp.length() - 6);
                username = "User" + mobile.substring(mobile.length() - 4) + timeSuffix;
            }
        } while (userMapper.selectByUsername(username) != null && attempts <= maxAttempts);
        
        return username;
    }

}
