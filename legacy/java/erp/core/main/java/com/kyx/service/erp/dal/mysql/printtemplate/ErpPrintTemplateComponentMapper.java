package com.kyx.service.erp.dal.mysql.printtemplate;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.service.erp.dal.dataobject.printtemplate.ErpPrintTemplateComponentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 打印模版组件 Mapper
 *
 * @author kyx
 */
@Mapper
public interface ErpPrintTemplateComponentMapper extends BaseMapperX<ErpPrintTemplateComponentDO> {

    default List<ErpPrintTemplateComponentDO> selectListByTemplateId(Long templateId) {
        return selectList(ErpPrintTemplateComponentDO::getTemplateId, templateId);
    }

    default List<ErpPrintTemplateComponentDO> selectListByTemplateIds(List<Long> templateIds) {
        return selectList(ErpPrintTemplateComponentDO::getTemplateId, templateIds);
    }

    default void deleteByTemplateId(Long templateId) {
        delete(ErpPrintTemplateComponentDO::getTemplateId, templateId);
    }

} 