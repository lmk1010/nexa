package com.kyx.service.erp.dal.dataobject.asset;

import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * ERP 资产条码打印日志 DO
 *
 * @author kyx
 */
@TableName("erp_asset_barcode_print_log")
@KeySequence("erp_asset_barcode_print_log_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpAssetBarcodePrintLogDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 条码打印记录编号
     */
    private Long barcodePrintId;
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
     * 操作类型：1-生成条码，2-打印条码，3-重新打印，4-作废条码
     */
    private Integer operationType;
    /**
     * 本次打印数量
     */
    private Integer printCount;
    /**
     * 累计打印次数
     */
    private Integer totalPrintCount;
    /**
     * 打印机名称
     */
    private String printerName;
    /**
     * 打印用户编号
     */
    private Long printUserId;
    /**
     * 打印用户姓名
     */
    private String printUserName;
    /**
     * 打印结果：1-成功，2-失败
     */
    private Integer printResult;
    /**
     * 错误信息
     */
    private String errorMessage;
    /**
     * 操作IP地址
     */
    private String operationIp;
    /**
     * 用户代理
     */
    private String userAgent;
    /**
     * 备注
     */
    private String remark;

} 