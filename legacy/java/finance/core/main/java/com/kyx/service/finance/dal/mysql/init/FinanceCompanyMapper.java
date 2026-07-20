package com.kyx.service.finance.dal.mysql.init;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.finance.controller.admin.init.vo.company.FinanceCompanyPageReqVO;
import com.kyx.service.finance.dal.dataobject.init.FinanceCompanyDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 账套表 Mapper
 *
 * @author Trae AI
 */
@Mapper
public interface FinanceCompanyMapper extends BaseMapperX<FinanceCompanyDO> {
    default PageResult<FinanceCompanyDO> selectPage(FinanceCompanyPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<FinanceCompanyDO>()
                .likeIfPresent(FinanceCompanyDO::getCompanyName, reqVO.getCompanyName())
                .eqIfPresent(FinanceCompanyDO::getCompanyCode, reqVO.getCompanyCode())
                .eqIfPresent(FinanceCompanyDO::getStatus, reqVO.getStatus())
                .orderByDesc(FinanceCompanyDO::getId)
                .orderByDesc(BaseDO::getCreateTime)
        );
    }

    default boolean existsByCompanyCode(String companyCode) {
        return selectCount(new LambdaQueryWrapperX<FinanceCompanyDO>()
                .eq(FinanceCompanyDO::getCompanyCode, companyCode)) > 0;
    }
}
