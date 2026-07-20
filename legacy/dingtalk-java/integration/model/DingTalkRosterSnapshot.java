package com.kyx.service.hr.integration.dingtalk.model;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DingTalk intelligent HR roster snapshot.
 */
@Data
public class DingTalkRosterSnapshot {

    private String userId;

    // sys02 个人信息
    private String realName;
    private String certNo;
    private String birthTime;
    private String sexType;
    private String nationType;
    private String certAddress;
    private String certEndTime;
    private String residenceType;
    private String address;
    private String politicalStatus;
    private String marriage;
    private String joinWorkingTime;
    private String confirmJoinTime;
    private String personalSi;
    private String personalHf;

    // sys00 基础信息
    private String name;
    private String mobile;
    private String email;
    private String jobNumber;
    private String position;
    private String workPlace;
    private String remark;
    private String reportManager;
    private String reportManagerId;

    // sys01 工作信息
    private String employeeType;
    private String employeeStatus;
    private String probationPeriodType;
    private String regularTime;
    private String planRegularTime;
    private String positionLevel;

    // sys03 学历
    private String highestEdu;
    private String graduateSchool;
    private String graduationTime;
    private String major;

    // sys04 银行卡
    private String bankAccountNo;
    private String accountBank;

    // sys05 合同
    private String contractCompanyName;
    private String contractType;
    private String firstContractStartTime;
    private String firstContractEndTime;
    private String nowContractStartTime;
    private String nowContractEndTime;
    private String contractPeriodType;
    private String contractRenewCount;

    // sys06 紧急联系人
    private String urgentContactsName;
    private String urgentContactsRelation;
    private String urgentContactsPhone;

    // sys07 家庭
    private String haveChild;
    private String childName;
    private String childSex;
    private String childBirthDate;

    private String rawPayload;

    private Map<String, String> fieldValues = new LinkedHashMap<>();

}
