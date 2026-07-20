package com.kyx.service.hr.integration.dingtalk.model;

import lombok.Data;

/**
 * DingTalk department snapshot used to mirror the organization tree into OA.
 */
@Data
public class DingTalkDeptSnapshot {

    private Long deptId;

    private String name;

    private Long parentId;

    private Long topDeptId;

    private String topDeptName;

    /**
     * 钉钉部门排序（order），数值越小越靠前。
     */
    private Long order;

}
