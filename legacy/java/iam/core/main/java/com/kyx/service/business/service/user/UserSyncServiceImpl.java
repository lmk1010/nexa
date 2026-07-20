package com.kyx.service.business.service.user;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.kyx.service.business.controller.admin.user.vo.user.UserSaveReqVO;
import com.kyx.service.business.dal.dataobject.user.AdminUserDO;
import com.kyx.service.business.dal.dataobject.migration.UserSyncDO;
import com.kyx.service.business.dal.mysql.migration.UserSyncMapper;
import com.kyx.service.business.service.migration.UserMigrationService;
import com.kyx.service.business.service.dept.DeptService;
import com.kyx.service.business.dal.dataobject.dept.DeptDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户同步 Service 实现类
 *
 * @author MK
 */
@Service
@Validated
@Slf4j
public class UserSyncServiceImpl implements UserSyncService {

    @Resource
    private UserSyncMapper userSyncMapper;

    @Resource
    private AdminUserService adminUserService;

    @Resource
    private UserMigrationService userMigrationService;

    @Resource
    private DeptService deptService;

    // 外部系统配置，从配置文件读取
    @Value("${sync.external.user.system-name:连途}")
    private String externalSystemName;

    @Value("${sync.external.user.max-users:1000}")
    private int maxUsers;



    @Override
    @Transactional(rollbackFor = Exception.class)
    public int fetchAndSaveExternalUsers() {
        try {
            log.info("开始拉取外部用户数据...");
            
            // 使用迁移服务拉取外部数据
            UserMigrationService.SyncResult syncResult = userMigrationService.syncUsersFromExternal(externalSystemName, maxUsers);
            
            log.info("成功拉取并保存外部用户数据，共{}条", syncResult.getSuccessCount());
            return syncResult.getSuccessCount();
            
        } catch (Exception e) {
            log.error("拉取并保存外部用户数据失败", e);
            throw new RuntimeException("拉取并保存外部用户数据失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String syncPendingUsers() {
        try {
            log.info("开始执行待同步用户数据同步...");
            
            // 获取所有待同步的记录
            List<UserSyncDO> pendingSyncs = userSyncMapper.selectPendingSyncList();
            
            if (pendingSyncs.isEmpty()) {
                log.info("没有待同步的用户数据");
                return "没有待同步的用户数据";
            }

            int successCount = 0;
            int failCount = 0;

            for (UserSyncDO userSync : pendingSyncs) {
                try {
                    // 检查本地是否已存在相同用户名的用户
                    AdminUserDO existingUser = null;
                    if (StrUtil.isNotBlank(userSync.getUsername())) {
                        existingUser = adminUserService.getUserByUsername(userSync.getUsername());
                    }
                    
                    // 如果用户名不存在，尝试用手机号查找
                    if (existingUser == null && StrUtil.isNotBlank(userSync.getMobile())) {
                        existingUser = adminUserService.getUserByMobile(userSync.getMobile());
                    }
                    
                    // 如果手机号不存在，尝试用邮箱查找
                    if (existingUser == null && StrUtil.isNotBlank(userSync.getEmail())) {
                        existingUser = adminUserService.getUserByEmail(userSync.getEmail());
                    }

                    UserSaveReqVO userSaveReqVO = new UserSaveReqVO();
                    userSaveReqVO.setUsername(userSync.getUsername());
                    // 使用name字段作为nickname
                    userSaveReqVO.setNickname(StrUtil.isNotBlank(userSync.getNickname()) ? userSync.getNickname() : userSync.getUsername());
                    userSaveReqVO.setEmail(userSync.getEmail());
                    userSaveReqVO.setMobile(userSync.getMobile());
                    userSaveReqVO.setSex(userSync.getSex() != null ? userSync.getSex() : 2); // 默认未知
                    userSaveReqVO.setAvatar(userSync.getAvatar());
                    
                    // 设置微信号
                    userSaveReqVO.setWechatId(userSync.getWechat());
                    
                    // 设置用户等级
                    userSaveReqVO.setUserLevel(userSync.getUserLevel() != null ? userSync.getUserLevel() : 1);
                    
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
                        userSaveReqVO.setDeptId(deptId);
                    }
                    
                    // 设置岗位IDs（从postIds或roles信息解析）
                    if (StrUtil.isNotBlank(userSync.getExternalData())) {
                        try {
                            JSONObject externalData = JSONUtil.parseObj(userSync.getExternalData());
                            if (externalData.containsKey("postIds") && externalData.getJSONArray("postIds") != null) {
                                Set<Long> postIds = new HashSet<>();
                                JSONArray postIdsArray = externalData.getJSONArray("postIds");
                                for (int i = 0; i < postIdsArray.size(); i++) {
                                    Long postId = postIdsArray.getLong(i);
                                    if (postId != null) {
                                        postIds.add(postId);
                                    }
                                }
                                if (!postIds.isEmpty()) {
                                    userSaveReqVO.setPostIds(postIds);
                                }
                            }
                        } catch (Exception e) {
                            log.warn("解析外部数据中的岗位信息失败: {}", e.getMessage());
                        }
                    }
                    
                    // 设置备注信息，包含同步过来的额外信息
                    StringBuilder remarkBuilder = new StringBuilder();
                    if (StrUtil.isNotBlank(userSync.getAddress())) {
                        remarkBuilder.append("地址：").append(userSync.getAddress()).append("; ");
                    }
                    if (StrUtil.isNotBlank(userSync.getWechat())) {
                        remarkBuilder.append("微信：").append(userSync.getWechat()).append("; ");
                    }
                    if (StrUtil.isNotBlank(userSync.getLinkName())) {
                        remarkBuilder.append("联系人：").append(userSync.getLinkName()).append("; ");
                    }
                    if (StrUtil.isNotBlank(userSync.getUserType())) {
                        remarkBuilder.append("用户类型：").append(userSync.getUserType()).append("; ");
                    }
                    if (userSync.getIntegral() != null) {
                        remarkBuilder.append("积分：").append(userSync.getIntegral()).append("; ");
                    }
                    if (remarkBuilder.length() > 0) {
                        userSaveReqVO.setRemark(remarkBuilder.toString());
                    }
                    
                    // 设置默认密码（同步的用户需要重置密码）
                    userSaveReqVO.setPassword("123456"); // 可以从配置文件读取默认密码

                    Long localUserId;
                    if (existingUser != null) {
                        // 更新现有用户
                        userSaveReqVO.setId(existingUser.getId());
                        adminUserService.updateUser(userSaveReqVO);
                        localUserId = existingUser.getId();
                        log.info("更新用户成功：{}", userSync.getUsername());
                    } else {
                        // 创建新用户
                        localUserId = adminUserService.createUser(userSaveReqVO);
                        log.info("创建用户成功：{}，ID：{}，包含字段：昵称[{}]，邮箱[{}]，手机[{}]，微信[{}]，部门ID[{}]，用户类型[{}]", 
                                userSync.getUsername(), localUserId, userSync.getNickname(), 
                                userSync.getEmail(), userSync.getMobile(), userSync.getWechat(), 
                                userSaveReqVO.getDeptId(), userSync.getUserType());
                    }

                    // 更新同步状态
                    userSync.setSyncStatus(UserSyncDO.SyncStatus.SUCCESS.getCode());
                    userSync.setSyncTime(LocalDateTime.now());
                    userSync.setSyncError(null);
                    userSyncMapper.updateById(userSync);

                    successCount++;

                } catch (Exception e) {
                    log.error("同步用户失败：{}，错误：{}", userSync.getUsername(), e.getMessage(), e);
                    
                    // 更新同步状态为失败
                    userSync.setSyncStatus(UserSyncDO.SyncStatus.FAILED.getCode());
                    userSync.setSyncTime(LocalDateTime.now());
                    userSync.setSyncError(e.getMessage());
                    userSyncMapper.updateById(userSync);
                    
                    failCount++;
                }
            }

            String result = String.format("用户同步完成：成功 %d 条，失败 %d 条", successCount, failCount);
            log.info(result);
            return result;

        } catch (Exception e) {
            log.error("执行待同步用户数据同步失败", e);
            throw new RuntimeException("执行待同步用户数据同步失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupSyncedRecords(int daysToKeep) {
        try {
            log.info("开始清理已同步的用户记录，保留{}天", daysToKeep);
            
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysToKeep);
            
            // 删除超过保留期限的已同步记录
            // 使用查询然后删除的方式，避免lambdaQuery方法不存在的问题
            List<UserSyncDO> recordsToDelete = userSyncMapper.selectListBySyncStatus(UserSyncDO.SyncStatus.SUCCESS.getCode())
                    .stream()
                    .filter(record -> record.getSyncTime() != null && record.getSyncTime().isBefore(cutoffTime))
                    .collect(java.util.stream.Collectors.toList());
            
            int deletedCount = 0;
            for (UserSyncDO record : recordsToDelete) {
                userSyncMapper.deleteById(record.getId());
                deletedCount++;
            }
            
            log.info("清理已同步的用户记录完成，删除{}条记录", deletedCount);
            return deletedCount;
            
        } catch (Exception e) {
            log.error("清理已同步用户记录失败", e);
            throw new RuntimeException("清理已同步用户记录失败：" + e.getMessage());
        }
    }

}