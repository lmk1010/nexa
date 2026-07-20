package com.kyx.service.erp.dal.dataobject.asset;

import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * ERP 资产附件 DO
 *
 * @author kyx
 */
@TableName("erp_asset_attachment")
@KeySequence("erp_asset_attachment_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpAssetAttachmentDO extends BaseDO {

    /**
     * 附件编号
     */
    @TableId
    private Long id;
    /**
     * 资产编号
     */
    private Long assetId;
    /**
     * 文件名称
     */
    private String fileName;
    /**
     * 文件路径
     */
    private String filePath;
    /**
     * 文件ID（OP服务返回的UUID，实际存储在filePath字段中）
     */
    private String fileId;
    /**
     * 文件访问URL
     */
    private String fileUrl;
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    /**
     * 文件类型
     */
    private String fileType;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 备注
     */
    private String remark;

} 