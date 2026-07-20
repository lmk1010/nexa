package com.kyx.service.business.service.migration;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.pojo.PageResult;

import com.kyx.service.business.controller.admin.user.vo.user.UserSaveReqVO;
import com.kyx.service.business.dal.dataobject.migration.ExternalAuthConfigDO;
import com.kyx.service.business.dal.dataobject.migration.UserSyncDO;
import com.kyx.service.business.dal.dataobject.user.AdminUserDO;
import com.kyx.service.business.dal.mysql.migration.ExternalAuthConfigMapper;
import com.kyx.service.business.dal.mysql.migration.UserSyncMapper;
import com.kyx.service.business.service.user.AdminUserService;
import com.kyx.service.business.service.dept.DeptService;
import com.kyx.service.business.dal.dataobject.dept.DeptDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 用户数据迁移服务实现
 * 
 * @author MK
 */
@Service
@Slf4j
public class UserMigrationServiceImpl implements UserMigrationService {

    @Resource
    private UserSyncMapper userSyncMapper;
    
    @Resource
    private ExternalAuthConfigMapper externalAuthConfigMapper;
    
    @Resource
    private AdminUserService adminUserService;
    
    @Resource
    private DeptService deptService;

    /**
     * 内存中的Token缓存
     */
    private final ConcurrentMap<String, TokenCache> tokenCacheMap = new ConcurrentHashMap<>();
    
    /**
     * 并发控制 - 控制同步请求的并发数
     */
    private final Semaphore syncSemaphore = new Semaphore(1); // 只允许一个同步任务运行
    
    /**
     * 同步状态标志 - 标记是否有同步任务正在运行
     */
    private final AtomicBoolean isSyncing = new AtomicBoolean(false);

    @Override
    public String loginAndGetToken(String systemName) {
        // 检查缓存
        TokenCache cached = tokenCacheMap.get(systemName);
        if (cached != null && !cached.isExpired()) {
            log.info("使用缓存的Token: {}", systemName);
            return cached.getToken();
        }

        // 获取外部系统配置
        ExternalAuthConfigDO config = externalAuthConfigMapper.selectBySystemName(systemName);
        if (config == null) {
            throw new RuntimeException("外部系统配置不存在: " + systemName);
        }

        try {
            // 构建登录请求
            String loginUrl = config.getBaseUrl() + config.getLoginUrl();
            JSONObject loginData = new JSONObject();
            loginData.set("username", config.getUsername());
            loginData.set("password", config.getPassword());

            // 发送登录请求
            HttpRequest request = HttpRequest.post(loginUrl)
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .header("isToken", "false")
                    .header("Accept", "*/*")
                    .header("Connection", "keep-alive");

            // 添加UUID头
            if (StrUtil.isNotBlank(config.getUuidValue())) {
                request.header(config.getUuidHeader(), config.getUuidValue());
            }

            HttpResponse response = request.body(loginData.toString()).execute();

            if (!response.isOk()) {
                throw new RuntimeException("登录请求失败: " + response.getStatus());
            }

            // 解析响应
            String responseBody = response.body();
            JSONObject responseJson = JSONUtil.parseObj(responseBody);
            
            log.info("登录响应: {}", responseBody);

            // 提取Token
            String token = null;
            if (responseJson.containsKey("data") && responseJson.getJSONObject("data").containsKey("accessToken")) {
                token = responseJson.getJSONObject("data").getStr("accessToken");
            } else if (responseJson.containsKey("token")) {
                token = responseJson.getStr("token");
            }

            if (StrUtil.isBlank(token)) {
                throw new RuntimeException("无法从登录响应中提取Token: " + responseBody);
            }

            // 缓存Token（1小时过期）
            tokenCacheMap.put(systemName, new TokenCache(token, System.currentTimeMillis() + 3600000));
            
            log.info("登录成功，获取到Token: {}", token);
            return token;

        } catch (Exception e) {
            log.error("登录外部系统失败: {}", systemName, e);
            throw new RuntimeException("登录外部系统失败: " + e.getMessage());
        }
    }

    @Override
    public PageResult<UserSyncDO> fetchExternalUsers(String systemName, String token, int pageNum, int pageSize) {
        // 获取外部系统配置
        ExternalAuthConfigDO config = externalAuthConfigMapper.selectBySystemName(systemName);
        if (config == null) {
            throw new RuntimeException("外部系统配置不存在: " + systemName);
        }

        try {
            // 构建用户列表请求URL
            String userListUrl = config.getBaseUrl() + config.getUserListUrl() 
                    + "?pageNum=" + pageNum + "&pageSize=" + pageSize;

            // 发送请求
            HttpRequest request = HttpRequest.get(userListUrl)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Connection", "keep-alive")
                    .header("Sec-Fetch-Dest", "empty")
                    .header("Sec-Fetch-Mode", "cors")
                    .header("Sec-Fetch-Site", "same-origin");

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
                throw new RuntimeException("获取用户列表失败: " + response.getStatus());
            }

            // 解析响应
            String responseBody = response.body();
            JSONObject responseJson = JSONUtil.parseObj(responseBody);
            
            log.info("用户列表响应: {}", responseBody);

            List<UserSyncDO> userList = new ArrayList<>();
            long total = 0;

            if (responseJson.containsKey("rows")) {
                JSONArray rows = responseJson.getJSONArray("rows");
                total = responseJson.getLong("total");
                
                for (int i = 0; i < rows.size(); i++) {
                    JSONObject userJson = rows.getJSONObject(i);
                    UserSyncDO userSync = convertToUserSync(userJson, systemName);
                    userList.add(userSync);
                }
            }

            return new PageResult<>(userList, total);

        } catch (Exception e) {
            log.error("获取外部用户列表失败: {}", systemName, e);
            throw new RuntimeException("获取外部用户列表失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public SyncResult syncUsersFromExternal(String systemName, int maxUsers) {
        log.info("开始同步用户数据: systemName={}, maxUsers={}", systemName, maxUsers);
        
        // 并发控制 - 检查是否有同步任务正在运行
        if (!isSyncing.compareAndSet(false, true)) {
            log.warn("同步任务已在运行中，拒绝新的同步请求");
            SyncResult result = new SyncResult();
            List<String> errors = new ArrayList<>();
            errors.add("同步任务已在运行中，请稍后再试");
            result.setErrors(errors);
            return result;
        }
        
        // 尝试获取信号量，如果获取失败则直接返回
        boolean acquired = false;
        try {
            acquired = syncSemaphore.tryAcquire();
            if (!acquired) {
                log.warn("无法获取同步信号量，同步任务被拒绝");
                SyncResult result = new SyncResult();
                List<String> errors = new ArrayList<>();
                errors.add("系统繁忙，同步任务被拒绝，请稍后再试");
                result.setErrors(errors);
                return result;
            }
            
            log.info("成功获取同步许可，开始执行用户数据同步");
            
            SyncResult result = new SyncResult();
            List<String> errors = new ArrayList<>();
            int totalFetched = 0;
            int successCount = 0;
            int failedCount = 0;

        try {
            // 获取Token
            String token = loginAndGetToken(systemName);

            // 分页获取用户数据 - 设计为180条数据分18次请求
            int pageSize = 10; // 每页10个用户，180条数据需要18次请求
            int pageNum = 1;
            boolean hasMore = true;
            int expectedPages = (maxUsers + pageSize - 1) / pageSize; // 计算预期页数
            
            log.info("开始分页同步，预计需要{}次请求，每页{}条数据", expectedPages, pageSize);

            while (hasMore && totalFetched < maxUsers) {
                try {
                    log.info("正在请求第{}/{}页数据...", pageNum, expectedPages);
                    
                    PageResult<UserSyncDO> pageResult = fetchExternalUsers(systemName, token, pageNum, pageSize);
                    
                    if (CollUtil.isEmpty(pageResult.getList())) {
                        log.info("第{}页无数据，结束同步", pageNum);
                        hasMore = false;
                        break;
                    }

                    log.info("第{}页获取到{}条数据", pageNum, pageResult.getList().size());

                    // 保存到同步表
                    for (UserSyncDO userSync : pageResult.getList()) {
                        if (totalFetched >= maxUsers) {
                            break;
                        }

                        try {
                            // 检查是否已存在
                            UserSyncDO existing = userSyncMapper.selectByExternalUserId(userSync.getExternalUserId());
                            if (existing == null) {
                                userSyncMapper.insert(userSync);
                                successCount++;
                            } else {
                                // 更新现有记录
                                userSync.setId(existing.getId());
                                userSyncMapper.updateById(userSync);
                                successCount++;
                            }
                            totalFetched++;
                        } catch (Exception e) {
                            failedCount++;
                            errors.add("保存用户失败: " + userSync.getUsername() + ", 错误: " + e.getMessage());
                            log.error("保存用户同步数据失败: {}", userSync.getUsername(), e);
                        }
                    }

                    log.info("第{}页处理完成，已同步: {}/{}条", pageNum, totalFetched, maxUsers);

                    // 检查是否还有更多数据
                    if (pageResult.getList().size() < pageSize || 
                        totalFetched >= pageResult.getTotal()) {
                        hasMore = false;
                    } else {
                        pageNum++;
                        
                        // 添加延迟控制，避免请求过于频繁
                        if (hasMore) {
                            try {
                                log.debug("延迟50ms后进行下一次请求...");
                                Thread.sleep(50); // 延迟50毫秒
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                log.warn("延迟被中断", ie);
                            }
                        }
                    }

                } catch (Exception e) {
                    errors.add("获取第" + pageNum + "页数据失败: " + e.getMessage());
                    log.error("获取第{}页用户数据失败", pageNum, e);
                    
                    // 请求失败时也添加延迟，避免立即重试造成更大压力
                    try {
                        Thread.sleep(100); // 失败时延迟更长时间
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    break;
                }
            }

            result.setTotalFetched(totalFetched);
            result.setSuccessCount(successCount);
            result.setFailedCount(failedCount);
            result.setErrors(errors);

            log.info("用户数据同步完成: 共{}次请求，totalFetched={}, successCount={}, failedCount={}", 
                    pageNum - 1, totalFetched, successCount, failedCount);

        } catch (Exception e) {
            errors.add("同步过程异常: " + e.getMessage());
            result.setErrors(errors);
            log.error("用户数据同步异常", e);
        }

        return result;
            
        } finally {
            // 释放信号量和重置同步状态
            if (acquired) {
                syncSemaphore.release();
                log.info("释放同步许可");
            }
            isSyncing.set(false);
            log.info("同步状态已重置");
        }
    }

    @Override
    @Transactional
    public MigrationResult migrateUsersToSystemTable() {
        log.info("开始将同步表用户迁移到系统用户表");
        
        MigrationResult result = new MigrationResult();
        List<String> errors = new ArrayList<>();
        int totalMigrated = 0;
        int successCount = 0;
        int failedCount = 0;

        try {
            // 获取待同步的用户列表
            List<UserSyncDO> pendingUsers = userSyncMapper.selectPendingSyncList();
            
            for (UserSyncDO userSync : pendingUsers) {
                try {
                    // 检查系统中是否已存在该用户
                    AdminUserDO existingUser = adminUserService.getUserByUsername(userSync.getUsername());
                    
                    if (existingUser == null) {
                        // 创建新用户
                        UserSaveReqVO createReqVO = new UserSaveReqVO();
                        createReqVO.setUsername(userSync.getUsername());
                        createReqVO.setNickname(userSync.getNickname());
                        createReqVO.setEmail(userSync.getEmail());
                        createReqVO.setMobile(userSync.getMobile());
                        createReqVO.setSex(userSync.getSex());
                        createReqVO.setAvatar(userSync.getAvatar());
                        createReqVO.setPassword("123456"); // 默认密码
                        
                        // 设置部门ID（通过部门名称匹配）
                        Long deptId = null;
                        
                        // 根据deptName匹配现有部门
                        if (StrUtil.isNotBlank(userSync.getDeptName())) {
                            try {
                                DeptDO dept = deptService.getDeptByName(userSync.getDeptName());
                                if (dept != null) {
                                    deptId = dept.getId();
                                    log.info("用户[{}]根据部门名称[{}]匹配到部门ID[{}]", 
                                            userSync.getUsername(), userSync.getDeptName(), deptId);
                                } else {
                                    log.warn("用户[{}]的部门名称[{}]未找到匹配的部门，将跳过部门设置", 
                                            userSync.getUsername(), userSync.getDeptName());
                                }
                            } catch (Exception e) {
                                log.error("根据部门名称[{}]查找部门失败: {}", userSync.getDeptName(), e.getMessage());
                            }
                        }
                        
                        // 设置部门ID（如果找到的话）
                        if (deptId != null) {
                            createReqVO.setDeptId(deptId);
                        }
                        
                        Long userId = adminUserService.createUser(createReqVO);
                        
                        // 更新同步状态
                        userSync.setSyncStatus(UserSyncDO.SyncStatus.SUCCESS.getCode());
                        userSync.setSyncTime(LocalDateTime.now());
                        userSyncMapper.updateById(userSync);
                        
                        successCount++;
                        log.info("成功创建用户: {}, ID: {}, 部门ID: {}, 部门名称: {}", 
                                userSync.getUsername(), userId, deptId, userSync.getDeptName());
                    } else {
                        // 用户已存在，更新同步状态
                        userSync.setSyncStatus(UserSyncDO.SyncStatus.SUCCESS.getCode());
                        userSync.setSyncTime(LocalDateTime.now());
                        userSync.setSyncError("用户已存在");
                        userSyncMapper.updateById(userSync);
                        successCount++;
                        log.info("用户已存在，跳过创建: {}", userSync.getUsername());
                    }
                    
                    totalMigrated++;
                    
                } catch (Exception e) {
                    failedCount++;
                    String errorMsg = "迁移用户失败: " + userSync.getUsername() + ", 错误: " + e.getMessage();
                    errors.add(errorMsg);
                    
                    // 更新同步失败状态
                    userSync.setSyncStatus(UserSyncDO.SyncStatus.FAILED.getCode());
                    userSync.setSyncTime(LocalDateTime.now());
                    userSync.setSyncError(e.getMessage());
                    userSyncMapper.updateById(userSync);
                    
                    log.error("迁移用户失败: {}", userSync.getUsername(), e);
                }
            }

            result.setTotalMigrated(totalMigrated);
            result.setSuccessCount(successCount);
            result.setFailedCount(failedCount);
            result.setErrors(errors);

            log.info("用户迁移完成: totalMigrated={}, successCount={}, failedCount={}", 
                    totalMigrated, successCount, failedCount);

        } catch (Exception e) {
            errors.add("迁移过程异常: " + e.getMessage());
            result.setErrors(errors);
            log.error("用户迁移异常", e);
        }

        return result;
    }

    @Override
    public SyncStatusStat getSyncStatusStat() {
        long pendingCount = userSyncMapper.selectCount(UserSyncDO::getSyncStatus, UserSyncDO.SyncStatus.PENDING.getCode());
        long successCount = userSyncMapper.selectCount(UserSyncDO::getSyncStatus, UserSyncDO.SyncStatus.SUCCESS.getCode());
        long failedCount = userSyncMapper.selectCount(UserSyncDO::getSyncStatus, UserSyncDO.SyncStatus.FAILED.getCode());
        long totalCount = userSyncMapper.selectCount();

        return new SyncStatusStat(pendingCount, successCount, failedCount, totalCount);
    }

    /**
     * 将外部系统用户数据转换为同步数据对象
     */
    private UserSyncDO convertToUserSync(JSONObject userJson, String systemName) {
        UserSyncDO userSync = new UserSyncDO();
        
        // 基本信息
        userSync.setExternalUserId(userJson.getStr("userId"));
        userSync.setUsername(userJson.getStr("userName"));
        // 注意：外部系统的name字段对应我们系统的nickname
        userSync.setNickname(userJson.getStr("name")); // 使用name作为nickname
        userSync.setEmail(userJson.getStr("email"));
        userSync.setMobile(userJson.getStr("mobile")); // 使用mobile而不是phonenumber
        userSync.setSex(userJson.getInt("sex", 0));
        userSync.setAvatar(userJson.getStr("avatar"));
        userSync.setStatus(userJson.getInt("status", CommonStatusEnum.ENABLE.getStatus()));
        
        // 地区相关信息
        userSync.setRegion(userJson.getInt("region"));
        if (userJson.containsKey("regionPath")) {
            userSync.setRegionPath(userJson.getJSONArray("regionPath").toString());
        }
        if (userJson.containsKey("regionPathStr")) {
            userSync.setRegionPathStr(userJson.getJSONArray("regionPathStr").toString());
        }
        userSync.setAddress(userJson.getStr("address"));
        
        // 登录相关信息
        userSync.setLoginIp(userJson.getStr("loginIp"));
        String loginDateStr = userJson.getStr("loginDate");
        if (StrUtil.isNotBlank(loginDateStr)) {
            try {
                userSync.setLoginDate(LocalDateTime.parse(loginDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } catch (Exception e) {
                log.warn("解析登录时间失败: {}", loginDateStr);
            }
        }
        
        // 用户类型和工作相关
        userSync.setUserType(userJson.getStr("userType"));
        userSync.setLinkName(userJson.getStr("linkName"));
        userSync.setWorked(userJson.getInt("worked"));
        userSync.setPermissionPd(userJson.getBool("permissionPd"));
        userSync.setPermissionGd(userJson.getBool("permissionGd"));
        userSync.setHeirUid(userJson.getLong("heirUid"));
        userSync.setSameWork(userJson.getStr("sameWork"));
        
        // 联系方式
        userSync.setWechat(userJson.getStr("wechat"));
        
        // 积分和权重相关
        userSync.setRoleName(userJson.getStr("roleName"));
        userSync.setIntegral(userJson.getDouble("integral"));
        userSync.setPoint(userJson.getDouble("point"));
        userSync.setWorkWeight(userJson.getInt("workWeight"));
        userSync.setPlatform(userJson.getStr("platform"));
        userSync.setOrderNum(userJson.getInt("orderNum"));
        
        // 上线时间
        String upTimeStr = userJson.getStr("upTime");
        if (StrUtil.isNotBlank(upTimeStr)) {
            try {
                userSync.setUpTime(LocalDateTime.parse(upTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } catch (Exception e) {
                log.warn("解析上线时间失败: {}", upTimeStr);
            }
        }
        
        // 新增字段 - 保留原有的userLevel设置
        userSync.setUserLevel(userJson.getInt("userLevel", 1)); // 用户等级，默认为1
        
        // 部门和岗位信息
        if (userJson.containsKey("dept") && userJson.getJSONObject("dept") != null) {
            userSync.setDeptName(userJson.getJSONObject("dept").getStr("deptName"));
        }
        
        // 同步状态
        userSync.setSyncStatus(UserSyncDO.SyncStatus.PENDING.getCode());
        
        // 保存原始数据
        userSync.setExternalData(userJson.toString());
        
        return userSync;
    }

    /**
     * Token缓存类
     */
    private static class TokenCache {
        private final String token;
        private final long expireTime;

        public TokenCache(String token, long expireTime) {
            this.token = token;
            this.expireTime = expireTime;
        }

        public String getToken() {
            return token;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }
}