package com.kyx.service.finance.service.receivable;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.finance.controller.admin.receivable.vo.FinanceReceivablePayablePageReqVO;
import com.kyx.service.finance.controller.admin.receivable.vo.FinanceReceivablePayableSaveReqVO;
import com.kyx.service.finance.controller.admin.receivable.vo.FinanceReceivablePayableUpdateReqVO;
import com.kyx.service.finance.controller.admin.receivable.vo.FinanceReceivablePayableWriteOffPageReqVO;
import com.kyx.service.finance.controller.admin.receivable.vo.FinanceReceivablePayableWriteOffSaveReqVO;
import com.kyx.service.finance.dal.dataobject.receivable.FinanceReceivablePayableDetailDO;
import com.kyx.service.finance.dal.dataobject.receivable.FinanceReceivablePayableDO;

/**
 * 往来账 Service
 */
public interface FinanceReceivablePayableService {

    /**
     * 新增往来账
     */
    Long createReceivablePayable(FinanceReceivablePayableSaveReqVO reqVO);

    /**
     * 更新往来账
     */
    Boolean updateReceivablePayable(FinanceReceivablePayableUpdateReqVO reqVO);

    /**
     * 删除往来账
     */
    Boolean deleteReceivablePayable(Long id);

    /**
     * 获取往来账详情
     */
    FinanceReceivablePayableDO getReceivablePayable(Long id);

    /**
     * 分页查询往来账
     */
    PageResult<FinanceReceivablePayableDO> pageReceivablePayable(FinanceReceivablePayablePageReqVO reqVO);

    /**
     * 新增核销记录
     */
    Long createWriteOff(FinanceReceivablePayableWriteOffSaveReqVO reqVO);

    /**
     * 分页查询核销记录
     */
    PageResult<FinanceReceivablePayableDetailDO> pageWriteOff(FinanceReceivablePayableWriteOffPageReqVO reqVO);
}
