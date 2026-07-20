package com.kyx.service.hr.enums;

import com.kyx.foundation.common.exception.ErrorCode;

/**
 * HR 错误码枚举类
 *
 * HR 错误码区间范围 1-000-000-000 ~ 1-000-999-999
 *
 * @author MK
 */
public interface ErrorCodeConstants {

    // ========== 员工档案相关 1-000-001-000 ==========
    ErrorCode EMPLOYEE_PROFILE_NOT_EXISTS = new ErrorCode(1_000_001_000, "员工档案不存在");
    ErrorCode EMPLOYEE_PROFILE_NO_EXISTS = new ErrorCode(1_000_001_001, "档案编号已存在");
    ErrorCode EMPLOYEE_ID_NUMBER_EXISTS = new ErrorCode(1_000_001_002, "身份证号已存在");
    ErrorCode EMPLOYEE_MOBILE_EXISTS = new ErrorCode(1_000_001_003, "员工手机号已存在");
    ErrorCode EMPLOYEE_EMAIL_EXISTS = new ErrorCode(1_000_001_004, "员工邮箱已存在");
    ErrorCode EMPLOYEE_DEPT_NOT_EXISTS = new ErrorCode(1_000_001_005, "员工部门不存在");
    ErrorCode EMPLOYEE_CUSTOM_FIELD_NOT_EXISTS = new ErrorCode(1_000_001_006, "员工自定义字段不存在");
    ErrorCode EMPLOYEE_CUSTOM_FIELD_KEY_DUPLICATE = new ErrorCode(1_000_001_007, "员工自定义字段编码已存在");
    ErrorCode EMPLOYEE_SAVED_VIEW_NOT_EXISTS = new ErrorCode(1_000_001_008, "员工花名册保存视图不存在");
    ErrorCode EMPLOYEE_MASTER_LOGIN_USER_REQUIRED = new ErrorCode(1_000_001_009, "当前登录用户不存在，无法操作员工主数据");
    ErrorCode EMPLOYEE_CUSTOM_FIELD_TYPE_INVALID = new ErrorCode(1_000_001_010, "员工自定义字段类型不合法");
    ErrorCode EMPLOYEE_CUSTOM_FIELD_KEY_INVALID = new ErrorCode(1_000_001_011, "员工自定义字段编码不合法");
    ErrorCode HR_LIFECYCLE_EVENT_NOT_EXISTS = new ErrorCode(1_000_001_020, "生命周期事件不存在");
    ErrorCode HR_LIFECYCLE_TASK_NOT_EXISTS = new ErrorCode(1_000_001_021, "生命周期任务不存在");
    ErrorCode HR_LIFECYCLE_REQUIRED_TASK_OPEN = new ErrorCode(1_000_001_022, "存在未完成的必办检查项，无法生效");
    ErrorCode HR_LIFECYCLE_RESIGNATION_PENDING_EXISTS = new ErrorCode(1_000_001_023, "该员工已有办理中的离职事件");
    ErrorCode HR_LIFECYCLE_EVENT_TYPE_UNSUPPORTED = new ErrorCode(1_000_001_024, "当前生命周期事件暂不支持该操作");
    ErrorCode HR_LIFECYCLE_EVENT_PENDING_EXISTS = new ErrorCode(1_000_001_025, "该员工已有办理中的同类生命周期事件");
    ErrorCode HR_LIFECYCLE_APPROVAL_NOT_PASSED = new ErrorCode(1_000_001_026, "生命周期审批尚未通过，无法生效");
    ErrorCode RECRUITMENT_PUBLIC_LINK_NOT_EXISTS = new ErrorCode(1_000_001_030, "招聘投递链接不存在");
    ErrorCode RECRUITMENT_PUBLIC_LINK_EXPIRED = new ErrorCode(1_000_001_031, "招聘投递链接已过期");
    ErrorCode RECRUITMENT_PUBLIC_LINK_DISABLED = new ErrorCode(1_000_001_032, "招聘投递链接已禁用");
    ErrorCode RECRUITMENT_PUBLIC_LINK_MAX_SUBMIT = new ErrorCode(1_000_001_033, "招聘投递名额已满");

    // ========== 员工入职记录相关 1-000-001-100 ==========
    ErrorCode EMPLOYEE_ENTRY_NOT_EXISTS = new ErrorCode(1_000_001_100, "入职记录不存在");
    ErrorCode EMPLOYEE_ENTRY_NO_EXISTS = new ErrorCode(1_000_001_101, "入职编号已存在");
    ErrorCode EMPLOYEE_NO_EXISTS = new ErrorCode(1_000_001_102, "员工编号已存在");
    ErrorCode EMPLOYEE_ALREADY_RESIGNED = new ErrorCode(1_000_001_103, "员工已离职，无法重复办理");

    // ========== 员工成长记录相关 1-000-001-200 ==========
    ErrorCode EMPLOYEE_GROWTH_LOG_NOT_EXISTS = new ErrorCode(1_000_001_200, "员工成长记录不存在");

    // ========== 员工积分相关 1-000-001-300 ==========
    ErrorCode EMPLOYEE_POINTS_ACCOUNT_NOT_EXISTS = new ErrorCode(1_000_001_300, "员工积分账户不存在");
    ErrorCode EMPLOYEE_POINTS_BALANCE_NOT_ENOUGH = new ErrorCode(1_000_001_301, "员工积分余额不足");

    // ========== 入职管理相关 1-000-002-000 ==========
    ErrorCode ONBOARDING_NOT_EXISTS = new ErrorCode(1_000_002_000, "入职申请不存在");
    ErrorCode ONBOARDING_APPLICATION_NO_EXISTS = new ErrorCode(1_000_002_001, "申请编号已存在");
    ErrorCode ONBOARDING_ID_CARD_EXISTS = new ErrorCode(1_000_002_002, "身份证号已存在");
    ErrorCode ONBOARDING_STATUS_NOT_DRAFT = new ErrorCode(1_000_002_003, "入职申请状态不是待提交");
    ErrorCode ONBOARDING_STATUS_NOT_APPROVING = new ErrorCode(1_000_002_004, "入职申请状态不是审批中");
    ErrorCode ONBOARDING_STATUS_CANNOT_CANCEL = new ErrorCode(1_000_002_005, "入职申请状态不允许取消");
    ErrorCode ONBOARDING_STATUS_CANNOT_RESTORE = new ErrorCode(1_000_002_006, "入职申请状态不允许恢复");
    ErrorCode ONBOARDING_LINK_GENERATE_FAILED = new ErrorCode(1_000_002_007, "入职链接生成失败");
    ErrorCode ONBOARDING_CREATE_FAILED = new ErrorCode(1_000_002_008, "入职申请创建失败");

    // ========== 序列管理相关 1-000-003-000 ==========
    ErrorCode SEQUENCE_NOT_EXISTS = new ErrorCode(1_000_003_000, "序列不存在");
    ErrorCode SEQUENCE_NAME_DUPLICATE = new ErrorCode(1_000_003_001, "序列名称已存在");
    ErrorCode SEQUENCE_EXISTS_CHILDREN = new ErrorCode(1_000_003_002, "序列存在子序列，无法删除");
    ErrorCode SEQUENCE_PARENT_ERROR = new ErrorCode(1_000_003_003, "上级序列设置错误");

    // ========== 职级管理相关 1-000-004-000 ==========
    ErrorCode JOB_LEVEL_NOT_EXISTS = new ErrorCode(1_000_004_000, "职级不存在");
    ErrorCode JOB_LEVEL_CODE_DUPLICATE = new ErrorCode(1_000_004_001, "职级编码已存在");
    ErrorCode JOB_LEVEL_NAME_DUPLICATE = new ErrorCode(1_000_004_002, "职级名称已存在");

    // ========== 地点管理相关 1-000-005-000 ==========
    ErrorCode LOCATION_NOT_EXISTS = new ErrorCode(1_000_005_000, "地点不存在");
    ErrorCode LOCATION_NAME_DUPLICATE = new ErrorCode(1_000_005_001, "地点名称已存在");

    // ========== 问卷管理相关 1-000-007-000 ==========
    ErrorCode QUESTIONNAIRE_NOT_EXISTS = new ErrorCode(1_000_007_000, "问卷不存在");
    ErrorCode QUESTIONNAIRE_CODE_DUPLICATE = new ErrorCode(1_000_007_001, "问卷编码已存在");
    ErrorCode QUESTIONNAIRE_PUBLISH_NOT_EXISTS = new ErrorCode(1_000_007_010, "问卷发布不存在");
    ErrorCode QUESTIONNAIRE_ASSIGNMENT_NOT_EXISTS = new ErrorCode(1_000_007_020, "问卷分配不存在");
    ErrorCode QUESTIONNAIRE_IMPORT_FAIL = new ErrorCode(1_000_007_030, "问卷导入失败");
    ErrorCode QUESTIONNAIRE_PUBLIC_LINK_NOT_EXISTS = new ErrorCode(1_000_007_040, "问卷公开链接不存在");
    ErrorCode QUESTIONNAIRE_PUBLIC_LINK_EXPIRED = new ErrorCode(1_000_007_041, "问卷链接已过期");
    ErrorCode QUESTIONNAIRE_PUBLIC_LINK_DISABLED = new ErrorCode(1_000_007_042, "问卷链接已禁用");
    ErrorCode QUESTIONNAIRE_PUBLIC_LINK_MAX_SUBMIT = new ErrorCode(1_000_007_043, "问卷提交数已达上限");
    ErrorCode QUESTIONNAIRE_PUBLIC_LINK_PASSWORD_ERROR = new ErrorCode(1_000_007_044, "问卷访问密码错误");
    ErrorCode QUESTIONNAIRE_ASSIGNMENT_FORBIDDEN = new ErrorCode(1_000_007_045, "无权限提交该问卷");
    ErrorCode QUESTIONNAIRE_PUBLISH_ENDED = new ErrorCode(1_000_007_046, "问卷已截止，无法提交");
    ErrorCode QUESTIONNAIRE_PUBLIC_RESPONDENT_REQUIRED = new ErrorCode(1_000_007_047, "请填写姓名、手机和邮箱");
    ErrorCode QUESTIONNAIRE_ASSIGNMENT_ALREADY_SUBMITTED = new ErrorCode(1_000_007_048, "问卷已完成，不能重复提交");
    ErrorCode QUESTIONNAIRE_STANDARD_ANSWER_REQUIRED = new ErrorCode(1_000_007_049, "单选题必须且仅能设置1个标准答案，多选题至少设置1个标准答案");
    ErrorCode QUESTIONNAIRE_PUBLISH_BATCH_CANNOT_ENABLE = new ErrorCode(1_000_007_050, "该问卷批次当前不允许启用");

    // ========== 考试管理相关 1-000-008-000 ==========
    ErrorCode EXAM_NOT_EXISTS = new ErrorCode(1_000_008_000, "考试不存在");
    ErrorCode EXAM_CODE_DUPLICATE = new ErrorCode(1_000_008_001, "考试编码已存在");
    ErrorCode EXAM_IMPORT_FAIL = new ErrorCode(1_000_008_010, "考试导入失败");
    ErrorCode EXAM_ATTEMPT_NOT_EXISTS = new ErrorCode(1_000_008_020, "考试作答不存在");
    ErrorCode EXAM_ATTEMPT_FORBIDDEN = new ErrorCode(1_000_008_021, "无权限提交考试");
    ErrorCode EXAM_ATTEMPT_LIMIT = new ErrorCode(1_000_008_022, "超过最大考试次数");
    ErrorCode EXAM_PAPER_NOT_CONFIGURED = new ErrorCode(1_000_008_023, "考试未配置试卷");
    ErrorCode EXAM_NOT_IN_SCOPE = new ErrorCode(1_000_008_024, "你不在此次考试人员范围内");
    ErrorCode EXAM_NOT_STARTED = new ErrorCode(1_000_008_030, "考试未开始");
    ErrorCode EXAM_ENDED = new ErrorCode(1_000_008_031, "考试已结束");
    ErrorCode EXAM_PUBLISH_NOT_EXISTS = new ErrorCode(1_000_008_040, "考试发布不存在");
    ErrorCode EXAM_PUBLISH_CANNOT_PAUSE = new ErrorCode(1_000_008_041, "只有进行中的定期考核才能暂停");
    ErrorCode EXAM_PUBLISH_CANNOT_RESUME = new ErrorCode(1_000_008_042, "只有已暂停的定期考核才能恢复");
    ErrorCode EXAM_PUBLISH_ALREADY_EXISTS = new ErrorCode(1_000_008_043, "当前考试已存在发布配置，请直接编辑");

    // ========== 打卡管理相关 1-000-009-000 ==========
    ErrorCode ATTENDANCE_EMPLOYEE_NOT_BOUND = new ErrorCode(1_000_009_000, "当前用户未绑定员工档案");
    ErrorCode ATTENDANCE_CLOCK_TYPE_INVALID = new ErrorCode(1_000_009_001, "打卡类型不合法");
    ErrorCode ATTENDANCE_CLOCK_ALREADY_EXISTS = new ErrorCode(1_000_009_002, "今日该类型打卡已存在");

    // ========== 请假管理相关 1-000-006-000 ==========
    ErrorCode HR_LEAVE_NOT_EXISTS = new ErrorCode(1_000_006_000, "请假申请不存在");
    ErrorCode HR_TRIP_NOT_EXISTS = new ErrorCode(1_000_006_010, "出差申请不存在");
    ErrorCode HR_MEETING_NOT_EXISTS = new ErrorCode(1_000_006_020, "会议预约不存在");
    ErrorCode HR_MEETING_ROOM_NOT_EXISTS = new ErrorCode(1_000_006_030, "会议室不存在");
    ErrorCode HR_MEETING_ROOM_CODE_DUPLICATE = new ErrorCode(1_000_006_031, "会议室编号已存在");

}
