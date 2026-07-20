package com.kyx.service.finance.dal.mysql.voucher;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.service.finance.dal.dataobject.voucher.FinanceVoucherDetailHistoryDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 凭证明细历史 Mapper
 *
 * @author xyang
 */
@Mapper
public interface FinanceVoucherDetailHistoryMapper extends BaseMapperX<FinanceVoucherDetailHistoryDO> {
}
