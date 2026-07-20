package com.kyx.service.biz.enums;

import com.kyx.foundation.common.exception.ErrorCode;

/**
 * Business module error codes.
 *
 * Range: 1-040-000-000.
 */
public interface ErrorCodeConstants {

    // ===== Todo 1-040-000-000 =====
    ErrorCode TODO_NOT_EXISTS = new ErrorCode(1_040_000_000, "Todo does not exist");
    ErrorCode TODO_FORBIDDEN = new ErrorCode(1_040_000_001, "No permission to access this todo");

    // ===== Work Requirement 1-040-001-000 =====
    ErrorCode WORK_REQUIREMENT_NOT_EXISTS = new ErrorCode(1_040_001_000, "Requirement does not exist");
    ErrorCode WORK_REQUIREMENT_FORBIDDEN = new ErrorCode(1_040_001_001, "No permission to access this requirement");
    ErrorCode WORK_REQUIREMENT_STATUS_INVALID = new ErrorCode(1_040_001_002, "Current status does not allow this action");
    ErrorCode WORK_REQUIREMENT_ASSIGNEE_REQUIRED = new ErrorCode(1_040_001_003, "Assignee is required");
    ErrorCode WORK_REQUIREMENT_REJECT_REASON_REQUIRED = new ErrorCode(1_040_001_004, "Reject reason is required");
    ErrorCode WORK_REQUIREMENT_APPROVAL_PENDING = new ErrorCode(1_040_001_005, "Requirement is not approved yet");
    ErrorCode WORK_REQUIREMENT_APPROVAL_ALREADY_APPROVED = new ErrorCode(1_040_001_006, "Requirement is already approved");
    ErrorCode WORK_REQUIREMENT_ACCEPT_ATTACHMENT_REQUIRED = new ErrorCode(1_040_001_007, "请填写验收说明或上传验收截图");
    ErrorCode WORK_REQUIREMENT_COMMENT_CONTENT_REQUIRED = new ErrorCode(1_040_001_008, "请填写评论内容或上传附件");
    ErrorCode WORK_REQUIREMENT_PARENT_STATUS_INVALID = new ErrorCode(1_040_001_009, "父需求当前状态不允许新增子需求");
    ErrorCode WORK_REQUIREMENT_TREE_DEPTH_EXCEEDED = new ErrorCode(1_040_001_010, "子需求不能再创建子需求");
    ErrorCode WORK_REQUIREMENT_HAS_CHILDREN = new ErrorCode(1_040_001_011, "当前需求存在子需求，不能直接删除");
    ErrorCode WORK_REQUIREMENT_HAS_OPEN_CHILDREN = new ErrorCode(1_040_001_012, "当前需求存在未完成子需求，不能验收通过");

    // ===== Hotel Work Order 1-040-003-000 =====
    ErrorCode HOTEL_WORK_ORDER_FORBIDDEN = new ErrorCode(1_040_003_000, "暂无酒店前台权限");
    ErrorCode HOTEL_WORK_ORDER_NOT_EXISTS = new ErrorCode(1_040_003_001, "酒店工单不存在");

    // ===== Work Calendar 1-040-002-000 =====
    ErrorCode WORK_CALENDAR_EVENT_NOT_EXISTS = new ErrorCode(1_040_002_000, "Calendar event does not exist");
    ErrorCode WORK_CALENDAR_EVENT_FORBIDDEN = new ErrorCode(1_040_002_001, "No permission to access this calendar event");
    ErrorCode WORK_CALENDAR_EVENT_TIME_INVALID = new ErrorCode(1_040_002_002, "End time must be later than start time");

}
