package com.kyx.service.erp.dal.dataobject.asset;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ERP 资产转移记录 DO
 *
 * @author kyx
 */
@TableName("erp_asset_transfer")
@KeySequence("erp_asset_transfer_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpAssetTransferDO extends BaseDO {

    /**
     * 转移记录ID
     */
    @TableId
    private Long id;
    /**
     * 转移编号
     */
    private String transferNo;
    /**
     * 资产ID
     */
    private Long assetId;
    /**
     * 转移人用户ID
     */
    private Long fromUserId;
    /**
     * 转移人部门ID
     */
    private Long fromDeptId;
    /**
     * 接收人用户ID
     */
    private Long toUserId;
    /**
     * 接收人部门ID
     */
    private Long toDeptId;
    /**
     * 转移日期
     */
    private LocalDateTime transferDate;
    /**
     * 转移原因
     */
    private String transferReason;
    /**
     * 转移备注
     */
    private String transferRemark;
    /**
     * 转移状态
     *
     * 枚举 {@link com.kyx.service.erp.enums.asset.ErpAssetTransferStatusEnum}
     */
    private Integer status;
    /**
     * 审批人用户ID
     */
    private Long approverUserId;
    /**
     * 审批时间
     */
    private LocalDateTime approvalTime;
    /**
     * 审批状态
     *
     * 枚举 {@link com.kyx.service.erp.enums.asset.ErpAssetTransferApprovalStatusEnum}
     */
    private Integer approvalStatus;
    /**
     * 审批备注
     */
    private String approvalRemark;
    /**
     * BPM流程状态
     *
     * 枚举 {@link com.kyx.service.erp.enums.asset.ErpAssetTransferBmpStatusEnum}
     */
    private Integer bmpStatus;
    /**
     * BPM流程实例ID
     */
    private String processInstanceId;
    /**
     * 接收确认时间
     */
    private LocalDateTime confirmTime;
    /**
     * 接收确认备注
     */
    private String confirmRemark;

} 