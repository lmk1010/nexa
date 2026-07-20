package com.kyx.service.biz.controller.admin.work.vo;

import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.common.pojo.PageParam;
import com.kyx.foundation.common.validation.InEnum;
import com.kyx.service.biz.enums.WorkRequirementPriorityEnum;
import com.kyx.service.biz.enums.WorkRequirementStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Schema(description = "Admin - Work Requirement page Request VO")
@Data
public class WorkRequirementPageReqVO extends PageParam {

    @Schema(description = "Keyword")
    private String keyword;

    @Schema(description = "Status", example = "2")
    @InEnum(WorkRequirementStatusEnum.class)
    private Integer status;

    @Schema(description = "Priority", example = "2")
    @InEnum(WorkRequirementPriorityEnum.class)
    private Integer priority;

    @Schema(description = "Approval status", example = "1")
    private Integer approvalStatus;

    @Schema(description = "Comma separated process instance ids")
    private String processInstanceIds;

    @Schema(description = "Parent requirement ID", example = "100")
    private Long parentId;

    @Schema(description = "Root requirement ID", example = "100")
    private Long rootId;

    @Schema(description = "Only root requirements")
    private Boolean onlyRoot;

    @Schema(description = "When onlyRoot=true, include roots whose children match filters")
    private Boolean includeChildMatches;

    @Schema(description = "Related user ID, matches proposer or assignee", example = "1")
    private Long userId;

    @Schema(description = "Proposer user ID", example = "1")
    private Long proposerUserId;

    @Schema(description = "Proposer name", example = "张三")
    private String proposerName;

    @Schema(description = "Assignee user ID", example = "2")
    private Long assigneeUserId;

    @Schema(description = "Assignee name", example = "李四")
    private String assigneeName;

    @Schema(description = "Only requirements without assigned developer")
    private Boolean assigneeUnassignedOnly;

    @Schema(description = "Only requirements with unread comments")
    private Boolean commentUnreadOnly;

    @Schema(description = "Sort field")
    private String sortField;

    @Schema(description = "Sort order: asc or desc")
    private String sortOrder;

    // 时间范围以逗号分隔字符串传入（`start,end`），Spring `LocalDateTime[]`
    // 多值绑定跟前端 axios repeat 序列化不匹配，改用 String + 手动 parse 保险。
    @Schema(description = "Create time range start,end (yyyy-MM-dd HH:mm:ss)")
    private String createTime;

    @Schema(description = "Update time range start,end (yyyy-MM-dd HH:mm:ss)")
    private String updateTime;

    private static final DateTimeFormatter TIME_RANGE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Schema(hidden = true)
    public LocalDateTime[] getCreateTimeRange() {
        return parseTimeRange(createTime);
    }

    @Schema(hidden = true)
    public LocalDateTime[] getUpdateTimeRange() {
        return parseTimeRange(updateTime);
    }

    private static LocalDateTime[] parseTimeRange(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        String[] parts = raw.split(",", 2);
        if (parts.length != 2) {
            return null;
        }
        try {
            LocalDateTime start = LocalDateTime.parse(parts[0].trim(), TIME_RANGE_FMT);
            LocalDateTime end = LocalDateTime.parse(parts[1].trim(), TIME_RANGE_FMT);
            return new LocalDateTime[]{start, end};
        } catch (Exception ignored) {
            return null;
        }
    }

    @Schema(hidden = true)
    private Long unreadCommentUserId;

    @Schema(hidden = true)
    public List<String> getProcessInstanceIdList() {
        if (StrUtil.isBlank(processInstanceIds)) {
            return Collections.emptyList();
        }
        return StrUtil.splitTrim(processInstanceIds, ',');
    }

    @Schema(hidden = true)
    public String getTrimmedKeyword() {
        String value = StrUtil.trim(keyword);
        return StrUtil.isBlank(value) ? null : value;
    }

    @Schema(hidden = true)
    public Long getKeywordId() {
        String value = getTrimmedKeyword();
        if (StrUtil.isBlank(value) || !value.matches("\\d{1,18}")) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

}
