package com.kyx.service.erp.dal.dataobject.asset;

import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * ERP 资产报废文件关联 DO
 *
 * @author kyx
 */
@TableName("erp_asset_scrapped_file")
@KeySequence("erp_asset_scrapped_file_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpAssetScrappedFileDO extends BaseDO {

    /**
     * 主键编号
     */
    @TableId
    private Long id;
    
    /**
     * 资产报废ID
     */
    private Long scrappedId;
    
    /**
     * OP服务文件ID
     */
    private String fileId;
    
    /**
     * 文件名称
     */
    private String fileName;
    
    /**
     * 文件大小(字节)
     */
    private Long fileSize;
    
    /**
     * 文件类型
     */
    private String fileType;
} 