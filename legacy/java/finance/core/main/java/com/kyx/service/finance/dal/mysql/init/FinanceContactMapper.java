package com.kyx.service.finance.dal.mysql.init;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.finance.controller.admin.init.vo.contact.FinanceContactPageReqVO;
import com.kyx.service.finance.dal.dataobject.init.FinanceContactDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;

/**
 * 往来信息 Mapper
 *
 * @author xyang
 */
@Mapper
public interface FinanceContactMapper extends BaseMapperX<FinanceContactDO> {

    default PageResult<FinanceContactDO> selectPage(FinanceContactPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<FinanceContactDO>()
                .likeIfPresent(FinanceContactDO::getContactName, StringUtils.trimWhitespace(reqVO.getContactName()))
                .inIfPresent(FinanceContactDO::getGroupId, reqVO.getGroupIds())
                .eqIfPresent(FinanceContactDO::getStatus, reqVO.getStatus())
                .orderByDesc(BaseDO::getCreateTime, FinanceContactDO::getId));
    }

    default int updateStatusByIds(Collection<Long> ids, Integer status) {
        return update(null, new LambdaUpdateWrapper<FinanceContactDO>()
                .in(FinanceContactDO::getId, ids)
                .set(FinanceContactDO::getStatus, status));
    }

    default boolean existsByGroupId(Long groupId) {
        return selectCount(new LambdaQueryWrapperX<FinanceContactDO>()
                .eq(FinanceContactDO::getGroupId, groupId)) > 0;
    }

    default boolean existsByGroupIds(List<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return false;
        }
        return selectCount(new LambdaQueryWrapperX<FinanceContactDO>()
                .in(FinanceContactDO::getGroupId, groupIds)) > 0;
    }
}
