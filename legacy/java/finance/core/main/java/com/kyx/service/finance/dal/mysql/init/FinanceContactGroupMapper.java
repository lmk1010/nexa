package com.kyx.service.finance.dal.mysql.init;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.finance.dal.dataobject.init.FinanceContactGroupDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 往来分组 Mapper
 */
@Mapper
public interface FinanceContactGroupMapper extends BaseMapperX<FinanceContactGroupDO> {

    default List<FinanceContactGroupDO> selectAll() {
        return selectList(new LambdaQueryWrapperX<FinanceContactGroupDO>()
                .orderByAsc(FinanceContactGroupDO::getLevel, FinanceContactGroupDO::getSort, FinanceContactGroupDO::getId));
    }

    default FinanceContactGroupDO selectByParentIdAndName(Long parentId, String groupName) {
        if (parentId == null || !StringUtils.hasText(groupName)) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<FinanceContactGroupDO>()
                .eq(FinanceContactGroupDO::getParentId, parentId)
                .eq(FinanceContactGroupDO::getGroupName, StringUtils.trimWhitespace(groupName))
                .last("LIMIT 1"));
    }

    default boolean existsByParentIdAndName(Long parentId, String groupName, Long excludeId) {
        if (parentId == null || !StringUtils.hasText(groupName)) {
            return false;
        }
        return selectCount(new LambdaQueryWrapperX<FinanceContactGroupDO>()
                .eq(FinanceContactGroupDO::getParentId, parentId)
                .eq(FinanceContactGroupDO::getGroupName, StringUtils.trimWhitespace(groupName))
                .neIfPresent(FinanceContactGroupDO::getId, excludeId)) > 0;
    }

    default boolean existsByParentId(Long parentId) {
        return selectCount(new LambdaQueryWrapperX<FinanceContactGroupDO>()
                .eq(FinanceContactGroupDO::getParentId, parentId)) > 0;
    }

    default Integer selectMaxSortByParentId(Long parentId) {
        FinanceContactGroupDO groupDO = selectOne(new LambdaQueryWrapperX<FinanceContactGroupDO>()
                .eq(FinanceContactGroupDO::getParentId, parentId)
                .orderByDesc(FinanceContactGroupDO::getSort)
                .last("LIMIT 1"));
        return groupDO == null ? null : groupDO.getSort();
    }
}
