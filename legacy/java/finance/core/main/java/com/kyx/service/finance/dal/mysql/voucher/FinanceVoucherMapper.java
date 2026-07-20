package com.kyx.service.finance.dal.mysql.voucher;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.finance.controller.admin.voucher.vo.FinanceVoucherPageReqVO;
import com.kyx.service.finance.dal.dataobject.voucher.FinanceVoucherDO;
import com.kyx.service.finance.enums.FinanceVoucherStatusEnum;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

/**
 * 凭证 Mapper
 *
 * @author xyang
 */
@Mapper
public interface FinanceVoucherMapper extends BaseMapperX<FinanceVoucherDO> {

    default boolean existsByVoucherNo(Long companyId, String voucherNo, Long excludeId) {
        if (!StringUtils.hasText(voucherNo) || companyId == null) {
            return false;
        }
        return selectCount(new LambdaQueryWrapperX<FinanceVoucherDO>()
                .eq(FinanceVoucherDO::getCompanyId, companyId)
                .eq(FinanceVoucherDO::getVoucherNo, voucherNo)
                .neIfPresent(FinanceVoucherDO::getId, excludeId)) > 0;
    }

    default PageResult<FinanceVoucherDO> selectPage(FinanceVoucherPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<FinanceVoucherDO>()
                .eqIfPresent(FinanceVoucherDO::getCompanyId, reqVO.getCompanyId())
                .likeIfPresent(FinanceVoucherDO::getVoucherNo, StringUtils.trimWhitespace(reqVO.getVoucherNo()))
                .eqIfPresent(FinanceVoucherDO::getVoucherType, reqVO.getVoucherType())
                .eqIfPresent(FinanceVoucherDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(FinanceVoucherDO::getVoucherDate, reqVO.getVoucherDate())
                .orderByDesc(FinanceVoucherDO::getVoucherDate)
                .orderByDesc(FinanceVoucherDO::getId)
                .orderByDesc(BaseDO::getCreateTime));
    }

    default boolean existsUnpostedByCompanyIdAndPeriod(Long companyId, String voucherPeriod) {
        return countUnpostedByCompanyIdAndPeriod(companyId, voucherPeriod) > 0;
    }

    default int countUnpostedByCompanyIdAndPeriod(Long companyId, String voucherPeriod) {
        if (companyId == null || !StringUtils.hasText(voucherPeriod)) {
            return 0;
        }
        return Math.toIntExact(selectCount(new LambdaQueryWrapperX<FinanceVoucherDO>()
                .eq(FinanceVoucherDO::getCompanyId, companyId)
                .eq(FinanceVoucherDO::getVoucherPeriod, StringUtils.trimWhitespace(voucherPeriod))
                .in(FinanceVoucherDO::getStatus,
                        FinanceVoucherStatusEnum.DRAFT.name(),
                        FinanceVoucherStatusEnum.APPROVED.name())));
    }
}
