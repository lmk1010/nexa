package com.kyx.service.erp.dal.dataobject.asset;

import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * ERP 资产挂失文件关联 DO
 *
 * @author kyx
 */
@TableName("erp_asset_lost_file")
@KeySequence("erp_asset_lost_file_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpAssetLostFileDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    
    /**
     * 挂失记录编号
     */
    private Long lostId;
    
    /**
     * 文件编号(OP服务返回的文件ID)
     */
    private String fileId;
    
    /**
     * 文件名称
     */
    private String fileName;
    
    /**
     * 文件大小
     */
    private Long fileSize;
    
    /**
     * 文件类型
     */
    private String fileType;
} 