package com.kyx.service.finance.operatelog;

import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.dict.core.DictFrameworkUtils;
import com.kyx.service.finance.enums.DictTypeConstants;
import com.mzt.logapi.service.IParseFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 会计制度的 {@link IParseFunction} 实现类
 *
 * @author xyang
 */
@Component
@Slf4j
public class AccountingSystemParseFunction implements IParseFunction {

    public static final String NAME = "getAccountingSystem";

    @Override
    public boolean executeBefore() {
        return true;
    }

    @Override
    public String functionName() {
        return NAME;
    }

    @Override
    public String apply(Object value) {
        if (StrUtil.isEmptyIfStr(value)) {
            return "";
        }
        return DictFrameworkUtils.parseDictDataLabel(DictTypeConstants.ACCOUNTING_SYSTEM, value.toString());
    }

}
