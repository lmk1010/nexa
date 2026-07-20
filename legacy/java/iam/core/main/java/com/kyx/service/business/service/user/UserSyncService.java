package com.kyx.service.business.service.user;

/**
 * 用户同步 Service 接口
 *
 * @author MK
 */
public interface UserSyncService {

    /**
     * 从外部系统拉取并保存用户数据
     *
     * @return 拉取并保存的数量
     */
    int fetchAndSaveExternalUsers();

    /**
     * 执行待同步用户数据同步
     *
     * @return 同步结果信息
     */
    String syncPendingUsers();

    /**
     * 清理已同步的记录（可选，保留一定时间的记录用于审计）
     *
     * @param daysToKeep 保留天数
     * @return 清理的记录数
     */
    int cleanupSyncedRecords(int daysToKeep);

}