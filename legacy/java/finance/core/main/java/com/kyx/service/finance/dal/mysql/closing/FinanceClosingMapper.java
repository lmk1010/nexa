package com.kyx.service.finance.dal.mysql.closing;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.finance.controller.admin.closing.vo.FinanceClosingPageReqVO;
import com.kyx.service.finance.dal.dataobject.closing.FinanceClosingDO;
import com.kyx.service.finance.enums.FinanceClosingStatusEnum;
import org.apache.ibatis.annotations.Mapper;

/**
 * 结账记录 Mapper
 *
 * @author xyang
 */
@Mapper
public interface FinanceClosingMapper extends BaseMapperX<FinanceClosingDO> {

    default PageResult<FinanceClosingDO> selectPage(FinanceClosingPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<FinanceClosingDO>()
                .eqIfPresent(FinanceClosingDO::getCompanyId, reqVO.getCompanyId())
                .eqIfPresent(FinanceClosingDO::getClosingPeriod, reqVO.getClosingPeriod())
                .eqIfPresent(FinanceClosingDO::getStatus, reqVO.getStatus())
                .orderByDesc(FinanceClosingDO::getCloseTime)
                .orderByDesc(FinanceClosingDO::getId)
                .orderByDesc(BaseDO::getCreateTime));
    }

    default FinanceClosingDO selectByCompanyIdAndPeriod(Long companyId, String closingPeriod) {
        if (companyId == null || closingPeriod == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<FinanceClosingDO>()
                .eq(FinanceClosingDO::getCompanyId, companyId)
                .eq(FinanceClosingDO::getClosingPeriod, closingPeriod)
                .orderByDesc(FinanceClosingDO::getId)
                .last("LIMIT 1"));
    }

    default FinanceClosingDO selectLatestSuccessByCompanyId(Long companyId) {
        if (companyId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<FinanceClosingDO>()
                .eq(FinanceClosingDO::getCompanyId, companyId)
                .eq(FinanceClosingDO::getStatus, FinanceClosingStatusEnum.SUCCESS.name())
                .orderByDesc(FinanceClosingDO::getClosingPeriod)
                .orderByDesc(FinanceClosingDO::getId)
                .last("LIMIT 1"));
    }
}
