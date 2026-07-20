package com.kyx.service.erp.service.finance;

import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.erp.controller.admin.finance.vo.account.ErpAccountPageReqVO;
import com.kyx.service.erp.controller.admin.finance.vo.account.ErpAccountSaveReqVO;
import com.kyx.service.erp.dal.dataobject.finance.ErpAccountDO;
import com.kyx.service.erp.dal.mysql.finance.ErpAccountMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.erp.enums.ErrorCodeConstants.ACCOUNT_NOT_ENABLE;
import static com.kyx.service.erp.enums.ErrorCodeConstants.ACCOUNT_NOT_EXISTS;

/**
 * ERP 结算账户 Service 实现类
 *
 * @author MK
 */
@Service
@Validated
public class ErpAccountServiceImpl implements ErpAccountService {

    @Resource
    private ErpAccountMapper accountMapper;

    @Override
    public Long createAccount(ErpAccountSaveReqVO createReqVO) {
        // 插入
        ErpAccountDO account = BeanUtils.toBean(createReqVO, ErpAccountDO.class);
        accountMapper.insert(account);
        // 返回
        return account.getId();
    }

    @Override
    public void updateAccount(ErpAccountSaveReqVO updateReqVO) {
        // 校验存在
        validateAccountExists(updateReqVO.getId());
        // 更新
        ErpAccountDO updateObj = BeanUtils.toBean(updateReqVO, ErpAccountDO.class);
        accountMapper.updateById(updateObj);
    }

    @Override
    public void updateAccountDefaultStatus(Long id, Boolean defaultStatus) {
        // 1. 校验存在
        validateAccountExists(id);

        // 2.1 如果开启，则需要关闭所有其它的默认
        if (defaultStatus) {
            ErpAccountDO account = accountMapper.selectByDefaultStatus();
            if (account != null) {
                accountMapper.updateById(new ErpAccountDO().setId(account.getId()).setDefaultStatus(false));
            }
        }
        // 2.2 更新对应的默认状态
        accountMapper.updateById(new ErpAccountDO().setId(id).setDefaultStatus(defaultStatus));
    }

    @Override
    public void deleteAccount(Long id) {
        // 校验存在
        validateAccountExists(id);
        // 删除
        accountMapper.deleteById(id);
    }

    private void validateAccountExists(Long id) {
        if (accountMapper.selectById(id) == null) {
            throw exception(ACCOUNT_NOT_EXISTS);
        }
    }

    @Override
    public ErpAccountDO getAccount(Long id) {
        return accountMapper.selectById(id);
    }

    @Override
    public ErpAccountDO validateAccount(Long id) {
        ErpAccountDO account = accountMapper.selectById(id);
        if (account == null) {
            throw exception(ACCOUNT_NOT_EXISTS);
        }
        if (CommonStatusEnum.isDisable(account.getStatus())) {
            throw exception(ACCOUNT_NOT_ENABLE, account.getName());
        }
        return account;
    }

    @Override
    public List<ErpAccountDO> getAccountListByStatus(Integer status) {
        return accountMapper.selectListByStatus(status);
    }

    @Override
    public List<ErpAccountDO> getAccountList(Collection<Long> ids) {
        return accountMapper.selectBatchIds(ids);
    }

    @Override
    public PageResult<ErpAccountDO> getAccountPage(ErpAccountPageReqVO pageReqVO) {
        return accountMapper.selectPage(pageReqVO);
    }

}