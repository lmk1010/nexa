package com.kyx.service.finance.dal.mysql.init;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.service.finance.dal.dataobject.init.FinanceSubjectTemplateDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 科目表 Mapper
 *
 * @author xyang
 */
@Mapper
public interface FinanceSubjectTemplateMapper extends BaseMapperX<FinanceSubjectTemplateDO> {

    /**
     * 根据会计制度编码查询租户下已启用的科目模板列表
     * @param accountingSystem 会计制度编码
     * @return 租户下已启用的科目模板列表
     */
    default List<FinanceSubjectTemplateDO> selectTenantListBySystemCode(String accountingSystem) {
        LambdaQueryWrapper<FinanceSubjectTemplateDO> eq = new LambdaQueryWrapper<FinanceSubjectTemplateDO>()
                        .eq(FinanceSubjectTemplateDO::getAccountingSystem, accountingSystem)
                        .eq(FinanceSubjectTemplateDO::getStatus, CommonStatusEnum.ENABLE.getStatus())
                        .in(FinanceSubjectTemplateDO::getCustomTenantId, 0L, TenantContextHolder.getRequiredTenantId())
                        .orderByAsc(FinanceSubjectTemplateDO::getLevel, FinanceSubjectTemplateDO::getSort, FinanceSubjectTemplateDO::getSubjectCode);
        return selectList(eq);
    }

    default boolean checkSubjectCodeExist(String accountingSystem, String subjectCode) {
        return checkSubjectCodeExist(accountingSystem, 0L, subjectCode);
    }

    default boolean checkSubjectCodeExist(String accountingSystem, Long customTenantId, String subjectCode) {
        if (!StringUtils.hasText(accountingSystem) || !StringUtils.hasText(subjectCode)) {
            return false;
        }
        return selectCount(new LambdaQueryWrapper<FinanceSubjectTemplateDO>()
                        .eq(FinanceSubjectTemplateDO::getAccountingSystem, accountingSystem)
                        .eq(FinanceSubjectTemplateDO::getCustomTenantId, customTenantId == null ? 0L : customTenantId)
                        .eq(FinanceSubjectTemplateDO::getSubjectCode, subjectCode)
        ) > 0;
    }

    default long countByParentCode(String accountingSystem, Long customTenantId, String parentCode) {
        if (!StringUtils.hasText(accountingSystem) || !StringUtils.hasText(parentCode)) {
            return 0L;
        }
        return selectCount(new LambdaQueryWrapper<FinanceSubjectTemplateDO>()
                .eq(FinanceSubjectTemplateDO::getAccountingSystem, accountingSystem)
                .eq(FinanceSubjectTemplateDO::getCustomTenantId, customTenantId == null ? 0L : customTenantId)
                .eq(FinanceSubjectTemplateDO::getParentCode, parentCode));
    }
}
