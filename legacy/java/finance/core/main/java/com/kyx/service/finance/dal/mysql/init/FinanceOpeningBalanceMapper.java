package com.kyx.service.finance.dal.mysql.init;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.finance.controller.admin.init.vo.opening.FinanceOpeningBalanceListReqVO;
import com.kyx.service.finance.controller.admin.init.vo.opening.FinanceOpeningBalancePageReqVO;
import com.kyx.service.finance.dal.dataobject.init.FinanceOpeningBalanceDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;

/**
 * 期初余额 Mapper
 */
@Mapper
public interface FinanceOpeningBalanceMapper extends BaseMapperX<FinanceOpeningBalanceDO> {

    int upsertBatch(@Param("list") List<FinanceOpeningBalanceDO> list);

    int updateLockedByCompanyAndPeriod(@Param("companyId") Long companyId,
                                       @Param("period") String period,
                                       @Param("locked") Boolean locked);

    default PageResult<FinanceOpeningBalanceDO> selectPage(FinanceOpeningBalancePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<FinanceOpeningBalanceDO>()
                .eqIfPresent(FinanceOpeningBalanceDO::getCompanyId, reqVO.getCompanyId())
                .eqIfPresent(FinanceOpeningBalanceDO::getPeriod, reqVO.getPeriod())
                .likeIfPresent(FinanceOpeningBalanceDO::getSubjectCode, StringUtils.trimWhitespace(reqVO.getSubjectCode()))
                .likeIfPresent(FinanceOpeningBalanceDO::getSubjectName, StringUtils.trimWhitespace(reqVO.getSubjectName()))
                .eqIfPresent(FinanceOpeningBalanceDO::getStatus, reqVO.getStatus())
                .eqIfPresent(FinanceOpeningBalanceDO::getLocked, reqVO.getLocked())
                .orderByDesc(FinanceOpeningBalanceDO::getId)
                .orderByDesc(BaseDO::getCreateTime)
        );
    }

    default List<FinanceOpeningBalanceDO> selectListByReq(FinanceOpeningBalanceListReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<FinanceOpeningBalanceDO>()
                .eqIfPresent(FinanceOpeningBalanceDO::getCompanyId, reqVO.getCompanyId())
                .eqIfPresent(FinanceOpeningBalanceDO::getPeriod, reqVO.getPeriod())
                .likeIfPresent(FinanceOpeningBalanceDO::getSubjectCode, StringUtils.trimWhitespace(reqVO.getSubjectCode()))
                .likeIfPresent(FinanceOpeningBalanceDO::getSubjectName, StringUtils.trimWhitespace(reqVO.getSubjectName()))
                .eqIfPresent(FinanceOpeningBalanceDO::getStatus, reqVO.getStatus())
                .eqIfPresent(FinanceOpeningBalanceDO::getLocked, reqVO.getLocked())
                .orderByAsc(FinanceOpeningBalanceDO::getSubjectCode)
                .orderByDesc(BaseDO::getCreateTime)
        );
    }

    default FinanceOpeningBalanceDO selectByUniqueKey(Long companyId, String period, String subjectCode) {
        return selectOne(new LambdaQueryWrapperX<FinanceOpeningBalanceDO>()
                .eq(FinanceOpeningBalanceDO::getCompanyId, companyId)
                .eq(FinanceOpeningBalanceDO::getPeriod, period)
                .eq(FinanceOpeningBalanceDO::getSubjectCode, subjectCode)
                .last("LIMIT 1"));
    }

    default List<FinanceOpeningBalanceDO> selectListByCompanyAndPeriodAndSubjectCodes(Long companyId,
                                                                                       String period,
                                                                                       Collection<String> subjectCodes) {
        if (subjectCodes == null || subjectCodes.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<FinanceOpeningBalanceDO>()
                .eq(FinanceOpeningBalanceDO::getCompanyId, companyId)
                .eq(FinanceOpeningBalanceDO::getPeriod, period)
                .in(FinanceOpeningBalanceDO::getSubjectCode, subjectCodes));
    }

    default boolean existsLockedByCompanyAndPeriod(Long companyId, String period) {
        return selectCount(new LambdaQueryWrapperX<FinanceOpeningBalanceDO>()
                .eq(FinanceOpeningBalanceDO::getCompanyId, companyId)
                .eq(FinanceOpeningBalanceDO::getPeriod, period)
                .eq(FinanceOpeningBalanceDO::getLocked, Boolean.TRUE)) > 0;
    }

    default List<FinanceOpeningBalanceDO> selectListByCompanyAndPeriod(Long companyId, String period) {
        return selectList(new LambdaQueryWrapperX<FinanceOpeningBalanceDO>()
                .eq(FinanceOpeningBalanceDO::getCompanyId, companyId)
                .eq(FinanceOpeningBalanceDO::getPeriod, period)
                .orderByAsc(FinanceOpeningBalanceDO::getSubjectCode));
    }

}
