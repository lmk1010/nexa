package com.kyx.service.finance.service.closing;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.finance.controller.admin.closing.vo.FinanceClosingExecuteReqVO;
import com.kyx.service.finance.controller.admin.closing.vo.FinanceClosingPageReqVO;
import com.kyx.service.finance.controller.admin.closing.vo.FinanceClosingReverseReqVO;
import com.kyx.service.finance.dal.dataobject.closing.FinanceClosingDO;

/**
 * 月末结账 Service
 *
 * @author xyang
 */
public interface FinanceClosingService {

    /**
     * 分页查询结账记录
     */
    PageResult<FinanceClosingDO> pageClosing(FinanceClosingPageReqVO reqVO);

    /**
     * 执行结账
     */
    Long close(FinanceClosingExecuteReqVO reqVO);

    /**
     * 反结账
     */
    Boolean reverse(FinanceClosingReverseReqVO reqVO);
}
