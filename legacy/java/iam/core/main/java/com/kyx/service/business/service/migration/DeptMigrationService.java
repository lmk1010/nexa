package com.kyx.service.business.service.migration;


import com.kyx.service.business.dal.dataobject.migration.DeptSyncDO;

import java.util.List;

/**
 * 部门数据迁移服务接口
 * 
 * 提供部门数据从外部系统迁移到本系统的功能
 * 包括部门数据获取、存储、同步等操作
 * 
 * @author MK
 */
public interface DeptMigrationService {

    /**
     * 从外部系统获取部门列表
     * 
     * @param systemName 外部系统名称
     * @param token 访问令牌
     * @return 部门数据列表
     */
    List<DeptSyncDO> fetchExternalDepts(String systemName, String token);

    /**
     * 同步部门数据到同步表
     * 
     * @param systemName 外部系统名称
     * @return 同步结果统计
     */
    SyncResult syncDeptsFromExternal(String systemName);

    /**
     * 将同步表中的部门数据迁移到正式部门表
     * 
     * @return 迁移结果统计
     */
    MigrationResult migrateDeptsToSystemTable();

    /**
     * 获取部门同步状态统计
     * 
     * @return 同步状态统计信息
     */
    SyncStatusStat getDeptSyncStatusStat();

    /**
     * 构建部门层级关系
     * 
     * @return 构建结果
     */
    BuildHierarchyResult buildDeptHierarchy();

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

    /**
     * 层级构建结果
     */
    class BuildHierarchyResult {
        private int processedCount;
        private int successCount;
        private int failedCount;
        private List<String> errors;

        public BuildHierarchyResult() {}

        public BuildHierarchyResult(int processedCount, int successCount, int failedCount, List<String> errors) {
            this.processedCount = processedCount;
            this.successCount = successCount;
            this.failedCount = failedCount;
            this.errors = errors;
        }

        // Getters and Setters
        public int getProcessedCount() { return processedCount; }
        public void setProcessedCount(int processedCount) { this.processedCount = processedCount; }
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public int getFailedCount() { return failedCount; }
        public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
    }
}