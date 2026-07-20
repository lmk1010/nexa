package com.kyx.service.finance.operatelog;

import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.dict.core.DictFrameworkUtils;
import com.kyx.service.finance.enums.DictTypeConstants;
import com.mzt.logapi.service.IParseFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 资产负债类型的 {@link IParseFunction} 实现类
 *
 * @author xyang
 */
@Slf4j
@Component
public class SubjectTypeParseFunction implements IParseFunction {

    public static final String NAME = "getSubjectTypeName";

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
        return DictFrameworkUtils.parseDictDataLabel(DictTypeConstants.SUBJECT_TYPE, value.toString());
    }

}