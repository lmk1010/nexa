package com.kyx.service.hr.integration.dingtalk.model;

import lombok.Data;

import java.util.List;

/**
 * DingTalk user snapshot used by sync pipeline.
 */
@Data
public class DingTalkUserSnapshot {

    private String userId;

    private String name;

    private String mobile;

    private String email;

    private String jobTitle;

    /**
     * 钉钉工号（topapi/v2/user/get 的 job_number）。
     */
    private String jobNumber;

    private Boolean active;

    private Boolean admin;

    private Boolean boss;

    private Boolean leader;

    /**
     * 钉钉直属主管 userid（topapi/v2/user/get 的 manager_userid）。
     */
    private String managerUserId;

    private List<String> roleNames;

    private Long deptId;

    private String deptName;

    private Long topDeptId;

    private String topDeptName;

    private String rawPayload;

}
