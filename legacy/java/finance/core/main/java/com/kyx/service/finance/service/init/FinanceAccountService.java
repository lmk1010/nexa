package com.kyx.service.finance.service.init;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.finance.controller.admin.init.vo.account.FinanceAccountPageReqVO;
import com.kyx.service.finance.controller.admin.init.vo.account.FinanceAccountSaveReqVO;
import com.kyx.service.finance.dal.dataobject.init.FinanceAccountDO;
import com.kyx.service.finance.dal.dataobject.init.FinanceAccountOptionDO;

import java.util.Collection;
import java.util.List;

/**
 * 账户服务接口
 *
 * @author xyang
 */
public interface FinanceAccountService {

    /**
     * 创建账户
     *
     * @param reqVO 账户保存请求
     * @return 账户ID
     */
    Long createAccount(FinanceAccountSaveReqVO reqVO);

    /**
     * 更新账户
     *
     * @param reqVO 账户保存请求
     * @return 是否成功
     */
    Boolean updateAccount(FinanceAccountSaveReqVO reqVO);

    /**
     * 删除账户
     *
     * @param id 账户ID
     * @return 是否成功
     */
    Boolean deleteAccount(Long id);

    Boolean updateAccountStatus(Long id, Integer status);

    /**
     * 获取账户详情
     *
     * @param id 账户 ID
     * @return 账户响应
     */
    FinanceAccountDO getAccount(Long id);

    /**
     * 根据 ID 列表查询账户列表
     *
     * @param ids 账户 ID
     * @return 账户列表
     */
    List<FinanceAccountDO> getAccountByIds(Collection<Long> ids);

    /**
     * 分页查询账户
     *
     * @param reqVO 分页查询请求
     * @return 分页结果
     */
    PageResult<FinanceAccountDO> pageAccount(FinanceAccountPageReqVO reqVO);

    List<String> getCompanyEntityOptions();

    List<String> getAccountTagOptions();

    List<String> getBankBranchOptions(String keyword);

    List<FinanceAccountOptionDO> getCompanyEntityOptionList(String keyword);

    List<FinanceAccountOptionDO> getBankBranchOptionList(String keyword);

    Long createCompanyEntityOption(String optionValue);

    Boolean updateCompanyEntityOption(Long id, String optionValue);

    Boolean deleteCompanyEntityOption(Long id);

    Long createBankBranchOption(String optionValue);

    Boolean updateBankBranchOption(Long id, String optionValue);

    Boolean deleteBankBranchOption(Long id);

    Boolean batchUpdateAccountStatus(Collection<Long> ids, Integer status);

    Boolean batchDeleteAccount(Collection<Long> ids);
}
