package com.kyx.service.erp.enums.asset;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ERP 盘点计划状态枚举
 *
 * @author kyx
 */
@AllArgsConstructor
@Getter
public enum ErpInventoryPlanStatusEnum {

    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布"),
    IN_PROGRESS(2, "进行中"),
    COMPLETED(3, "已完成"),
    SUBMITTED(4, "已提交"),
    PENDING_AUDIT(5, "待审核"),
    AUDITED_CLOSED(6, "已审核/已关闭"),
    CANCELLED(7, "已取消");

    /**
     * 状态值
     */
    private final Integer status;
    
    /**
     * 状态名称
     */
    private final String name;

    /**
     * 根据状态值获取枚举
     *
     * @param status 状态值
     * @return 对应的枚举，如果没有找到则返回null
     */
    public static ErpInventoryPlanStatusEnum valueOf(Integer status) {
        for (ErpInventoryPlanStatusEnum statusEnum : values()) {
            if (statusEnum.getStatus().equals(status)) {
                return statusEnum;
            }
        }
        return null;
    }

    /**
     * 判断是否可以进行编辑操作
     *
     * @param status 状态值
     * @return 是否可以编辑
     */
    public static boolean canEdit(Integer status) {
        return DRAFT.getStatus().equals(status);
    }

    /**
     * 判断是否可以进行删除操作
     *
     * @param status 状态值
     * @return 是否可以删除
     */
    public static boolean canDelete(Integer status) {
        return DRAFT.getStatus().equals(status);
    }

    /**
     * 判断是否可以进行发布操作
     *
     * @param status 状态值
     * @return 是否可以发布
     */
    public static boolean canPublish(Integer status) {
        return DRAFT.getStatus().equals(status);
    }

    /**
     * 判断是否可以进行扫码操作
     *
     * @param status 状态值
     * @return 是否可以扫码
     */
    public static boolean canScan(Integer status) {
        return PUBLISHED.getStatus().equals(status) || IN_PROGRESS.getStatus().equals(status);
    }

    /**
     * 判断是否可以进行提交操作
     *
     * @param status 状态值
     * @return 是否可以提交
     */
    public static boolean canSubmit(Integer status) {
        return COMPLETED.getStatus().equals(status);
    }

    /**
     * 判断是否可以进行审核操作
     *
     * @param status 状态值
     * @return 是否可以审核
     */
    public static boolean canAudit(Integer status) {
        return PENDING_AUDIT.getStatus().equals(status);
    }

    /**
     * 判断是否为终态（不能再进行状态变更）
     *
     * @param status 状态值
     * @return 是否为终态
     */
    public static boolean isFinalStatus(Integer status) {
        return AUDITED_CLOSED.getStatus().equals(status) || CANCELLED.getStatus().equals(status);
    }

} 