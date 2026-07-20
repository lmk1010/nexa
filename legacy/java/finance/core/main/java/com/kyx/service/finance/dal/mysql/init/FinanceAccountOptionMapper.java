package com.kyx.service.finance.dal.mysql.init;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.finance.dal.dataobject.init.FinanceAccountOptionDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 账户选项 Mapper
 */
@Mapper
public interface FinanceAccountOptionMapper extends BaseMapperX<FinanceAccountOptionDO> {

    default List<FinanceAccountOptionDO> selectOptions(String optionType, String keyword) {
        return selectList(new LambdaQueryWrapperX<FinanceAccountOptionDO>()
                .eq(FinanceAccountOptionDO::getOptionType, optionType)
                .likeIfPresent(FinanceAccountOptionDO::getOptionValue, StringUtils.trimWhitespace(keyword))
                .eq(FinanceAccountOptionDO::getStatus, 0)
                .orderByAsc(FinanceAccountOptionDO::getSort)
                .orderByAsc(FinanceAccountOptionDO::getOptionValue));
    }

    default List<String> selectOptionValues(String optionType, String keyword) {
        return selectOptions(optionType, keyword)
                .stream()
                .map(FinanceAccountOptionDO::getOptionValue)
                .collect(Collectors.toList());
    }

    default boolean existsByTypeAndValue(String optionType, String optionValue) {
        return selectCount(new LambdaQueryWrapperX<FinanceAccountOptionDO>()
                .eq(FinanceAccountOptionDO::getOptionType, optionType)
                .eq(FinanceAccountOptionDO::getOptionValue, optionValue)) > 0;
    }

    default boolean existsByTypeAndValue(String optionType, String optionValue, Long excludeId) {
        return selectCount(new LambdaQueryWrapperX<FinanceAccountOptionDO>()
                .eq(FinanceAccountOptionDO::getOptionType, optionType)
                .eq(FinanceAccountOptionDO::getOptionValue, optionValue)
                .neIfPresent(FinanceAccountOptionDO::getId, excludeId)) > 0;
    }
}
