package com.kyx.service.biz.controller.admin.executive.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "Admin - Executive cockpit chat Response VO")
@Data
public class ExecutiveCockpitChatRespVO {

    private String conversationId;
    private String reply;
    private List<String> suggestions;
    private List<ChartHint> chartHints;
    private ExecutiveCockpitOverviewRespVO snapshot;

    @Data
    public static class ChartHint {

        private String type;
        private String title;
        private String dataKey;

    }

}
