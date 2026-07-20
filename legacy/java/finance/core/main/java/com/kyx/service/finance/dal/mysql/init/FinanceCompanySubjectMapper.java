package com.kyx.service.finance.dal.mysql.init;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.finance.dal.dataobject.init.FinanceCompanySubjectDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 账套科目 Mapper
 *
 * @author xyang
 */
@Mapper
public interface FinanceCompanySubjectMapper extends BaseMapperX<FinanceCompanySubjectDO> {

    // ----------------------------------------------------------------
    // 存在性校验
    // ----------------------------------------------------------------

    default boolean existsByTemplateId(Long templateId) {
        return selectCount(new LambdaQueryWrapperX<FinanceCompanySubjectDO>()
                .eq(FinanceCompanySubjectDO::getTemplateId, templateId)) > 0;
    }

    default boolean existsEnabledByCompanyIdAndSubjectCode(Long companyId, String subjectCode) {
        if (companyId == null || !StringUtils.hasText(subjectCode)) {
            return false;
        }
        return selectCount(new LambdaQueryWrapperX<FinanceCompanySubjectDO>()
                .eq(FinanceCompanySubjectDO::getCompanyId, companyId)
                .eq(FinanceCompanySubjectDO::getSubjectCode, StringUtils.trimWhitespace(subjectCode))
                .eq(FinanceCompanySubjectDO::getStatus, CommonStatusEnum.ENABLE.getStatus())) > 0;
    }

    // ----------------------------------------------------------------
    // 单条查询
    // ----------------------------------------------------------------

    default FinanceCompanySubjectDO selectEnabledByCompanyIdAndSubjectCode(Long companyId, String subjectCode) {
        if (companyId == null || !StringUtils.hasText(subjectCode)) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<FinanceCompanySubjectDO>()
                .eq(FinanceCompanySubjectDO::getCompanyId, companyId)
                .eq(FinanceCompanySubjectDO::getSubjectCode, StringUtils.trimWhitespace(subjectCode))
                .eq(FinanceCompanySubjectDO::getStatus, CommonStatusEnum.ENABLE.getStatus()));
    }

    /**
     * 不限状态查询（用于编码唯一性校验）
     */
    default FinanceCompanySubjectDO selectByCompanyIdAndSubjectCode(Long companyId, String subjectCode) {
        if (companyId == null || !StringUtils.hasText(subjectCode)) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<FinanceCompanySubjectDO>()
                .eq(FinanceCompanySubjectDO::getCompanyId, companyId)
                .eq(FinanceCompanySubjectDO::getSubjectCode, StringUtils.trimWhitespace(subjectCode)));
    }

    // ----------------------------------------------------------------
    // 列表查询
    // ----------------------------------------------------------------

    /**
     * 查询账套下所有科目（含停用），用于树形展示
     */
    default List<FinanceCompanySubjectDO> selectListByCompanyId(Long companyId) {
        return selectList(new LambdaQueryWrapperX<FinanceCompanySubjectDO>()
                .eq(FinanceCompanySubjectDO::getCompanyId, companyId)
                .orderByAsc(FinanceCompanySubjectDO::getLevel,
                        FinanceCompanySubjectDO::getSort,
                        FinanceCompanySubjectDO::getSubjectCode));
    }

    /**
     * 查询账套下所有启用的末级科目（用于凭证/流水选择科目）
     */
    default List<FinanceCompanySubjectDO> selectLeafEnabledByCompanyId(Long companyId) {
        return selectList(new LambdaQueryWrapperX<FinanceCompanySubjectDO>()
                .eq(FinanceCompanySubjectDO::getCompanyId, companyId)
                .eq(FinanceCompanySubjectDO::getIsLeaf, true)
                .eq(FinanceCompanySubjectDO::getStatus, CommonStatusEnum.ENABLE.getStatus())
                .orderByAsc(FinanceCompanySubjectDO::getSubjectCode));
    }

    /**
     * 按科目类型查询账套下启用的末级科目（用于损益结转）
     */
    default List<FinanceCompanySubjectDO> selectLeafEnabledByCompanyIdAndType(Long companyId, String subjectType) {
        return selectList(new LambdaQueryWrapperX<FinanceCompanySubjectDO>()
                .eq(FinanceCompanySubjectDO::getCompanyId, companyId)
                .eq(FinanceCompanySubjectDO::getSubjectType, subjectType)
                .eq(FinanceCompanySubjectDO::getIsLeaf, true)
                .eq(FinanceCompanySubjectDO::getStatus, CommonStatusEnum.ENABLE.getStatus())
                .orderByAsc(FinanceCompanySubjectDO::getSubjectCode));
    }

    // ----------------------------------------------------------------
    // 统计
    // ----------------------------------------------------------------

    /**
     * 统计指定父级下的子科目数量（用于删除校验 & isLeaf 维护）
     */
    default long countByCompanyIdAndParentCode(Long companyId, String parentCode) {
        if (companyId == null || !StringUtils.hasText(parentCode)) {
            return 0L;
        }
        return selectCount(new LambdaQueryWrapper<FinanceCompanySubjectDO>()
                .eq(FinanceCompanySubjectDO::getCompanyId, companyId)
                .eq(FinanceCompanySubjectDO::getParentCode, parentCode));
    }
}
