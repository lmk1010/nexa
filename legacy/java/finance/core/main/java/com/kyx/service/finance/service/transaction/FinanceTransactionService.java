package com.kyx.service.finance.service.transaction;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.finance.controller.admin.transaction.vo.FinanceTransactionSaveReqVO;
import com.kyx.service.finance.controller.admin.transaction.vo.FinanceTransactionPageReqVO;
import com.kyx.service.finance.controller.admin.transaction.vo.FinanceTransactionUpdateReqVO;
import com.kyx.service.finance.dal.dataobject.transaction.FinanceTransactionDO;

/**
 * 资金流水 Service
 *
 * @author xyang
 */
public interface FinanceTransactionService {

    /**
     * 新增流水
     *
     * @param reqVO 新增请求
     * @return 流水 ID
     */
    Long createTransaction(FinanceTransactionSaveReqVO reqVO);

    /**
     * 更新流水
     *
     * @param reqVO 更新请求
     * @return 是否成功
     */
    Boolean updateTransaction(FinanceTransactionUpdateReqVO reqVO);

    /**
     * 删除流水
     *
     * @param id 流水ID
     * @return 是否成功
     */
    Boolean deleteTransaction(Long id);

    /**
     * 作废流水
     *
     * @param id 流水ID
     * @return 是否成功
     */
    Boolean reverseTransaction(Long id);

    /**
     * 获取流水详情
     *
     * @param id 流水ID
     * @return 流水详情
     */
    FinanceTransactionDO getTransaction(Long id);

    /**
     * 分页查询流水
     *
     * @param reqVO 分页请求
     * @return 分页结果
     */
    PageResult<FinanceTransactionDO> pageTransaction(FinanceTransactionPageReqVO reqVO);

}
