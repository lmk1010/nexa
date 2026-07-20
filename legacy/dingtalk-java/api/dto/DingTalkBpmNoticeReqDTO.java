package com.kyx.service.hr.api.dingtalk.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
@Accessors(chain = true)
public class DingTalkBpmNoticeReqDTO {

    @NotEmpty(message = "接收人不能为空")
    private List<Long> receiverUserIds;

    @NotEmpty(message = "流程实例ID不能为空")
    private String processInstanceId;

    private String processInstanceName;

    private Long startUserId;

    private String startUserNickname;

    private String taskId;

    private String taskName;

    private String activityName;

    private String reason;

    private String detailUrl;

    private String dedupBizId;

}
