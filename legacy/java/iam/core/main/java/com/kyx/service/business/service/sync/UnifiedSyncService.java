package com.kyx.service.business.service.sync;

/**
 * 统一同步服务接口
 * 用于管理所有外部系统的数据同步
 *
 * @author MK
 */
public interface UnifiedSyncService {

    /**
     * 执行岗位数据同步
     * 包括：拉取外部数据 -> 保存到同步表 -> 同步到本地表
     *
     * @return 同步结果信息
     */
    String syncPosts();

    /**
     * 执行部门数据同步
     * TODO: 后续可以添加部门同步功能
     *
     * @return 同步结果信息
     */
    String syncDepartments();

    /**
     * 执行用户数据同步
     * TODO: 后续可以添加用户同步功能
     *
     * @return 同步结果信息
     */
    String syncUsers();

    /**
     * 执行完整的数据同步
     * 按照正确的顺序执行：部门 -> 岗位 -> 用户
     *
     * @return 完整同步结果信息
     */
    String syncAll();

    /**
     * 获取同步状态统计
     *
     * @return 同步状态统计信息
     */
    String getSyncStatus();

}