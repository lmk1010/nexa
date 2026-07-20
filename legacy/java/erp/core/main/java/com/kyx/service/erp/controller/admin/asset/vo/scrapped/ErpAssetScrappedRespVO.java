package com.kyx.service.erp.controller.admin.asset.vo.scrapped;

import com.kyx.foundation.excel.core.annotations.DictFormat;
import com.kyx.foundation.excel.core.convert.DictConvert;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ERP 资产报废响应 VO
 *
 * @author kyx
 */
@Data
public class ErpAssetScrappedRespVO {

    private Long id;

    private String scrappedNo;

    private Long assetId;

    private String assetNo;

    private String assetName;

    private String assetType;

    private String assetSpecification;

    private String assetBrand;

    private String assetModel;

    private String assetLocation;

    private String scrappedReason;

    private String scrappedType;

    private LocalDate scrappedDate;

    private Long handleUserId;

    private String handleUserName;

    private Long handleDeptId;

    private String handleDeptName;

    private BigDecimal estimatedValue;

    private BigDecimal actualValue;

    private String scrappedDescription;

    private Integer status;

    private String statusName;

    private Integer approvalStatus;

    private String approvalStatusName;

    private Long approverUserId;

    private String approverUserName;

    private LocalDateTime approvalTime;

    private String approvalRemark;

    private Integer bmpStatus;

    private String bmpProcessInstanceId;

    private LocalDate processingDate;

    private String processingMethod;

    private BigDecimal disposalRevenue;

    private String remark;

    /**
     * 附件文件列表
     */
    private java.util.List<FileInfo> attachments;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 文件信息
     */
    @Data
    public static class FileInfo {
        private String fileId;
        private String fileName;
        private Long fileSize;
        private String fileType;
    }
} 