package com.kyx.service.hr.integration.dingtalk.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * DingTalk attendance snapshot for one clock event.
 */
@Data
public class DingTalkAttendanceSnapshot {

    private String recordId;

    private String dingUserId;

    private String checkType;

    private String timeResult;

    private String locationResult;

    private LocalDateTime checkTime;

    private String locationName;

    private String locationAddress;

    private String deviceInfo;

    private String rawPayload;

}

