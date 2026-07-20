package com.kyx.service.hr.api.dingtalk.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class DingTalkRequirementNoticeReqDTO {

    @NotEmpty(message = "接收人不能为空")
    private List<Long> receiverUserIds;

    @NotNull(message = "需求ID不能为空")
    private Long requirementId;

    private String title;

    private String proposerName;

    private String proposerDept;

    private String assigneeName;

    private String expectedFinishDate;

    private String detailUrl;

    private String dedupBizId;

    private String operatorName;

    private String commentTypeLabel;

    private String commentContent;

    private String targetUserName;

}
