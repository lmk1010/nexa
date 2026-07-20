package com.kyx.service.finance.service.init.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.collection.CollectionUtils;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.service.finance.controller.admin.init.vo.subject.FinanceSubjectTemplatePageReqVO;
import com.kyx.service.finance.controller.admin.init.vo.subject.FinanceSubjectTemplateSaveReqVO;
import com.kyx.service.finance.controller.admin.init.vo.subject.FinanceSubjectTemplateTreeNodeVO;
import com.kyx.service.finance.dal.dataobject.init.FinanceSubjectTemplateDO;
import com.kyx.service.finance.dal.mysql.init.FinanceCompanySubjectMapper;
import com.kyx.service.finance.dal.mysql.init.FinanceSubjectTemplateMapper;
import com.kyx.service.finance.service.init.FinanceSubjectTemplateService;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.service.impl.DiffParseFunction;
import com.mzt.logapi.starter.annotation.LogRecord;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.finance.enums.ErrorCodeConstants.*;
import static com.kyx.service.finance.enums.LogRecordConstants.*;

/**
 * 科目模板服务实现
 *
 * @author Trae AI
 */
@Service
public class FinanceSubjectTemplateServiceImpl implements FinanceSubjectTemplateService {

    @Resource
    private FinanceSubjectTemplateMapper financeSubjectTemplateMapper;
    @Resource
    private FinanceCompanySubjectMapper financeCompanySubjectMapper;

    private static final String ROOT_SUBJECT_CODE = "0";

    @Override
    @LogRecord(type = FINANCE_SUBJECT_TEMPLATE_TYPE,
            subType = FINANCE_SUBJECT_TEMPLATE_CREATE_SUB_TYPE,
            bizNo = "{{#_ret}}",
            success = FINANCE_SUBJECT_TEMPLATE_CREATE_SUCCESS
    )
    public Long createSubjectTemplate(FinanceSubjectTemplateSaveReqVO reqVO) {
        Long customTenantId = resolveTemplateOwnerId(reqVO.getCustomTenantId());
        reqVO.setCustomTenantId(customTenantId);
        // 校验科目编码是否存在
        if (financeSubjectTemplateMapper.checkSubjectCodeExist(reqVO.getAccountingSystem(), customTenantId, reqVO.getSubjectCode())) {
            throw exception(SUBJECT_TEMPLATE_CODE_EXISTS);
        }
        if (reqVO.getParentCode() == null) {
            reqVO.setParentCode(ROOT_SUBJECT_CODE);
        }
        if (!ROOT_SUBJECT_CODE.equals(reqVO.getParentCode())
                && !financeSubjectTemplateMapper.checkSubjectCodeExist(reqVO.getAccountingSystem(), customTenantId, reqVO.getParentCode())) {
            throw exception(SUBJECT_TEMPLATE_PARENT_CODE_NOT_EXISTS);
        }

        FinanceSubjectTemplateDO subjectTemplateDO = BeanUtils.toBean(reqVO, FinanceSubjectTemplateDO.class);
        financeSubjectTemplateMapper.insert(subjectTemplateDO);
        return subjectTemplateDO.getId();
    }

    @Override
    @LogRecord(type = FINANCE_SUBJECT_TEMPLATE_TYPE,
            subType = FINANCE_SUBJECT_TEMPLATE_UPDATE_SUB_TYPE,
            bizNo = "{{#reqVO.id}}",
            success = FINANCE_SUBJECT_TEMPLATE_UPDATE_SUCCESS,
            extra = "{{#reqVO.toString()}}"
    )
    public Boolean updateSubjectTemplate(FinanceSubjectTemplateSaveReqVO reqVO) {
        FinanceSubjectTemplateDO old = financeSubjectTemplateMapper.selectById(reqVO.getId());
        if (old == null) {
            throw exception(SUBJECT_TEMPLATE_NOT_EXISTS);
        }
        FinanceSubjectTemplateDO subjectTemplateDO = new FinanceSubjectTemplateDO();
        subjectTemplateDO.setId(reqVO.getId());
        subjectTemplateDO.setSubjectName(reqVO.getSubjectName());
        subjectTemplateDO.setSort(reqVO.getSort());
        subjectTemplateDO.setRemark(reqVO.getRemark());
        subjectTemplateDO.setFeeType(reqVO.getFeeType());
        subjectTemplateDO.setManageSwitch(reqVO.getManageSwitch());
        subjectTemplateDO.setBizType(reqVO.getBizType());
        subjectTemplateDO.setStatus(reqVO.getStatus());
        subjectTemplateDO.setFeeType(reqVO.getFeeType());
        subjectTemplateDO.setBizType(reqVO.getBizType());
        subjectTemplateDO.setManageSwitch(reqVO.getManageSwitch());

        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, old);
        return financeSubjectTemplateMapper.updateById(subjectTemplateDO) > 0;
    }

    @Override
    @LogRecord(type = FINANCE_SUBJECT_TEMPLATE_TYPE,
            subType = FINANCE_SUBJECT_TEMPLATE_DELETE_SUB_TYPE,
            bizNo = "{{#id}}",
            success = FINANCE_SUBJECT_TEMPLATE_DELETE_SUCCESS
    )
    public Boolean deleteSubjectTemplate(Long id) {
        FinanceSubjectTemplateDO subjectTemplateDO = financeSubjectTemplateMapper.selectById(id);
        if (subjectTemplateDO == null) {
            throw exception(SUBJECT_TEMPLATE_NOT_EXISTS);
        }
        if (financeCompanySubjectMapper.existsByTemplateId(id)) {
            throw exception(SUBJECT_TEMPLATE_USED);
        }
        long count = financeSubjectTemplateMapper.countByParentCode(subjectTemplateDO.getAccountingSystem(),
                subjectTemplateDO.getCustomTenantId(), subjectTemplateDO.getSubjectCode());
        if (count > 0) {
            throw exception(SUBJECT_TEMPLATE_HAS_CHILDREN);
        }
        return financeSubjectTemplateMapper.deleteById(id) > 0;
    }

    @Override
    public FinanceSubjectTemplateDO getSubjectTemplate(Long id) {
        return financeSubjectTemplateMapper.selectById(id);
    }

    @Override
    public PageResult<FinanceSubjectTemplateDO> pageSubjectTemplate(FinanceSubjectTemplatePageReqVO reqVO) {
        // 实现树形结构排序：首先按父级的sort排序，然后按本级的sort排序
        // 对于顶级节点(parent_code=0)，直接使用自己的sort
        // 对于子节点，先按父级的sort排序，再按自己的sort排序
        return financeSubjectTemplateMapper.selectPage(reqVO, getQueryWrapper(reqVO));
    }

    @Override
    public List<FinanceSubjectTemplateDO> listSubjectTemplate(FinanceSubjectTemplatePageReqVO reqVO) {
        return financeSubjectTemplateMapper.selectList(getQueryWrapper(reqVO));
    }

    @Override
    public List<FinanceSubjectTemplateTreeNodeVO> treeSubjectTemplateTree(FinanceSubjectTemplatePageReqVO reqVO) {
        List<FinanceSubjectTemplateDO> list = listSubjectTemplate(reqVO);
        if (CollectionUtils.isAnyEmpty(list)) {
            return Collections.emptyList();
        }
        Map<String, List<FinanceSubjectTemplateTreeNodeVO>> nodes = list.stream()
                .map(t -> BeanUtils.toBean(t, FinanceSubjectTemplateTreeNodeVO.class))
                .collect(Collectors.groupingBy(t -> StringUtils.isBlank(t.getParentCode()) ? ROOT_SUBJECT_CODE : t.getParentCode()));

        return CollectionUtils.buildTree(nodes, FinanceSubjectTemplateTreeNodeVO::getSubjectCode, ROOT_SUBJECT_CODE,
                Comparator.comparingInt(FinanceSubjectTemplateTreeNodeVO::getSort).thenComparing(FinanceSubjectTemplateTreeNodeVO::getSubjectCode));
    }


    private static LambdaQueryWrapper<FinanceSubjectTemplateDO> getQueryWrapper(FinanceSubjectTemplatePageReqVO reqVO) {
        LambdaQueryWrapperX<FinanceSubjectTemplateDO> wrapper = new LambdaQueryWrapperX<FinanceSubjectTemplateDO>()
                .eqIfPresent(FinanceSubjectTemplateDO::getAccountingSystem, reqVO.getAccountingSystem())
                .likeIfPresent(FinanceSubjectTemplateDO::getSubjectCode, reqVO.getSubjectCode())
                .likeIfPresent(FinanceSubjectTemplateDO::getSubjectName, reqVO.getSubjectName())
                .eqIfPresent(FinanceSubjectTemplateDO::getSubjectType, reqVO.getSubjectType())
                .eqIfPresent(FinanceSubjectTemplateDO::getStatus, reqVO.getStatus());
        if (reqVO.getCustomTenantId() != null) {
            wrapper.eq(FinanceSubjectTemplateDO::getCustomTenantId, reqVO.getCustomTenantId());
        } else {
            wrapper.in(FinanceSubjectTemplateDO::getCustomTenantId, 0L, TenantContextHolder.getRequiredTenantId());
        }
        return wrapper.orderByAsc(FinanceSubjectTemplateDO::getLevel, FinanceSubjectTemplateDO::getSort,
                FinanceSubjectTemplateDO::getParentCode, FinanceSubjectTemplateDO::getSubjectCode);
    }

    private static Long resolveTemplateOwnerId(Long customTenantId) {
        return customTenantId == null ? 0L : customTenantId;
    }
}
