package com.kyx.service.hr.controller.admin.integration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Size;
import java.util.List;

@Schema(description = "管理后台 - 钉钉系统更新通知 Request VO")
@Data
public class DingTalkSystemUpdateNoticeReqVO {

    @Schema(description = "卡片标题")
    @Size(max = 80, message = "卡片标题不能超过 80 个字符")
    private String title;

    @Schema(description = "卡片正文")
    @Size(max = 500, message = "卡片正文不能超过 500 个字符")
    private String content;

    @Schema(description = "是否发送给队长")
    private Boolean includeLeaders;

    @Schema(description = "是否发送给技术部")
    private Boolean includeTechDept;

    @Schema(description = "按角色通知的角色 ID 列表")
    @Size(max = 100, message = "通知角色不能超过 100 个")
    private List<Long> roleIds;

    @Schema(description = "按部门通知的部门 ID 列表")
    @Size(max = 200, message = "通知部门不能超过 200 个")
    private List<Long> deptIds;

    @Schema(description = "卡片跳转链接")
    @Size(max = 500, message = "卡片跳转链接不能超过 500 个字符")
    private String detailUrl;

    @Schema(description = "手动选择的 OA 用户 ID 列表")
    @Size(max = 500, message = "手动选择人员不能超过 500 人")
    private List<Long> receiverUserIds;

    @Schema(description = "从自动范围中排除的 OA 用户 ID 列表")
    @Size(max = 500, message = "排除人员不能超过 500 人")
    private List<Long> excludeUserIds;

}
