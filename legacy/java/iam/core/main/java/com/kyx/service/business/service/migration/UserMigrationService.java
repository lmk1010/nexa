package com.kyx.service.business.service.migration;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.business.dal.dataobject.migration.UserSyncDO;

import java.util.List;

/**
 * 用户数据迁移服务接口
 * 
 * 提供用户数据从外部系统迁移到本系统的功能
 * 包括用户数据获取、存储、同步等操作
 * 
 * @author MK
 */
public interface UserMigrationService {

    /**
     * 从外部系统登录并获取访问令牌
     * 
     * @param systemName 外部系统名称
     * @return 访问令牌
     */
    String loginAndGetToken(String systemName);

    /**
     * 从外部系统获取用户列表
     * 
     * @param systemName 外部系统名称
     * @param token 访问令牌
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 用户数据列表
     */
    PageResult<UserSyncDO> fetchExternalUsers(String systemName, String token, int pageNum, int pageSize);

    /**
     * 批量同步用户数据
     * 
     * @param systemName 外部系统名称
     * @param maxUsers 最大同步用户数
     * @return 同步结果统计
     */
    SyncResult syncUsersFromExternal(String systemName, int maxUsers);

    /**
     * 将同步表中的用户数据迁移到正式用户表
     * 
     * @return 迁移结果统计
     */
    MigrationResult migrateUsersToSystemTable();

    /**
     * 获取同步状态统计
     * 
     * @return 同步状态统计信息
     */
    SyncStatusStat getSyncStatusStat();

    /**
     * 同步结果统计
     */
    class SyncResult {
        private int totalFetched;
        private int successCount;
        private int failedCount;
        private List<String> errors;

        public SyncResult() {}

        public SyncResult(int totalFetched, int successCount, int failedCount, List<String> errors) {
            this.totalFetched = totalFetched;
            this.successCount = successCount;
            this.failedCount = failedCount;
            this.errors = errors;
        }

        // Getters and Setters
        public int getTotalFetched() { return totalFetched; }
        public void setTotalFetched(int totalFetched) { this.totalFetched = totalFetched; }
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public int getFailedCount() { return failedCount; }
        public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
    }

    /**
     * 迁移结果统计
     */
    class MigrationResult {
        private int totalMigrated;
        private int successCount;
        private int failedCount;
        private List<String> errors;

        public MigrationResult() {}

        public MigrationResult(int totalMigrated, int successCount, int failedCount, List<String> errors) {
            this.totalMigrated = totalMigrated;
            this.successCount = successCount;
            this.failedCount = failedCount;
            this.errors = errors;
        }

        // Getters and Setters
        public int getTotalMigrated() { return totalMigrated; }
        public void setTotalMigrated(int totalMigrated) { this.totalMigrated = totalMigrated; }
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public int getFailedCount() { return failedCount; }
        public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
    }

    /**
     * 同步状态统计
     */
    class SyncStatusStat {
        private long pendingCount;
        private long successCount;
        private long failedCount;
        private long totalCount;

        public SyncStatusStat() {}

        public SyncStatusStat(long pendingCount, long successCount, long failedCount, long totalCount) {
            this.pendingCount = pendingCount;
            this.successCount = successCount;
            this.failedCount = failedCount;
            this.totalCount = totalCount;
        }

        // Getters and Setters
        public long getPendingCount() { return pendingCount; }
        public void setPendingCount(long pendingCount) { this.pendingCount = pendingCount; }
        public long getSuccessCount() { return successCount; }
        public void setSuccessCount(long successCount) { this.successCount = successCount; }
        public long getFailedCount() { return failedCount; }
        public void setFailedCount(long failedCount) { this.failedCount = failedCount; }
        public long getTotalCount() { return totalCount; }
        public void setTotalCount(long totalCount) { this.totalCount = totalCount; }
    }
}