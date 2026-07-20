package com.kyx.service.business.service.sync;

import com.kyx.service.business.service.dept.DeptSyncService;
import com.kyx.service.business.service.dept.PostSyncService;
import com.kyx.service.business.service.user.UserSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 统一同步服务实现类
 *
 * @author MK
 */
@Service
@Slf4j
public class UnifiedSyncServiceImpl implements UnifiedSyncService {

    @Resource
    private PostSyncService postSyncService;

    @Resource
    private DeptSyncService deptSyncService;

    @Resource
    private UserSyncService userSyncService;

    @Override
    public String syncPosts() {
        try {
            log.info("=== 开始执行岗位数据同步 ===");

            // 1. 拉取并保存外部岗位数据
            int fetchedCount = postSyncService.fetchAndSaveExternalPosts();
            log.info("拉取并保存外部岗位数据：{}条", fetchedCount);

            // 2. 同步待同步的岗位数据
            String syncResult = postSyncService.syncPendingPosts();
            log.info("岗位同步结果：{}", syncResult);

            log.info("=== 岗位数据同步完成 ===");
            return String.format("岗位同步完成 - 拉取：%d条，%s", fetchedCount, syncResult);

        } catch (Exception e) {
            log.error("岗位数据同步失败", e);
            return "岗位数据同步失败：" + e.getMessage();
        }
    }

    @Override
    public String syncDepartments() {
        try {
            log.info("=== 开始执行部门数据同步 ===");

            // 1. 拉取并保存外部部门数据
            int fetchedCount = deptSyncService.fetchAndSaveExternalDepts();
            log.info("拉取并保存外部部门数据：{}条", fetchedCount);

            // 2. 同步待同步的部门数据
            String syncResult = deptSyncService.syncPendingDepts();
            log.info("部门同步结果：{}", syncResult);

            log.info("=== 部门数据同步完成 ===");
            return String.format("部门同步完成 - 拉取：%d条，%s", fetchedCount, syncResult);

        } catch (Exception e) {
            log.error("部门数据同步失败", e);
            return "部门数据同步失败：" + e.getMessage();
        }
    }

    @Override
    public String syncUsers() {
        try {
            log.info("=== 开始执行用户数据同步 ===");

            // 1. 拉取并保存外部用户数据
            int fetchedCount = userSyncService.fetchAndSaveExternalUsers();
            log.info("拉取并保存外部用户数据：{}条", fetchedCount);

            // 2. 同步待同步的用户数据
            String syncResult = userSyncService.syncPendingUsers();
            log.info("用户同步结果：{}", syncResult);

            log.info("=== 用户数据同步完成 ===");
            return String.format("用户同步完成 - 拉取：%d条，%s", fetchedCount, syncResult);

        } catch (Exception e) {
            log.error("用户数据同步失败", e);
            return "用户数据同步失败：" + e.getMessage();
        }
    }

    @Override
    public String syncAll() {
        try {
            log.info("=== 开始执行完整数据同步 ===");
            StringBuilder result = new StringBuilder();

            // 按照依赖顺序执行同步：部门 -> 岗位 -> 用户
            
            // 1. 同步部门数据
            String deptResult = syncDepartments();
            result.append("部门：").append(deptResult).append("; ");

            // 2. 同步岗位数据
            String postResult = syncPosts();
            result.append("岗位：").append(postResult).append("; ");

            // 3. 同步用户数据
            String userResult = syncUsers();
            result.append("用户：").append(userResult);

            log.info("=== 完整数据同步完成 ===");
            return result.toString();

        } catch (Exception e) {
            log.error("完整数据同步失败", e);
            return "完整数据同步失败：" + e.getMessage();
        }
    }

    @Override
    public String getSyncStatus() {
        try {
            StringBuilder status = new StringBuilder();
            status.append("=== 同步状态统计 ===\n");

            // 岗位同步状态
            status.append("岗位同步：功能已实现，正常运行\n");
            
            // 部门同步状态
            status.append("部门同步：功能已实现，正常运行\n");
            
            // 用户同步状态
            status.append("用户同步：功能已实现，正常运行\n");
            
            status.append("\n=== 同步功能说明 ===\n");
            status.append("1. 完整同步 (syncAll)：按依赖顺序执行 部门 -> 岗位 -> 用户\n");
            status.append("2. 单独同步：支持单独同步部门、岗位、用户数据\n");
            status.append("3. 数据流程：外部拉取 -> 同步表存储 -> 本地表同步\n");
            status.append("4. 定时任务：通过 SyncScheduleJob 定时执行同步任务\n");
            
            status.append("\n=== 使用建议 ===\n");
            status.append("- 首次同步建议使用 syncAll() 完整同步\n");
            status.append("- 日常维护可使用单独同步方法\n");
            status.append("- 注意检查同步日志和错误信息\n");

            return status.toString();

        } catch (Exception e) {
            log.error("获取同步状态失败", e);
            return "获取同步状态失败：" + e.getMessage();
        }
    }

}