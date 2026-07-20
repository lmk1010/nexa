package com.kyx.service.erp.service.printtemplate;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.erp.controller.admin.printtemplate.vo.*;
import com.kyx.service.erp.dal.dataobject.printtemplate.ErpPrintTemplateDO;
import com.kyx.service.erp.dal.dataobject.printtemplate.ErpPrintTemplateComponentDO;
import com.kyx.service.erp.dal.mysql.printtemplate.ErpPrintTemplateMapper;
import com.kyx.service.erp.dal.mysql.printtemplate.ErpPrintTemplateComponentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.foundation.common.util.collection.CollectionUtils.convertMap;
import static com.kyx.service.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 打印模版 Service 实现类
 *
 * @author kyx
 */
@Service
@Validated
public class ErpPrintTemplateServiceImpl implements ErpPrintTemplateService {

    @Resource
    private ErpPrintTemplateMapper printTemplateMapper;
    
    @Resource
    private ErpPrintTemplateComponentMapper printTemplateComponentMapper;

    @Override
    @Transactional
    public Long createPrintTemplate(ErpPrintTemplateSaveReqVO createReqVO) {
        // 校验模版名称唯一性
        validateTemplateNameUnique(null, createReqVO.getName());
        
        // 插入模版
        ErpPrintTemplateDO template = BeanUtils.toBean(createReqVO, ErpPrintTemplateDO.class);
        if (template.getSort() == null) {
            template.setSort(0);
        }
        printTemplateMapper.insert(template);
        
        // 插入组件
        if (CollUtil.isNotEmpty(createReqVO.getComponents())) {
            createTemplateComponents(template.getId(), createReqVO.getComponents());
        }
        
        return template.getId();
    }

    @Override
    @Transactional
    public void updatePrintTemplate(ErpPrintTemplateSaveReqVO updateReqVO) {
        // 校验存在
        validatePrintTemplateExists(updateReqVO.getId());
        
        // 校验模版名称唯一性
        validateTemplateNameUnique(updateReqVO.getId(), updateReqVO.getName());
        
        // 更新模版
        ErpPrintTemplateDO updateObj = BeanUtils.toBean(updateReqVO, ErpPrintTemplateDO.class);
        printTemplateMapper.updateById(updateObj);
        
        // 更新组件：先删除再新增
        printTemplateComponentMapper.deleteByTemplateId(updateReqVO.getId());
        if (CollUtil.isNotEmpty(updateReqVO.getComponents())) {
            createTemplateComponents(updateReqVO.getId(), updateReqVO.getComponents());
        }
    }

    @Override
    @Transactional
    public void deletePrintTemplate(Long id) {
        // 校验存在
        validatePrintTemplateExists(id);
        
        // 删除模版
        printTemplateMapper.deleteById(id);
        
        // 删除组件
        printTemplateComponentMapper.deleteByTemplateId(id);
    }

    @Override
    public ErpPrintTemplateDO getPrintTemplate(Long id) {
        return printTemplateMapper.selectById(id);
    }

    @Override
    public List<ErpPrintTemplateDO> getPrintTemplateList(Collection<Long> ids) {
        return printTemplateMapper.selectBatchIds(ids);
    }

    @Override
    public PageResult<ErpPrintTemplateRespVO> getPrintTemplatePage(ErpPrintTemplatePageReqVO pageReqVO) {
        return getPrintTemplateVOPage(pageReqVO);
    }

    @Override
    public PageResult<ErpPrintTemplateRespVO> getPrintTemplateVOPage(ErpPrintTemplatePageReqVO pageReqVO) {
        PageResult<ErpPrintTemplateDO> pageResult = printTemplateMapper.selectPage(pageReqVO);
        if (pageResult.getList().isEmpty()) {
            return PageResult.empty(pageResult.getTotal());
        }
        
        // 查询组件信息
        List<Long> templateIds = pageResult.getList().stream()
                .map(ErpPrintTemplateDO::getId)
                .collect(Collectors.toList());
        List<ErpPrintTemplateComponentDO> components = printTemplateComponentMapper.selectListByTemplateIds(templateIds);
        Map<Long, List<ErpPrintTemplateComponentDO>> componentMap = components.stream()
                .collect(Collectors.groupingBy(ErpPrintTemplateComponentDO::getTemplateId));
        
        // 转换结果
        List<ErpPrintTemplateRespVO> respVOList = pageResult.getList().stream()
                .map(template -> {
                    ErpPrintTemplateRespVO respVO = BeanUtils.toBean(template, ErpPrintTemplateRespVO.class);
                    List<ErpPrintTemplateComponentDO> templateComponents = componentMap.get(template.getId());
                    if (CollUtil.isNotEmpty(templateComponents)) {
                        respVO.setComponents(BeanUtils.toBean(templateComponents, ErpPrintTemplateComponentRespVO.class));
                        respVO.setComponentCount(templateComponents.size());
                    } else {
                        respVO.setComponentCount(0);
                    }
                    return respVO;
                })
                .collect(Collectors.toList());
        
        return new PageResult<>(respVOList, pageResult.getTotal());
    }

    @Override
    public List<ErpPrintTemplateRespVO> getEnabledPrintTemplateList(String type) {
        List<ErpPrintTemplateDO> templates = printTemplateMapper.selectListByStatus(1); // 启用状态
        if (StrUtil.isNotBlank(type)) {
            templates = templates.stream()
                    .filter(template -> Objects.equals(template.getType(), type))
                    .collect(Collectors.toList());
        }
        
        return BeanUtils.toBean(templates, ErpPrintTemplateRespVO.class);
    }

    @Override
    @Transactional
    public Long copyPrintTemplate(Long id, String name) {
        // 获取原模版
        ErpPrintTemplateDO originalTemplate = validatePrintTemplateExists(id);
        
        // 校验新名称唯一性
        validateTemplateNameUnique(null, name);
        
        // 复制模版
        ErpPrintTemplateDO newTemplate = BeanUtils.toBean(originalTemplate, ErpPrintTemplateDO.class);
        newTemplate.setId(null);
        newTemplate.setName(name);
        newTemplate.setCreateTime(null);
        newTemplate.setUpdateTime(null);
        printTemplateMapper.insert(newTemplate);
        
        // 复制组件
        List<ErpPrintTemplateComponentDO> originalComponents = printTemplateComponentMapper.selectListByTemplateId(id);
        if (CollUtil.isNotEmpty(originalComponents)) {
            List<ErpPrintTemplateComponentDO> newComponents = originalComponents.stream()
                    .map(component -> {
                        ErpPrintTemplateComponentDO newComponent = BeanUtils.toBean(component, ErpPrintTemplateComponentDO.class);
                        newComponent.setId(null);
                        newComponent.setTemplateId(newTemplate.getId());
                        newComponent.setCreateTime(null);
                        newComponent.setUpdateTime(null);
                        return newComponent;
                    })
                    .collect(Collectors.toList());
            
            newComponents.forEach(printTemplateComponentMapper::insert);
        }
        
        return newTemplate.getId();
    }

    @Override
    public void updatePrintTemplateStatus(Long id, Integer status) {
        // 校验存在
        validatePrintTemplateExists(id);
        
        // 更新状态
        ErpPrintTemplateDO updateObj = new ErpPrintTemplateDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        printTemplateMapper.updateById(updateObj);
    }

    private void validateTemplateNameUnique(Long id, String name) {
        ErpPrintTemplateDO template = printTemplateMapper.selectByName(name);
        if (template == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的模版
        if (id == null) {
            throw exception(PRINT_TEMPLATE_NAME_DUPLICATE);
        }
        if (!template.getId().equals(id)) {
            throw exception(PRINT_TEMPLATE_NAME_DUPLICATE);
        }
    }

    private ErpPrintTemplateDO validatePrintTemplateExists(Long id) {
        if (id == null) {
            return null;
        }
        ErpPrintTemplateDO template = printTemplateMapper.selectById(id);
        if (template == null) {
            throw exception(PRINT_TEMPLATE_NOT_EXISTS);
        }
        return template;
    }

    private void createTemplateComponents(Long templateId, List<ErpPrintTemplateComponentSaveReqVO> componentReqVOs) {
        List<ErpPrintTemplateComponentDO> components = componentReqVOs.stream()
                .map(reqVO -> {
                    ErpPrintTemplateComponentDO component = BeanUtils.toBean(reqVO, ErpPrintTemplateComponentDO.class);
                    component.setTemplateId(templateId);
                    if (component.getSort() == null) {
                        component.setSort(0);
                    }
                    return component;
                })
                .collect(Collectors.toList());
        
        components.forEach(printTemplateComponentMapper::insert);
    }

} 