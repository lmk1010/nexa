package com.kyx.service.business.service.dept;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.datapermission.core.annotation.DataPermission;
import com.kyx.foundation.tenant.core.aop.TenantIgnore;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.service.business.controller.admin.dept.vo.dept.DeptListReqVO;
import com.kyx.service.business.controller.admin.dept.vo.dept.DeptSaveReqVO;
import com.kyx.service.business.dal.dataobject.dept.DeptDO;
import com.kyx.service.business.dal.mysql.dept.DeptMapper;
import com.kyx.service.business.dal.redis.RedisKeyConstants;
import com.kyx.service.business.dal.dataobject.user.AdminUserDO;
import com.kyx.service.business.service.user.AdminUserService;
import com.kyx.service.business.dal.dataobject.migration.DeptSyncDO;
import com.kyx.service.business.dal.mysql.migration.DeptSyncMapper;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.foundation.common.util.collection.CollectionUtils.convertSet;
import static com.kyx.service.business.enums.ErrorCodeConstants.*;

/**
 * 部门 Service 实现类
 *
 * @author MK
 */
@Service
@Validated
@Slf4j
public class DeptServiceImpl implements DeptService {

    @Resource
    private DeptMapper deptMapper;
    
    @Resource
    @Lazy
    private AdminUserService userService;
    
    @Resource
    private DeptSyncMapper deptSyncMapper;

    @Override
    @CacheEvict(cacheNames = RedisKeyConstants.DEPT_CHILDREN_ID_LIST,
            allEntries = true) // allEntries 清空所有缓存，因为操作一个部门，涉及到多个缓存
    public Long createDept(DeptSaveReqVO createReqVO) {
        if (createReqVO.getParentId() == null) {
            createReqVO.setParentId(DeptDO.PARENT_ID_ROOT);
        }
        // 校验父部门的有效性
        validateParentDept(null, createReqVO.getParentId());
        // 校验部门名的唯一性
        validateDeptNameUnique(null, createReqVO.getParentId(), createReqVO.getName());

        // 插入部门
        DeptDO dept = BeanUtils.toBean(createReqVO, DeptDO.class);
        deptMapper.insert(dept);
        return dept.getId();
    }

    @Override
    @CacheEvict(cacheNames = RedisKeyConstants.DEPT_CHILDREN_ID_LIST,
            allEntries = true)
    public UpsertDeptResult upsertDept(Long id, String name, Long parentId, Integer sort, Integer status,
                                       Long leaderUserId) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("部门编号不能为空");
        }
        String deptName = StrUtil.trimToNull(name);
        if (deptName == null) {
            throw new IllegalArgumentException("部门名称不能为空");
        }
        // parentId/sort/status 为 null：更新时不改；创建时使用默认值
        // leaderUserId 为 null：不改主管；<=0：清空主管
        boolean clearLeader = leaderUserId != null && leaderUserId <= 0;
        Long targetLeaderUserId = clearLeader ? null : leaderUserId;
        boolean touchLeader = leaderUserId != null;

        UpsertDeptResult result = new UpsertDeptResult();
        result.setId(id);

        DeptDO existed = deptMapper.selectById(id);
        if (existed == null) {
            Long createParentId = parentId == null ? DeptDO.PARENT_ID_ROOT : parentId;
            Integer createSort = sort == null ? 0 : sort;
            Integer createStatus = status == null ? CommonStatusEnum.ENABLE.getStatus() : status;
            validateParentDept(id, createParentId);

            DeptDO insertObj = new DeptDO();
            insertObj.setId(id);
            insertObj.setName(deptName);
            insertObj.setParentId(createParentId);
            insertObj.setSort(createSort);
            insertObj.setStatus(createStatus);
            if (touchLeader && !clearLeader) {
                insertObj.setLeaderUserId(targetLeaderUserId);
            }
            deptMapper.insert(insertObj);
            result.setCreated(true);
            return result;
        }

        Long nextParentId = parentId == null ? existed.getParentId() : parentId;
        if (nextParentId == null) {
            nextParentId = DeptDO.PARENT_ID_ROOT;
        }
        validateParentDept(id, nextParentId);

        boolean changed = false;
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DeptDO> updateWrapper =
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DeptDO>()
                        .eq(DeptDO::getId, id);

        if (!Objects.equals(existed.getName(), deptName)) {
            updateWrapper.set(DeptDO::getName, deptName);
            changed = true;
        }
        if (parentId != null && !Objects.equals(existed.getParentId(), nextParentId)) {
            updateWrapper.set(DeptDO::getParentId, nextParentId);
            changed = true;
        }
        if (sort != null && !Objects.equals(existed.getSort(), sort)) {
            updateWrapper.set(DeptDO::getSort, sort);
            changed = true;
        }
        if (status != null && !Objects.equals(existed.getStatus(), status)) {
            updateWrapper.set(DeptDO::getStatus, status);
            changed = true;
        }
        if (touchLeader && !Objects.equals(existed.getLeaderUserId(), targetLeaderUserId)) {
            updateWrapper.set(DeptDO::getLeaderUserId, targetLeaderUserId);
            changed = true;
        }
        if (changed) {
            deptMapper.update(null, updateWrapper);
        }
        result.setUpdated(changed);
        return result;
    }

    @Override
    @CacheEvict(cacheNames = RedisKeyConstants.DEPT_CHILDREN_ID_LIST,
            allEntries = true) // allEntries 清空所有缓存，因为操作一个部门，涉及到多个缓存
    public void updateDept(DeptSaveReqVO updateReqVO) {
        if (updateReqVO.getParentId() == null) {
            updateReqVO.setParentId(DeptDO.PARENT_ID_ROOT);
        }
        // 校验自己存在
        validateDeptExists(updateReqVO.getId());
        // 校验父部门的有效性
        validateParentDept(updateReqVO.getId(), updateReqVO.getParentId());
        // 校验部门名的唯一性
        validateDeptNameUnique(updateReqVO.getId(), updateReqVO.getParentId(), updateReqVO.getName());

        // 更新部门
        DeptDO updateObj = BeanUtils.toBean(updateReqVO, DeptDO.class);
        deptMapper.updateById(updateObj);
    }

    @Override
    @CacheEvict(cacheNames = RedisKeyConstants.DEPT_CHILDREN_ID_LIST,
            allEntries = true) // allEntries 清空所有缓存，因为操作一个部门，涉及到多个缓存
    public void deleteDept(Long id) {
        // 校验是否存在
        validateDeptExists(id);
        
        // 获取要删除的所有部门ID（包括当前部门和所有子部门）
        Set<Long> deptIdsToDelete = new HashSet<>();
        deptIdsToDelete.add(id);
        
        // 递归获取所有子部门
        List<DeptDO> childDepts = getChildDeptList(id);
        if (!CollUtil.isEmpty(childDepts)) {
            deptIdsToDelete.addAll(convertSet(childDepts, DeptDO::getId));
        }
        
        // 检查系统账号 + HR 入职档案是否仍挂在这些部门
        List<AdminUserDO> usersInDepts = userService.getUserListByDeptIds(deptIdsToDelete);
        Long entryCount = deptMapper.selectEntryCountByDeptIds(deptIdsToDelete);
        int entryCnt = entryCount == null ? 0 : entryCount.intValue();
        if (!CollUtil.isEmpty(usersInDepts) || entryCnt > 0) {
            int userCnt = CollUtil.isEmpty(usersInDepts) ? 0 : usersInDepts.size();
            throw exception(DEPT_HAS_USERS,
                    String.format("部门下存在 %d 名系统用户、%d 条任职记录，请先处理后再删除部门",
                            userCnt, entryCnt));
        }

        // 删除所有子部门
        if (!CollUtil.isEmpty(childDepts)) {
            List<Long> childDeptIds = new ArrayList<>(convertSet(childDepts, DeptDO::getId));
            deptMapper.deleteBatchIds(childDeptIds);
        }

        // 删除当前部门
        deptMapper.deleteById(id);
    }

    @Override
    @CacheEvict(cacheNames = RedisKeyConstants.DEPT_CHILDREN_ID_LIST,
            allEntries = true) // allEntries 清空所有缓存，因为操作一个部门，涉及到多个缓存
    public void deleteDeptWithUserTransfer(Long id, Long transferDeptId) {
        // 校验是否存在
        validateDeptExists(id);

        // 获取要删除的所有部门ID（包括当前部门和所有子部门）
        Set<Long> deptIdsToDelete = new HashSet<>();
        deptIdsToDelete.add(id);

        // 递归获取所有子部门
        List<DeptDO> childDepts = getChildDeptList(id);
        if (!CollUtil.isEmpty(childDepts)) {
            deptIdsToDelete.addAll(convertSet(childDepts, DeptDO::getId));
        }

        // 检查系统账号 + HR 入职档案
        List<AdminUserDO> usersInDepts = userService.getUserListByDeptIds(deptIdsToDelete);
        Long entryCount = deptMapper.selectEntryCountByDeptIds(deptIdsToDelete);
        int entryCnt = entryCount == null ? 0 : entryCount.intValue();
        boolean hasMembers = !CollUtil.isEmpty(usersInDepts) || entryCnt > 0;
        if (hasMembers) {
            if (transferDeptId == null) {
                int userCnt = CollUtil.isEmpty(usersInDepts) ? 0 : usersInDepts.size();
                throw exception(DEPT_HAS_USERS,
                        String.format("部门下存在 %d 名系统用户、%d 条任职记录，请先处理后再删除部门",
                                userCnt, entryCnt));
            }
            // 校验转移部门是否存在
            validateDeptExists(transferDeptId);
            // 校验不能转移到要删除的部门或其子部门
            if (deptIdsToDelete.contains(transferDeptId)) {
                throw exception(DEPT_PARENT_ERROR, "不能将员工转移到要删除的部门或其子部门");
            }

            // 1) 系统用户部门
            if (!CollUtil.isEmpty(usersInDepts)) {
                List<Long> userIds = new ArrayList<>(convertSet(usersInDepts, AdminUserDO::getId));
                userService.updateUserDeptBatch(userIds, transferDeptId);
            }
            // 2) HR 任职记录部门（覆盖无系统账号的档案）
            int transferredEntries = deptMapper.updateEntryDeptByDeptIds(deptIdsToDelete, transferDeptId);

            log.info("删除部门[{}]时，将 {} 名系统用户、{} 条任职记录转移到部门[{}]",
                    id,
                    CollUtil.isEmpty(usersInDepts) ? 0 : usersInDepts.size(),
                    transferredEntries,
                    transferDeptId);
        }

        // 删除所有子部门
        if (!CollUtil.isEmpty(childDepts)) {
            List<Long> childDeptIds = new ArrayList<>(convertSet(childDepts, DeptDO::getId));
            deptMapper.deleteBatchIds(childDeptIds);
        }

        // 删除当前部门
        deptMapper.deleteById(id);
    }

    @VisibleForTesting
    void validateDeptExists(Long id) {
        if (id == null) {
            return;
        }
        DeptDO dept = deptMapper.selectById(id);
        if (dept == null) {
            throw exception(DEPT_NOT_FOUND);
        }
    }

    @VisibleForTesting
    void validateParentDept(Long id, Long parentId) {
        if (parentId == null || DeptDO.PARENT_ID_ROOT.equals(parentId)) {
            return;
        }
        // 1. 不能设置自己为父部门
        if (Objects.equals(id, parentId)) {
            throw exception(DEPT_PARENT_ERROR);
        }
        // 2. 父部门不存在
        DeptDO parentDept = deptMapper.selectById(parentId);
        if (parentDept == null) {
            throw exception(DEPT_PARENT_NOT_EXITS);
        }
        // 3. 递归校验父部门，如果父部门是自己的子部门，则报错，避免形成环路
        if (id == null) { // id 为空，说明新增，不需要考虑环路
            return;
        }
        for (int i = 0; i < Short.MAX_VALUE; i++) {
            // 3.1 校验环路
            parentId = parentDept.getParentId();
            if (Objects.equals(id, parentId)) {
                throw exception(DEPT_PARENT_IS_CHILD);
            }
            // 3.2 继续递归下一级父部门
            if (parentId == null || DeptDO.PARENT_ID_ROOT.equals(parentId)) {
                break;
            }
            parentDept = deptMapper.selectById(parentId);
            if (parentDept == null) {
                break;
            }
        }
    }

    @VisibleForTesting
    void validateDeptNameUnique(Long id, Long parentId, String name) {
        DeptDO dept = deptMapper.selectByParentIdAndName(parentId, name);
        if (dept == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的部门
        if (id == null) {
            throw exception(DEPT_NAME_DUPLICATE);
        }
        if (ObjectUtil.notEqual(dept.getId(), id)) {
            throw exception(DEPT_NAME_DUPLICATE);
        }
    }

    @Override
    public DeptDO getDept(Long id) {
        return deptMapper.selectById(id);
    }

    @Override
    public List<DeptDO> getDeptList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return deptMapper.selectBatchIds(ids);
    }

    @Override
    public List<DeptDO> getDeptList(DeptListReqVO reqVO) {
        List<DeptDO> list = deptMapper.selectList(reqVO);
        list.sort(Comparator.comparing(DeptDO::getSort));
        return list;
    }

    @Override
    @TenantIgnore
    public List<DeptDO> getDeptListByTenants(String tenantIds) {
        // 如果 tenantIds 为空，查询当前租户
        if (StrUtil.isBlank(tenantIds)) {
            Long currentTenantId = TenantContextHolder.getTenantId();
            return deptMapper.selectListByTenantIds(
                CollUtil.newArrayList(currentTenantId),
                CommonStatusEnum.ENABLE.getStatus()
            );
        }

        // 解析租户ID列表
        long[] tenantIdArray = StrUtil.splitToLong(tenantIds, ",");
        if (tenantIdArray == null || tenantIdArray.length == 0) {
            return CollUtil.newArrayList();
        }
        List<Long> tenantIdList = CollUtil.newArrayList();
        for (long tenantId : tenantIdArray) {
            tenantIdList.add(tenantId);
        }

        // 查询指定租户的部门
        return deptMapper.selectListByTenantIds(tenantIdList, CommonStatusEnum.ENABLE.getStatus());
    }

    @Override
    public List<DeptDO> getChildDeptList(Collection<Long> ids) {
        List<DeptDO> children = new LinkedList<>();
        // 遍历每一层
        Collection<Long> parentIds = ids;
        for (int i = 0; i < Short.MAX_VALUE; i++) { // 使用 Short.MAX_VALUE 避免 bug 场景下，存在死循环
            // 查询当前层，所有的子部门
            List<DeptDO> depts = deptMapper.selectListByParentId(parentIds);
            // 1. 如果没有子部门，则结束遍历
            if (CollUtil.isEmpty(depts)) {
                break;
            }
            // 2. 如果有子部门，继续遍历
            children.addAll(depts);
            parentIds = convertSet(depts, DeptDO::getId);
        }
        return children;
    }

    @Override
    public List<DeptDO> getDeptListByLeaderUserId(Long id) {
        return deptMapper.selectListByLeaderUserId(id);
    }

    @Override
    public DeptDO getDeptByName(String name) {
        if (StrUtil.isBlank(name)) {
            return null;
        }
        return deptMapper.selectByName(name);
    }

    @Override
    public List<DeptDO> getDeptListByName(String name) {
        if (StrUtil.isBlank(name)) {
            return Collections.emptyList();
        }
        return deptMapper.selectListByName(name);
    }

    @Override
    public Long getInternalDeptIdByExternalId(String externalDeptId) {
        if (StrUtil.isBlank(externalDeptId)) {
            return null;
        }
        
        // 查找已同步的部门记录
        DeptSyncDO deptSync = deptSyncMapper.selectByExternalDeptId(externalDeptId);
        if (deptSync != null && DeptSyncDO.SyncStatus.SUCCESS.getCode().equals(deptSync.getSyncStatus())) {
            // 直接返回已映射的内部部门ID
            if (deptSync.getMappedDeptId() != null) {
                log.debug("外部部门ID[{}]映射到内部部门ID[{}]，部门名称：{}", 
                        externalDeptId, deptSync.getMappedDeptId(), deptSync.getDeptName());
                return deptSync.getMappedDeptId();
            }
            
            // 兼容性处理：如果mappedDeptId为空，则根据部门名称查找（用于历史数据）
            DeptDO internalDept = getDeptByName(deptSync.getDeptName());
            if (internalDept != null) {
                log.warn("外部部门ID[{}]的mappedDeptId为空，使用部门名称[{}]查找到内部部门ID[{}]", 
                        externalDeptId, deptSync.getDeptName(), internalDept.getId());
                return internalDept.getId();
            }
        }
        
        log.warn("未找到外部部门ID[{}]对应的内部部门", externalDeptId);
        return null;
    }

    @Override
    @DataPermission(enable = false) // 禁用数据权限，避免建立不正确的缓存
    @Cacheable(cacheNames = RedisKeyConstants.DEPT_CHILDREN_ID_LIST, key = "#id")
    public Set<Long> getChildDeptIdListFromCache(Long id) {
        List<DeptDO> children = getChildDeptList(id);
        return convertSet(children, DeptDO::getId);
    }

    @Override
    public void validateDeptList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        // 获得科室信息
        Map<Long, DeptDO> deptMap = getDeptMap(ids);
        // 校验
        ids.forEach(id -> {
            DeptDO dept = deptMap.get(id);
            if (dept == null) {
                throw exception(DEPT_NOT_FOUND);
            }
            if (!CommonStatusEnum.ENABLE.getStatus().equals(dept.getStatus())) {
                throw exception(DEPT_NOT_ENABLE, dept.getName());
            }
        });
    }

    @Override
    @DataPermission(enable = false) // 禁用数据权限，避免影响用户统计
    // 缓存暂时注销 统计有问题
//    @Cacheable(cacheNames = "dept:user:count", key = "#deptIds.toString()")
    public Map<Long, Integer> getUserCountByDeptIds(Collection<Long> deptIds) {
        log.info("getUserCountByDeptIds 被调用，参数: {}", deptIds);
        
        if (CollUtil.isEmpty(deptIds)) {
            log.info("部门ID集合为空，返回空Map");
            return Collections.emptyMap();
        }
        
        // 获取所有部门及其子部门的员工数量
        Map<Long, Integer> result = new HashMap<>();
        
        for (Long deptId : deptIds) {
            // 获取当前部门及其所有子部门
            Set<Long> allDeptIds = new HashSet<>();
            allDeptIds.add(deptId);
            
            // 递归获取所有子部门
            List<DeptDO> childDepts = getChildDeptList(deptId);
            if (!CollUtil.isEmpty(childDepts)) {
                allDeptIds.addAll(convertSet(childDepts, DeptDO::getId));
            }
            
            // 统计这些部门下的员工数量
            List<AdminUserDO> users = userService.getUserListByDeptIds(allDeptIds);
            result.put(deptId, users.size());
            
            log.info("部门[{}]及其子部门[{}]共有 {} 名员工", deptId, allDeptIds, users.size());
        }
        
        log.info("getUserCountByDeptIds 返回结果: {}", result);
        return result;
    }

    @Override
    @DataPermission(enable = false)
    public Map<Long, Integer> getEmployeeCountByDeptIds(Collection<Long> deptIds) {
        if (CollUtil.isEmpty(deptIds)) {
            return Collections.emptyMap();
        }
        Set<Long> allDeptIds = new HashSet<>(deptIds);
        for (Long deptId : deptIds) {
            List<DeptDO> childDepts = getChildDeptList(deptId);
            if (!CollUtil.isEmpty(childDepts)) {
                allDeptIds.addAll(convertSet(childDepts, DeptDO::getId));
            }
        }
        Map<Long, Integer> directCountMap = new HashMap<>();
        List<com.kyx.service.business.dal.dataobject.dept.DeptEmployeeCountDO> countList =
                deptMapper.selectEmployeeCountByDeptIds(allDeptIds);
        for (com.kyx.service.business.dal.dataobject.dept.DeptEmployeeCountDO item : countList) {
            directCountMap.put(item.getDeptId(), item.getUserCount());
        }

        Map<Long, Integer> result = new HashMap<>();
        for (Long deptId : deptIds) {
            Set<Long> currentDeptIds = new HashSet<>();
            currentDeptIds.add(deptId);
            List<DeptDO> childDepts = getChildDeptList(deptId);
            if (!CollUtil.isEmpty(childDepts)) {
                currentDeptIds.addAll(convertSet(childDepts, DeptDO::getId));
            }
            int total = 0;
            for (Long id : currentDeptIds) {
                total += directCountMap.getOrDefault(id, 0);
            }
            result.put(deptId, total);
        }
        return result;
    }

}
