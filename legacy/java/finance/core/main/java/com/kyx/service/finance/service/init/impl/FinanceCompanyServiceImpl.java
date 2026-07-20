package com.kyx.service.finance.service.init.impl;

import cn.hutool.core.date.DatePattern;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.finance.controller.admin.init.vo.company.FinanceCompanyPageReqVO;
import com.kyx.service.finance.controller.admin.init.vo.company.FinanceCompanySaveReqVO;
import com.kyx.service.finance.controller.admin.init.vo.company.FinanceCompanySubjectRespVO;
import com.kyx.service.finance.dal.dataobject.init.FinanceCompanyDO;
import com.kyx.service.finance.dal.dataobject.init.FinanceCompanySubjectDO;
import com.kyx.service.finance.dal.dataobject.init.FinanceSubjectTemplateDO;
import com.kyx.service.finance.dal.mysql.init.FinanceCompanyMapper;
import com.kyx.service.finance.dal.mysql.init.FinanceCompanySubjectMapper;
import com.kyx.service.finance.dal.mysql.init.FinanceSubjectTemplateMapper;
import com.kyx.service.finance.service.init.FinanceCompanyService;
import com.kyx.service.finance.service.init.FinanceCompanySubjectService;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.service.impl.DiffParseFunction;
import com.mzt.logapi.starter.annotation.LogRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.finance.enums.ErrorCodeConstants.*;
import static com.kyx.service.finance.enums.LogRecordConstants.*;

/**
 * 璐﹀鏈嶅姟瀹炵幇绫?
 *
 * @author xyang
 */
@Service
@Validated
public class FinanceCompanyServiceImpl implements FinanceCompanyService {

    private static final String DEFAULT_CURRENCY = "CNY";
    private static final String ROOT_SUBJECT_CODE = "0";

    @Resource
    private FinanceCompanyMapper financeCompanyMapper;
    @Resource
    private FinanceSubjectTemplateMapper financeSubjectTemplateMapper;
    @Resource
    private FinanceCompanySubjectMapper financeCompanySubjectMapper;
    @Resource
    private FinanceCompanySubjectService financeCompanySubjectService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = FINANCE_COMPANY_TEMPLATE_TYPE,
            subType = FINANCE_COMPANY_TEMPLATE_CREATE_SUB_TYPE,
            bizNo = "{{#_ret}}",
            success = FINANCE_COMPANY_TEMPLATE_CREATE_SUCCESS)
    public Long createCompany(FinanceCompanySaveReqVO reqVO) {
        if (financeCompanyMapper.existsByCompanyCode(reqVO.getCompanyCode())) {
            throw exception(COMPANY_CODE_EXISTS);
        }

        FinanceCompanyDO company = BeanUtils.toBean(reqVO, FinanceCompanyDO.class);
        String startPeriod = reqVO.getStartPeriod();
        if (startPeriod == null || startPeriod.trim().isEmpty()) {
            startPeriod = LocalDate.now().format(DatePattern.SIMPLE_MONTH_FORMATTER);
        }
        company.setStartPeriod(startPeriod);
        company.setCurrentClosePeriod(startPeriod);
        if (company.getCurrency() == null || company.getCurrency().trim().isEmpty()) {
            company.setCurrency(DEFAULT_CURRENCY);
        }

        financeCompanyMapper.insert(company);
        // 浠庢ā鏉垮鍏ュ叕鍙哥鐩?
        List<FinanceSubjectTemplateDO> templatesList = financeSubjectTemplateMapper.selectTenantListBySystemCode(reqVO.getAccountingSystem());
        if (templatesList.isEmpty()) {
            throw exception(SUBJECT_TEMPLATE_NOT_EXISTS);
        }
        Set<String> parentCodes = templatesList.stream()
                .map(FinanceSubjectTemplateDO::getParentCode)
                .collect(Collectors.toSet());
        List<FinanceCompanySubjectDO> companySubjects = templatesList.stream().map(t -> {
            FinanceCompanySubjectDO companySubject = BeanUtils.toBean(t, FinanceCompanySubjectDO.class);
            companySubject.setCompanyId(company.getId());
            companySubject.setIsLeaf(!parentCodes.contains(t.getSubjectCode()));
            companySubject.setTemplateId(t.getId());
            companySubject.setOpeningBalance(BigDecimal.ZERO);
            companySubject.setId(null);
            return companySubject;
        }).collect(Collectors.toList());
        financeCompanySubjectMapper.insertBatch(companySubjects);

        return company.getId();
    }

    @Override
    @LogRecord(type = FINANCE_COMPANY_TEMPLATE_TYPE,
            subType = FINANCE_COMPANY_TEMPLATE_UPDATE_SUB_TYPE,
            bizNo = "{{#reqVo.id}}",
            success = FINANCE_COMPANY_TEMPLATE_UPDATE_SUCCESS)
    public Boolean updateCompany(FinanceCompanySaveReqVO reqVO) {
        FinanceCompanyDO old = financeCompanyMapper.selectById(reqVO.getId());
        if (old == null) {
            throw exception(COMPANY_NOT_EXISTS);
        }
        FinanceCompanyDO companyDO = new FinanceCompanyDO();
        companyDO.setId(reqVO.getId());
        companyDO.setCompanyName(reqVO.getCompanyName());
        companyDO.setDescription(reqVO.getDescription());
        companyDO.setStatus(reqVO.getStatus());

        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, old);
        return SqlHelper.retBool(financeCompanyMapper.updateById(companyDO));
    }

    @Override
    public Boolean deleteCompany(List<Long> ids) {
        for (Long id : ids) {
            FinanceCompanyDO companyDO = financeCompanyMapper.selectById(id);
            if (companyDO == null) {
                return false;
            }
            if (CommonStatusEnum.isEnable(companyDO.getStatus())) {
                throw exception(STATUS_ENABLE_ERROR);
            }
            financeCompanyMapper.deleteById(id);
        }
        return true;
    }

    @Override
    public FinanceCompanyDO getCompany(Long id) {
        return financeCompanyMapper.selectById(id);
    }

    @Override
    public PageResult<FinanceCompanyDO> pageCompany(FinanceCompanyPageReqVO reqVO) {
        return financeCompanyMapper.selectPage(reqVO);
    }

    @Override
    public List<FinanceCompanySubjectRespVO> listCompanySubjectTree(Long companyId) {
        return financeCompanySubjectService.listCompanySubjectTree(companyId);
    }
}
