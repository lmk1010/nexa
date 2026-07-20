package com.kyx.service.erp.dal.mysql.asset;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetScrappedFileDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 资产报废文件关联 Mapper
 *
 * @author kyx
 */
@Mapper
public interface ErpAssetScrappedFileMapper extends BaseMapperX<ErpAssetScrappedFileDO> {

    default List<ErpAssetScrappedFileDO> selectListByScrappedId(Long scrappedId) {
        return selectList(new LambdaQueryWrapperX<ErpAssetScrappedFileDO>()
                .eq(ErpAssetScrappedFileDO::getScrappedId, scrappedId));
    }

    default int deleteByScrappedId(Long scrappedId) {
        return delete(new LambdaQueryWrapperX<ErpAssetScrappedFileDO>()
                .eq(ErpAssetScrappedFileDO::getScrappedId, scrappedId));
    }
} 