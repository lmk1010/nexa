package com.kyx.service.erp.dal.mysql.product;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.erp.controller.admin.product.vo.category.ErpProductCategoryListReqVO;
import com.kyx.service.erp.dal.dataobject.product.ErpProductCategoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 产品分类 Mapper
 *
 * @author MK
 */
@Mapper
public interface ErpProductCategoryMapper extends BaseMapperX<ErpProductCategoryDO> {

    default List<ErpProductCategoryDO> selectList(ErpProductCategoryListReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<ErpProductCategoryDO>()
                .likeIfPresent(ErpProductCategoryDO::getName, reqVO.getName())
                .eqIfPresent(ErpProductCategoryDO::getStatus, reqVO.getStatus())
                .orderByDesc(ErpProductCategoryDO::getId));
    }

	default ErpProductCategoryDO selectByParentIdAndName(Long parentId, String name) {
	    return selectOne(ErpProductCategoryDO::getParentId, parentId, ErpProductCategoryDO::getName, name);
	}

    default Long selectCountByParentId(Long parentId) {
        return selectCount(ErpProductCategoryDO::getParentId, parentId);
    }

}