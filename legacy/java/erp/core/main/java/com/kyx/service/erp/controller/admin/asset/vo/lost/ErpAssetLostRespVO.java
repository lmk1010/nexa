package com.kyx.service.erp.controller.admin.asset.vo.lost;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ERP 资产挂失响应 VO
 *
 * @author kyx
 */
@Data
public class ErpAssetLostRespVO {

    private Long id;

    private String lostNo;

    private Long assetId;

    private String assetNo;

    private String assetName;

    private String assetType;

    private String assetSpecification;

    private String assetBrand;

    private String assetModel;

    private String assetLocation;

    private String lostReason;

    private LocalDate lostDate;

    private LocalDate foundDate;

    private String lostLocation;

    private Long handleUserId;

    private String handleUserName;

    private Long handleDeptId;

    private String handleDeptName;

    private BigDecimal estimatedValue;

    private String lostDescription;

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

    private LocalDateTime foundTime;

    private String findLocation;

    private Long finderUserId;

    private String finderUserName;

    private String findDescription;

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