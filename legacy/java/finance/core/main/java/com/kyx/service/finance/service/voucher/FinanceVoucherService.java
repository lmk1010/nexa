package com.kyx.service.finance.service.voucher;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.finance.controller.admin.voucher.vo.FinanceVoucherPageReqVO;
import com.kyx.service.finance.controller.admin.voucher.vo.FinanceVoucherSaveReqVO;
import com.kyx.service.finance.controller.admin.voucher.vo.FinanceVoucherUpdateReqVO;
import com.kyx.service.finance.dal.dataobject.voucher.FinanceVoucherDO;
import com.kyx.service.finance.dal.dataobject.voucher.FinanceVoucherDetailDO;

import java.util.List;

/**
 * 凭证 Service
 *
 * @author xyang
 */
public interface FinanceVoucherService {

    /**
     * 新增凭证
     */
    Long createVoucher(FinanceVoucherSaveReqVO reqVO);

    /**
     * 更新凭证
     */
    Boolean updateVoucher(FinanceVoucherUpdateReqVO reqVO);

    /**
     * 删除凭证
     */
    Boolean deleteVoucher(Long id);

    /**
     * 获取凭证详情
     */
    FinanceVoucherDO getVoucher(Long id);

    /**
     * 获取凭证明细
     */
    List<FinanceVoucherDetailDO> getVoucherDetails(Long voucherId);

    /**
     * 分页查询凭证
     */
    PageResult<FinanceVoucherDO> pageVoucher(FinanceVoucherPageReqVO reqVO);

    /**
     * 过账凭证
     */
    Boolean postVoucher(Long id);

    /**
     * 作废凭证
     */
    Boolean voidVoucher(Long id);
}
