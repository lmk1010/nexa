package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "管理后台 - 员工总览数据 Response VO")
@Data
public class EmployeeOverviewRespVO {

    @Schema(description = "人员规模统计")
    private Scale scale;

    @Schema(description = "年龄司龄统计")
    private Age age;

    @Schema(description = "学历统计")
    private Education education;

    @Schema(description = "岗位分布统计")
    private java.util.List<PostStat> postStats;

    @Schema(description = "人员规模")
    @Data
    public static class Scale {
        @Schema(description = "总人数")
        private Integer total;
        @Schema(description = "行政岗人数")
        private Integer admin;
        @Schema(description = "前台人数")
        private Integer adminFront;
        @Schema(description = "后勤人数")
        private Integer adminLogistics;
        @Schema(description = "人事岗人数")
        private Integer hr;
        @Schema(description = "招聘人数")
        private Integer hrRecruit;
        @Schema(description = "薪酬人数")
        private Integer hrSalary;
    }

    @Schema(description = "年龄司龄")
    @Data
    public static class Age {
        @Schema(description = "平均年龄")
        private BigDecimal avgAge;
        @Schema(description = "30岁以下占比")
        private Integer under30;
        @Schema(description = "司龄3年及以上人数")
        private Integer over3Years;
        @Schema(description = "稳定性百分比（司龄3年及以上占比）")
        private Integer stability;
    }

    @Schema(description = "学历")
    @Data
    public static class Education {
        @Schema(description = "硕士及以上人数")
        private Integer master;
        @Schema(description = "硕士及以上占比")
        private Integer masterPercent;
        @Schema(description = "本科人数")
        private Integer bachelor;
        @Schema(description = "学历填报人数")
        private Integer related;
    }

    @Schema(description = "岗位分布")
    @Data
    public static class PostStat {
        @Schema(description = "岗位ID")
        private Long postId;
        @Schema(description = "岗位名称")
        private String postName;
        @Schema(description = "人数")
        private Integer total;
    }
}
