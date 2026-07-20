package com.kyx.service.business.service.migration;


import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.service.business.controller.admin.dept.vo.dept.DeptSaveReqVO;
import com.kyx.service.business.dal.dataobject.dept.DeptDO;
import com.kyx.service.business.dal.dataobject.migration.DeptSyncDO;
import com.kyx.service.business.dal.dataobject.migration.ExternalAuthConfigDO;
import com.kyx.service.business.dal.mysql.migration.DeptSyncMapper;
import com.kyx.service.business.dal.mysql.migration.ExternalAuthConfigMapper;
import com.kyx.service.business.service.dept.DeptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 部门数据迁移服务实现
 * 
 * @author MK
 */
@Service
@Slf4j
public class DeptMigrationServiceImpl implements DeptMigrationService {

    @Resource
    private DeptSyncMapper deptSyncMapper;
    
    @Resource
    private ExternalAuthConfigMapper externalAuthConfigMapper;
    
    @Resource
    private DeptService deptService;
    
    @Resource
    private UserMigrationService userMigrationService;
    
    @Resource
    private com.kyx.service.business.dal.mysql.dept.DeptMapper deptMapper;

    @Override
    public List<DeptSyncDO> fetchExternalDepts(String systemName, String token) {
        // 获取外部系统配置
        ExternalAuthConfigDO config = externalAuthConfigMapper.selectBySystemName(systemName);
        if (config == null) {
            throw new RuntimeException("外部系统配置不存在: " + systemName);
        }

        if (StrUtil.isBlank(config.getDeptListUrl())) {
            throw new RuntimeException("外部系统部门接口地址未配置: " + systemName);
        }

        try {
            // 构建部门列表请求URL
            String deptListUrl = config.getBaseUrl() + config.getDeptListUrl();

            // 发送请求
            HttpRequest request = HttpRequest.get(deptListUrl)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Connection", "keep-alive")
                    .header("Sec-Fetch-Dest", "empty")
                    .header("Sec-Fetch-Mode", "cors")
                    .header("Sec-Fetch-Site", "same-origin")
                    .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36");

            // 添加认证头
            if (StrUtil.isNotBlank(token)) {
                request.header(config.getTokenHeader(), "Bearer " + token);
            }

            // 添加UUID头
            if (StrUtil.isNotBlank(config.getUuidValue())) {
                request.header(config.getUuidHeader(), config.getUuidValue());
            }

            HttpResponse response = request.execute();

            if (!response.isOk()) {
                throw new RuntimeException("获取部门列表失败: " + response.getStatus());
            }

            // 解析响应
            String responseBody = response.body();
            JSONObject responseJson = JSONUtil.parseObj(responseBody);
            
            log.info("部门列表响应: {}", responseBody);

            List<DeptSyncDO> deptList = new ArrayList<>();

            if (responseJson.containsKey("data")) {
                JSONArray dataArray = responseJson.getJSONArray("data");
                
                for (int i = 0; i < dataArray.size(); i++) {
                    JSONObject deptJson = dataArray.getJSONObject(i);
                    DeptSyncDO deptSync = convertToDeptSync(deptJson, systemName);
                    deptList.add(deptSync);
                }
            }

            return deptList;

        } catch (Exception e) {
            log.error("获取外部部门列表失败: {}", systemName, e);
            throw new RuntimeException("获取外部部门列表失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public SyncResult syncDeptsFromExternal(String systemName) {
        log.info("开始同步部门数据: systemName={}", systemName);
        
        SyncResult result = new SyncResult();
        List<String> errors = new ArrayList<>();
        int totalFetched = 0;
        int successCount = 0;
        int failedCount = 0;

        try {
            // 获取Token（复用用户迁移服务的Token）
            String token = userMigrationService.loginAndGetToken(systemName);

            // 获取部门数据
            List<DeptSyncDO> deptList = fetchExternalDepts(systemName, token);
            totalFetched = deptList.size();

            // 保存到同步表
            for (DeptSyncDO deptSync : deptList) {
                try {
                    // 检查是否已存在
                    DeptSyncDO existing = deptSyncMapper.selectByExternalDeptId(deptSync.getExternalDeptId());
                    if (existing == null) {
                        deptSyncMapper.insert(deptSync);
                        successCount++;
                    } else {
                        // 更新现有记录
                        deptSync.setId(existing.getId());
                        deptSyncMapper.updateById(deptSync);
                        successCount++;
                    }
                } catch (Exception e) {
                    failedCount++;
                    errors.add("保存部门失败: " + deptSync.getDeptName() + ", 错误: " + e.getMessage());
                    log.error("保存部门同步数据失败: {}", deptSync.getDeptName(), e);
                }
            }

            result.setTotalFetched(totalFetched);
            result.setSuccessCount(successCount);
            result.setFailedCount(failedCount);
            result.setErrors(errors);

            log.info("部门数据同步完成: totalFetched={}, successCount={}, failedCount={}", 
                    totalFetched, successCount, failedCount);

        } catch (Exception e) {
            errors.add("同步过程异常: " + e.getMessage());
            result.setErrors(errors);
            log.error("部门数据同步异常", e);
        }

        return result;
    }

    @Override
    @Transactional
    public MigrationResult migrateDeptsToSystemTable() {
        log.info("开始将同步表部门迁移到系统部门表");
        
        MigrationResult result = new MigrationResult();
        List<String> errors = new ArrayList<>();
        int totalMigrated = 0;
        int successCount = 0;
        int failedCount = 0;

        try {
            // 获取待同步的部门列表
            List<DeptSyncDO> pendingDepts = deptSyncMapper.selectPendingSyncList();
            
            // 先构建层级关系映射
            Map<String, Long> externalIdToSystemIdMap = new HashMap<>();
            
            // 按层级顺序处理部门（先处理根部门，再处理子部门）
            List<DeptSyncDO> sortedDepts = buildDeptHierarchyOrder(pendingDepts);
            
            for (DeptSyncDO deptSync : sortedDepts) {
                try {
                    // 检查系统中是否已存在该部门（通过名称和父部门ID查询）
                    Long parentId = 0L; // 默认为根部门
                    if (StrUtil.isNotBlank(deptSync.getExternalParentId()) && 
                        externalIdToSystemIdMap.containsKey(deptSync.getExternalParentId())) {
                        parentId = externalIdToSystemIdMap.get(deptSync.getExternalParentId());
                    }
                    DeptDO existingDept = deptMapper.selectByParentIdAndName(parentId, deptSync.getDeptName());
                    
                    if (existingDept == null) {
                        // 创建新部门
                        DeptSaveReqVO createReqVO = new DeptSaveReqVO();
                        createReqVO.setName(deptSync.getDeptName());
                        createReqVO.setSort(deptSync.getOrderNum());
                        createReqVO.setPhone(deptSync.getPhone());
                        createReqVO.setEmail(deptSync.getEmail());
                        createReqVO.setStatus(deptSync.getStatus());
                        
                        // 设置父部门ID
                        if (StrUtil.isNotBlank(deptSync.getExternalParentId()) && 
                            externalIdToSystemIdMap.containsKey(deptSync.getExternalParentId())) {
                            createReqVO.setParentId(externalIdToSystemIdMap.get(deptSync.getExternalParentId()));
                        } else {
                            createReqVO.setParentId(0L); // 根部门
                        }
                        
                        Long deptId = deptService.createDept(createReqVO);
                        
                        // 记录映射关系
                        externalIdToSystemIdMap.put(deptSync.getExternalDeptId(), deptId);
                        
                        // 更新同步状态、parentId和mappedDeptId
                        deptSync.setSyncStatus(DeptSyncDO.SyncStatus.SUCCESS.getCode());
                        deptSync.setSyncTime(LocalDateTime.now());
                        deptSync.setParentId(parentId); // 重要：更新同步表中的parentId字段
                        deptSync.setMappedDeptId(deptId); // 重要：保存外部ID到内部ID的映射
                        deptSyncMapper.updateById(deptSync);
                        
                        successCount++;
                        log.info("成功创建部门: {}, ID: {}", deptSync.getDeptName(), deptId);
                    } else {
                        // 部门已存在，记录映射关系
                        externalIdToSystemIdMap.put(deptSync.getExternalDeptId(), existingDept.getId());
                        
                        // 更新同步状态、parentId和mappedDeptId
                        deptSync.setSyncStatus(DeptSyncDO.SyncStatus.SUCCESS.getCode());
                        deptSync.setSyncTime(LocalDateTime.now());
                        deptSync.setSyncError("部门已存在");
                        deptSync.setParentId(parentId); // 重要：更新同步表中的parentId字段
                        deptSync.setMappedDeptId(existingDept.getId()); // 重要：保存外部ID到内部ID的映射
                        deptSyncMapper.updateById(deptSync);
                        
                        successCount++;
                        log.info("部门已存在，跳过创建: {}", deptSync.getDeptName());
                    }
                    
                    totalMigrated++;
                    
                } catch (Exception e) {
                    failedCount++;
                    String errorMsg = "迁移部门失败: " + deptSync.getDeptName() + ", 错误: " + e.getMessage();
                    errors.add(errorMsg);
                    
                    // 更新同步失败状态
                    deptSync.setSyncStatus(DeptSyncDO.SyncStatus.FAILED.getCode());
                    deptSync.setSyncTime(LocalDateTime.now());
                    deptSync.setSyncError(e.getMessage());
                    deptSyncMapper.updateById(deptSync);
                    
                    log.error("迁移部门失败: {}", deptSync.getDeptName(), e);
                }
            }

            result.setTotalMigrated(totalMigrated);
            result.setSuccessCount(successCount);
            result.setFailedCount(failedCount);
            result.setErrors(errors);

            log.info("部门迁移完成: totalMigrated={}, successCount={}, failedCount={}", 
                    totalMigrated, successCount, failedCount);

        } catch (Exception e) {
            errors.add("迁移过程异常: " + e.getMessage());
            result.setErrors(errors);
            log.error("部门迁移异常", e);
        }

        return result;
    }

    @Override
    public SyncStatusStat getDeptSyncStatusStat() {
        long pendingCount = deptSyncMapper.selectCount(DeptSyncDO::getSyncStatus, DeptSyncDO.SyncStatus.PENDING.getCode());
        long successCount = deptSyncMapper.selectCount(DeptSyncDO::getSyncStatus, DeptSyncDO.SyncStatus.SUCCESS.getCode());
        long failedCount = deptSyncMapper.selectCount(DeptSyncDO::getSyncStatus, DeptSyncDO.SyncStatus.FAILED.getCode());
        long totalCount = deptSyncMapper.selectCount();

        return new SyncStatusStat(pendingCount, successCount, failedCount, totalCount);
    }

    @Override
    public BuildHierarchyResult buildDeptHierarchy() {
        log.info("开始构建部门层级关系");
        
        BuildHierarchyResult result = new BuildHierarchyResult();
        List<String> errors = new ArrayList<>();
        int processedCount = 0;
        int successCount = 0;
        int failedCount = 0;

        try {
            // 获取所有部门同步数据
            List<DeptSyncDO> allDepts = deptSyncMapper.selectList();
            
            // 构建外部ID到部门对象的映射
            Map<String, DeptSyncDO> externalIdToDeptMap = allDepts.stream()
                    .collect(Collectors.toMap(DeptSyncDO::getExternalDeptId, dept -> dept));

            for (DeptSyncDO dept : allDepts) {
                try {
                    processedCount++;
                    
                    // 构建祖级列表
                    String ancestors = buildAncestors(dept, externalIdToDeptMap);
                    dept.setAncestors(ancestors);
                    
                    // 更新数据库
                    deptSyncMapper.updateById(dept);
                    
                    successCount++;
                    
                } catch (Exception e) {
                    failedCount++;
                    errors.add("构建部门层级失败: " + dept.getDeptName() + ", 错误: " + e.getMessage());
                    log.error("构建部门层级失败: {}", dept.getDeptName(), e);
                }
            }

            result.setProcessedCount(processedCount);
            result.setSuccessCount(successCount);
            result.setFailedCount(failedCount);
            result.setErrors(errors);

            log.info("部门层级构建完成: processedCount={}, successCount={}, failedCount={}", 
                    processedCount, successCount, failedCount);

        } catch (Exception e) {
            errors.add("构建层级过程异常: " + e.getMessage());
            result.setErrors(errors);
            log.error("部门层级构建异常", e);
        }

        return result;
    }

    /**
     * 将外部系统部门数据转换为同步数据对象
     */
    private DeptSyncDO convertToDeptSync(JSONObject deptJson, String systemName) {
        DeptSyncDO deptSync = new DeptSyncDO();
        
        // 基本信息
        deptSync.setExternalDeptId(deptJson.getStr("deptId"));
        deptSync.setDeptName(deptJson.getStr("deptName"));
        deptSync.setExternalParentId(deptJson.getStr("parentId"));
        deptSync.setOrderNum(deptJson.getInt("orderNum", 0));
        deptSync.setLeaderName(deptJson.getStr("leader"));
        deptSync.setPhone(deptJson.getStr("phone"));
        deptSync.setEmail(deptJson.getStr("email"));
        deptSync.setStatus(deptJson.getInt("status", CommonStatusEnum.ENABLE.getStatus()));
        
        // 同步状态
        deptSync.setSyncStatus(DeptSyncDO.SyncStatus.PENDING.getCode());
        
        // 保存原始数据
        deptSync.setExternalData(deptJson.toString());
        
        return deptSync;
    }

    /**
     * 按层级顺序排列部门（先父部门，后子部门）
     */
    private List<DeptSyncDO> buildDeptHierarchyOrder(List<DeptSyncDO> deptList) {
        List<DeptSyncDO> result = new ArrayList<>();
        Set<String> processed = new HashSet<>();
        
        // 构建外部ID到部门的映射
        Map<String, DeptSyncDO> idToDeptMap = deptList.stream()
                .collect(Collectors.toMap(DeptSyncDO::getExternalDeptId, dept -> dept));
        
        // 递归添加部门（先根部门，再子部门）
        for (DeptSyncDO dept : deptList) {
            if (!processed.contains(dept.getExternalDeptId())) {
                addDeptAndChildren(dept, idToDeptMap, result, processed);
            }
        }
        
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
        
        // 先添加父部门
        if (StrUtil.isNotBlank(dept.getExternalParentId()) && 
            idToDeptMap.containsKey(dept.getExternalParentId()) &&
            !processed.contains(dept.getExternalParentId())) {
            addDeptAndChildren(idToDeptMap.get(dept.getExternalParentId()), idToDeptMap, result, processed);
        }
        
        // 再添加当前部门
        result.add(dept);
        processed.add(dept.getExternalDeptId());
    }

    /**
     * 构建祖级列表
     */
    private String buildAncestors(DeptSyncDO dept, Map<String, DeptSyncDO> externalIdToDeptMap) {
        List<String> ancestorIds = new ArrayList<>();
        
        String currentParentId = dept.getExternalParentId();
        Set<String> visited = new HashSet<>(); // 防止循环引用
        
        while (StrUtil.isNotBlank(currentParentId) && 
               externalIdToDeptMap.containsKey(currentParentId) &&
               !visited.contains(currentParentId)) {
            
            visited.add(currentParentId);
            ancestorIds.add(0, currentParentId); // 插入到开头
            
            DeptSyncDO parentDept = externalIdToDeptMap.get(currentParentId);
            currentParentId = parentDept.getExternalParentId();
        }
        
        return String.join(",", ancestorIds);
    }
}