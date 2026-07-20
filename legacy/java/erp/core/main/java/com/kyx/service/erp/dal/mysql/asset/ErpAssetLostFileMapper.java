package com.kyx.service.erp.dal.mysql.asset;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetLostFileDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 资产挂失文件 Mapper
 *
 * @author kyx
 */
@Mapper
public interface ErpAssetLostFileMapper extends BaseMapperX<ErpAssetLostFileDO> {

    default List<ErpAssetLostFileDO> selectByLostId(Long lostId) {
        return selectList(ErpAssetLostFileDO::getLostId, lostId);
    }

    default void deleteByLostId(Long lostId) {
        delete(ErpAssetLostFileDO::getLostId, lostId);
    }
} 