package com.kyx.service.erp.dal.dataobject.asset;

import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ERP 资产条码打印 DO
 *
 * @author kyx
 */
@TableName("erp_asset_barcode_print")
@KeySequence("erp_asset_barcode_print_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpAssetBarcodePrintDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 资产编号
     */
    private Long assetId;
    /**
     * 资产编码
     */
    private String assetNo;
    /**
     * 条码编号
     */
    private String barcodeNo;
    /**
     * 发放序号
     */
    private String printSerialNo;
    /**
     * 发放日期
     */
    private LocalDateTime issueDate;
    /**
     * 条码类型：1-一维码，2-二维码
     */
    private Integer barcodeType;
    /**
     * 条码内容/数据
     */
    private String barcodeContent;
    /**
     * 打印次数
     */
    private Integer printCount;
    /**
     * 最后打印时间
     */
    private LocalDateTime lastPrintTime;
    /**
     * 状态：1-正常，2-失效，3-已作废
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

} 