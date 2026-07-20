package com.kyx.service.finance.service.init;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.finance.controller.admin.init.vo.opening.FinanceOpeningBalanceBatchSaveReqVO;
import com.kyx.service.finance.controller.admin.init.vo.opening.FinanceOpeningBalanceListReqVO;
import com.kyx.service.finance.controller.admin.init.vo.opening.FinanceOpeningBalanceLockReqVO;
import com.kyx.service.finance.controller.admin.init.vo.opening.FinanceOpeningBalancePageReqVO;
import com.kyx.service.finance.controller.admin.init.vo.opening.FinanceOpeningBalanceRollReqVO;
import com.kyx.service.finance.dal.dataobject.init.FinanceOpeningBalanceDO;

import java.util.List;

/**
 * 期初余额服务接口
 */
public interface FinanceOpeningBalanceService {

    /**
     * 批量保存期初余额
     */
    Boolean batchSaveOpeningBalance(FinanceOpeningBalanceBatchSaveReqVO reqVO);

    /**
     * 锁定或解锁期间期初余额
     */
    Boolean lockOpeningBalance(FinanceOpeningBalanceLockReqVO reqVO);

    /**
     * 滚动期初余额到目标期间
     */
    Boolean rollOpeningBalance(FinanceOpeningBalanceRollReqVO reqVO);

    /**
     * 分页查询期初余额
     */
    PageResult<FinanceOpeningBalanceDO> pageOpeningBalance(FinanceOpeningBalancePageReqVO reqVO);

    /**
     * 列表查询期初余额（不分页）
     */
    List<FinanceOpeningBalanceDO> listOpeningBalance(FinanceOpeningBalanceListReqVO reqVO);

}
