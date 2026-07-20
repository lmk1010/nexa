package com.kyx.service.business.service.dept;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kyx.service.business.controller.admin.dept.vo.dept.DeptSaveReqVO;
import com.kyx.service.business.dal.dataobject.dept.DeptDO;
import com.kyx.service.business.dal.dataobject.migration.DeptSyncDO;
import com.kyx.service.business.dal.mysql.migration.DeptSyncMapper;
import com.kyx.service.business.service.migration.DeptMigrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import cn.hutool.core.util.StrUtil;

/**
 * 部门同步 Service 实现类
 *
 * @author MK
 */
@Service
@Validated
@Slf4j
public class DeptSyncServiceImpl implements DeptSyncService {

    @Resource
    private DeptSyncMapper deptSyncMapper;

    @Resource
    private DeptService deptService;

    @Resource
    private DeptMigrationService deptMigrationService;

    // 外部系统配置，从配置文件读取
    @Value("${sync.external.dept.system-name:连途}")
    private String externalSystemName;



    @Override
    @Transactional(rollbackFor = Exception.class)
    public int fetchAndSaveExternalDepts() {
        try {
            log.info("开始拉取外部部门数据...");
            
            // 使用迁移服务拉取外部数据
            DeptMigrationService.SyncResult syncResult = deptMigrationService.syncDeptsFromExternal(externalSystemName);
            
            log.info("成功拉取并保存外部部门数据，共{}条", syncResult.getSuccessCount());
            return syncResult.getSuccessCount();
            
        } catch (Exception e) {
            log.error("拉取并保存外部部门数据失败", e);
            throw new RuntimeException("拉取并保存外部部门数据失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String syncPendingDepts() {
        try {
            log.info("开始执行待同步部门数据同步...");
            
            // 获取所有待同步的记录
            List<DeptSyncDO> pendingSyncs = deptSyncMapper.selectPendingSyncList();
            
            if (pendingSyncs.isEmpty()) {
                log.info("没有待同步的部门数据");
                return "没有待同步的部门数据";
            }

            // 检查系统是否为初始化状态（无部门数据）
            com.kyx.service.business.controller.admin.dept.vo.dept.DeptListReqVO checkReqVO = 
                    new com.kyx.service.business.controller.admin.dept.vo.dept.DeptListReqVO();
            List<DeptDO> existingDepts = deptService.getDeptList(checkReqVO);
            boolean isInitialSync = existingDepts.isEmpty();
            
            if (isInitialSync) {
                log.info("检测到系统初始化状态，将进行完整的组织架构导入");
            } else {
                log.info("系统已有{}个部门，将进行增量同步", existingDepts.size());
            }

            // 1. 构建外部ID到系统内部ID的映射关系（用于记录同步过程中的映射）
            Map<String, Long> externalIdToSystemIdMap = new HashMap<>();
            
            // 如果不是初始化同步，需要先建立已有部门的外部ID映射
            if (!isInitialSync) {
                buildExistingDeptMapping(externalIdToSystemIdMap);
            }
            
            // 2. 按层级顺序排列部门（先根部门，再子部门）
            List<DeptSyncDO> sortedDepts = buildDeptHierarchyOrder(pendingSyncs);
            
            int successCount = 0;
            int failCount = 0;

            log.info("按层级顺序处理{}个部门", sortedDepts.size());

            for (DeptSyncDO deptSync : sortedDepts) {
                try {
                    // 3. 确定父部门ID
                    Long parentId = 0L; // 默认为根部门
                    if (cn.hutool.core.util.StrUtil.isNotBlank(deptSync.getExternalParentId())) {
                        // 如果有外部父部门ID，查找对应的系统内部ID
                        if (externalIdToSystemIdMap.containsKey(deptSync.getExternalParentId())) {
                            parentId = externalIdToSystemIdMap.get(deptSync.getExternalParentId());
                            log.debug("部门[{}]找到父部门映射: 外部ID[{}] -> 系统ID[{}]", 
                                    deptSync.getDeptName(), deptSync.getExternalParentId(), parentId);
                        } else {
                            log.warn("部门[{}]的父部门外部ID[{}]未找到对应的系统ID，将设为根部门", 
                                    deptSync.getDeptName(), deptSync.getExternalParentId());
                        }
                    }

                    // 4. 检查系统中是否已存在该部门（通过名称和父部门ID查询）
                    DeptDO existingDept = findExistingDeptByParentIdAndName(parentId, deptSync.getDeptName());

                    DeptSaveReqVO deptSaveReqVO = new DeptSaveReqVO();
                    deptSaveReqVO.setName(deptSync.getDeptName());
                    deptSaveReqVO.setParentId(parentId);
                    deptSaveReqVO.setSort(deptSync.getOrderNum() != null ? deptSync.getOrderNum() : 0);
                    deptSaveReqVO.setStatus(deptSync.getStatus() != null ? deptSync.getStatus() : 0);
                    deptSaveReqVO.setLeaderUserId(deptSync.getLeaderId());
                    deptSaveReqVO.setPhone(deptSync.getPhone());
                    deptSaveReqVO.setEmail(deptSync.getEmail());

                    Long localDeptId;
                    if (existingDept != null) {
                        // 更新现有部门
                        deptSaveReqVO.setId(existingDept.getId());
                        deptService.updateDept(deptSaveReqVO);
                        localDeptId = existingDept.getId();
                        log.info("更新部门成功：[{}]，系统ID：{}，父部门ID：{}", 
                                deptSync.getDeptName(), localDeptId, parentId);
                    } else {
                        // 创建新部门
                        localDeptId = deptService.createDept(deptSaveReqVO);
                        log.info("创建部门成功：[{}]，系统ID：{}，父部门ID：{}", 
                                deptSync.getDeptName(), localDeptId, parentId);
                    }

                    // 5. 记录外部ID到系统ID的映射关系
                    externalIdToSystemIdMap.put(deptSync.getExternalDeptId(), localDeptId);

                    // 6. 更新同步状态、parentId和mappedDeptId
                    deptSync.setSyncStatus(DeptSyncDO.SyncStatus.SUCCESS.getCode());
                    deptSync.setSyncTime(LocalDateTime.now());
                    deptSync.setSyncError(null);
                    deptSync.setParentId(parentId); // 重要：更新同步表中的parentId字段
                    deptSync.setMappedDeptId(localDeptId); // 重要：保存外部ID到内部ID的映射
                    deptSyncMapper.updateById(deptSync);

                    successCount++;

                } catch (Exception e) {
                    log.error("同步部门失败：[{}]，错误：{}", deptSync.getDeptName(), e.getMessage(), e);
                    
                    // 更新同步状态为失败
                    deptSync.setSyncStatus(DeptSyncDO.SyncStatus.FAILED.getCode());
                    deptSync.setSyncTime(LocalDateTime.now());
                    deptSync.setSyncError(e.getMessage());
                    deptSyncMapper.updateById(deptSync);
                    
                    failCount++;
                }
            }

            String result = String.format("部门同步完成：成功 %d 条，失败 %d 条", successCount, failCount);
            log.info(result);
            return result;

        } catch (Exception e) {
            log.error("执行待同步部门数据同步失败", e);
            throw new RuntimeException("执行待同步部门数据同步失败：" + e.getMessage());
        }
    }

    /**
     * 构建已有部门的外部ID映射关系
     * 通过查询已同步成功的记录来建立映射
     */
    private void buildExistingDeptMapping(Map<String, Long> externalIdToSystemIdMap) {
        try {
            log.info("开始构建已有部门的外部ID映射关系...");
            
            // 查询所有已同步成功的部门记录
            List<DeptSyncDO> syncedDepts = deptSyncMapper.selectListBySyncStatus(DeptSyncDO.SyncStatus.SUCCESS.getCode());
            
            for (DeptSyncDO syncedDept : syncedDepts) {
                // 根据部门名称查找对应的系统部门
                com.kyx.service.business.controller.admin.dept.vo.dept.DeptListReqVO reqVO = 
                        new com.kyx.service.business.controller.admin.dept.vo.dept.DeptListReqVO();
                reqVO.setName(syncedDept.getDeptName());
                List<DeptDO> matchedDepts = deptService.getDeptList(reqVO);
                
                if (!matchedDepts.isEmpty()) {
                    // 如果找到多个同名部门，需要进一步匹配
                    DeptDO targetDept = matchedDepts.size() == 1 ? matchedDepts.get(0) : 
                                      findBestMatchDept(matchedDepts, syncedDept);
                    
                    if (targetDept != null) {
                        externalIdToSystemIdMap.put(syncedDept.getExternalDeptId(), targetDept.getId());
                        log.debug("建立映射关系：外部ID[{}] -> 系统ID[{}]，部门名称：{}", 
                                syncedDept.getExternalDeptId(), targetDept.getId(), syncedDept.getDeptName());
                    }
                }
            }
            
            log.info("完成已有部门映射构建，共建立{}个映射关系", externalIdToSystemIdMap.size());
            
        } catch (Exception e) {
            log.error("构建已有部门映射关系失败", e);
        }
    }

    /**
     * 在多个同名部门中找到最佳匹配的部门
     */
    private DeptDO findBestMatchDept(List<DeptDO> candidates, DeptSyncDO syncDept) {
        // 简单策略：选择第一个匹配的，后续可以根据实际需求优化
        // 比如可以比较父部门名称、创建时间等
        return candidates.get(0);
    }

    /**
     * 查找已存在的部门（通过父部门ID和部门名称）
     */
    private DeptDO findExistingDeptByParentIdAndName(Long parentId, String deptName) {
        try {
            // 获取所有部门列表
            com.kyx.service.business.controller.admin.dept.vo.dept.DeptListReqVO reqVO = 
                    new com.kyx.service.business.controller.admin.dept.vo.dept.DeptListReqVO();
            List<DeptDO> allDepts = deptService.getDeptList(reqVO);
            
            // 查找匹配的部门
            return allDepts.stream()
                    .filter(dept -> dept.getName().equals(deptName) && 
                                  Objects.equals(dept.getParentId(), parentId))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.error("查找已存在部门失败：parentId={}, deptName={}", parentId, deptName, e);
            return null;
        }
    }

    /**
     * 按层级顺序排列部门（先父部门，后子部门）
     * 这个方法确保组织架构的正确导入顺序
     */
    private List<DeptSyncDO> buildDeptHierarchyOrder(List<DeptSyncDO> deptList) {
        log.info("开始构建部门层级顺序，共{}个部门", deptList.size());
        
        List<DeptSyncDO> result = new ArrayList<>();
        Set<String> processed = new HashSet<>();
        
        // 构建外部ID到部门的映射
        Map<String, DeptSyncDO> idToDeptMap = deptList.stream()
                .collect(Collectors.toMap(DeptSyncDO::getExternalDeptId, dept -> dept));
        
        // 分离根部门和子部门
        List<DeptSyncDO> rootDepts = new ArrayList<>();
        List<DeptSyncDO> childDepts = new ArrayList<>();
        
        for (DeptSyncDO dept : deptList) {
            if (StrUtil.isBlank(dept.getExternalParentId()) || 
                !idToDeptMap.containsKey(dept.getExternalParentId())) {
                // 没有父部门ID或父部门不在当前列表中，视为根部门
                rootDepts.add(dept);
            } else {
                childDepts.add(dept);
            }
        }
        
        log.info("识别到{}个根部门，{}个子部门", rootDepts.size(), childDepts.size());
        
        // 先处理所有根部门
        for (DeptSyncDO rootDept : rootDepts) {
            if (!processed.contains(rootDept.getExternalDeptId())) {
                addDeptAndChildren(rootDept, idToDeptMap, result, processed);
            }
        }
        
        // 处理剩余的部门（可能是孤儿部门或循环引用的部门）
        for (DeptSyncDO dept : childDepts) {
            if (!processed.contains(dept.getExternalDeptId())) {
                log.warn("发现孤儿部门或循环引用：{}，父部门ID：{}", dept.getDeptName(), dept.getExternalParentId());
                addDeptAndChildren(dept, idToDeptMap, result, processed);
            }
        }
        
        log.info("层级排序完成，共{}个部门", result.size());
        return result;
    }

    /**
     * 递归添加部门及其子部门
     */
    private void addDeptAndChildren(DeptSyncDO dept, Map<String, DeptSyncDO> idToDeptMap, 
                                   List<DeptSyncDO> result, Set<String> processed) {
        if (processed.contains(dept.getExternalDeptId())) {
            return;
        }
        
        // 防止循环引用
        Set<String> visitingPath = new HashSet<>();
        addDeptAndChildrenRecursive(dept, idToDeptMap, result, processed, visitingPath);
    }

    /**
     * 递归添加部门及其子部门（带循环检测）
     */
    private void addDeptAndChildrenRecursive(DeptSyncDO dept, Map<String, DeptSyncDO> idToDeptMap, 
                                           List<DeptSyncDO> result, Set<String> processed, Set<String> visitingPath) {
        if (processed.contains(dept.getExternalDeptId())) {
            return;
        }
        
        if (visitingPath.contains(dept.getExternalDeptId())) {
            log.error("检测到循环引用：部门[{}]，ID：{}", dept.getDeptName(), dept.getExternalDeptId());
            return;
        }
        
        visitingPath.add(dept.getExternalDeptId());
        
        // 先添加父部门
        if (StrUtil.isNotBlank(dept.getExternalParentId()) && 
            idToDeptMap.containsKey(dept.getExternalParentId()) &&
            !processed.contains(dept.getExternalParentId())) {
            addDeptAndChildrenRecursive(idToDeptMap.get(dept.getExternalParentId()), 
                                      idToDeptMap, result, processed, visitingPath);
        }
        
        // 再添加当前部门
        if (!processed.contains(dept.getExternalDeptId())) {
            result.add(dept);
            processed.add(dept.getExternalDeptId());
            log.debug("添加部门到同步队列：[{}]，外部ID：{}，父部门外部ID：{}", 
                     dept.getDeptName(), dept.getExternalDeptId(), dept.getExternalParentId());
        }
        
        visitingPath.remove(dept.getExternalDeptId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupSyncedRecords(int daysToKeep) {
        try {
            log.info("开始清理已同步的部门记录，保留{}天", daysToKeep);
            
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysToKeep);
            
            // 删除超过保留期限的已同步记录
            // 使用查询然后删除的方式，避免lambdaQuery方法不存在的问题
            List<DeptSyncDO> recordsToDelete = deptSyncMapper.selectListBySyncStatus(DeptSyncDO.SyncStatus.SUCCESS.getCode())
                    .stream()
                    .filter(record -> record.getSyncTime() != null && record.getSyncTime().isBefore(cutoffTime))
                    .collect(java.util.stream.Collectors.toList());
            
            int deletedCount = 0;
            for (DeptSyncDO record : recordsToDelete) {
                deptSyncMapper.deleteById(record.getId());
                deletedCount++;
            }
            
            log.info("清理已同步的部门记录完成，删除{}条记录", deletedCount);
            return deletedCount;
            
        } catch (Exception e) {
            log.error("清理已同步部门记录失败", e);
            throw new RuntimeException("清理已同步部门记录失败：" + e.getMessage());
        }
    }

}