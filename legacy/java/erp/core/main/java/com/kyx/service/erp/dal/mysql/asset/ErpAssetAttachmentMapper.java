package com.kyx.service.erp.dal.mysql.asset;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetAttachmentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 资产附件 Mapper
 *
 * @author kyx
 */
@Mapper
public interface ErpAssetAttachmentMapper extends BaseMapperX<ErpAssetAttachmentDO> {

    /**
     * 根据资产ID查询附件列表
     *
     * @param assetId 资产ID
     * @return 附件列表
     */
    default List<ErpAssetAttachmentDO> selectListByAssetId(Long assetId) {
        return selectList(new LambdaQueryWrapperX<ErpAssetAttachmentDO>()
                .eq(ErpAssetAttachmentDO::getAssetId, assetId)
                .orderByAsc(ErpAssetAttachmentDO::getSort)
                .orderByDesc(ErpAssetAttachmentDO::getCreateTime));
    }

    /**
     * 根据资产ID统计附件数量
     *
     * @param assetId 资产ID
     * @return 附件数量
     */
    default Long selectCountByAssetId(Long assetId) {
        return selectCount(ErpAssetAttachmentDO::getAssetId, assetId);
    }

    /**
     * 根据资产ID删除所有附件
     *
     * @param assetId 资产ID
     * @return 删除数量
     */
    default int deleteByAssetId(Long assetId) {
        return delete(new LambdaQueryWrapperX<ErpAssetAttachmentDO>()
                .eq(ErpAssetAttachmentDO::getAssetId, assetId));
    }

} 