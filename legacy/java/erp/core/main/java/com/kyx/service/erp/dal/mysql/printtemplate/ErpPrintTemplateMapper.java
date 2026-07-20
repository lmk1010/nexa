package com.kyx.service.erp.dal.mysql.printtemplate;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.erp.controller.admin.printtemplate.vo.ErpPrintTemplatePageReqVO;
import com.kyx.service.erp.dal.dataobject.printtemplate.ErpPrintTemplateDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 打印模版 Mapper
 *
 * @author kyx
 */
@Mapper
public interface ErpPrintTemplateMapper extends BaseMapperX<ErpPrintTemplateDO> {

    default PageResult<ErpPrintTemplateDO> selectPage(ErpPrintTemplatePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpPrintTemplateDO>()
                .likeIfPresent(ErpPrintTemplateDO::getName, reqVO.getName())
                .eqIfPresent(ErpPrintTemplateDO::getType, reqVO.getType())
                .eqIfPresent(ErpPrintTemplateDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(ErpPrintTemplateDO::getCreateTime, reqVO.getCreateTime())
                .orderByAsc(ErpPrintTemplateDO::getSort)
                .orderByDesc(ErpPrintTemplateDO::getId));
    }

    default List<ErpPrintTemplateDO> selectListByType(String type) {
        return selectList(ErpPrintTemplateDO::getType, type);
    }

    default List<ErpPrintTemplateDO> selectListByStatus(Integer status) {
        return selectList(ErpPrintTemplateDO::getStatus, status);
    }

    default ErpPrintTemplateDO selectByName(String name) {
        return selectOne(ErpPrintTemplateDO::getName, name);
    }

} 