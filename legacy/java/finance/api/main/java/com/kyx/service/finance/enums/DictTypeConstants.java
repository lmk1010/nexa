package com.kyx.service.finance.enums;

/**
 * 字典类型常量类
 *
 * @author xyang
 */
public interface DictTypeConstants {

    String FINANCE_DICT_PREFIX = "finance_";

    /** 科目类型 */
    String SUBJECT_TYPE = FINANCE_DICT_PREFIX + "subject_type";

    /** 账户类型 */
    String ACCOUNT_TYPE = FINANCE_DICT_PREFIX + "account_type";

    /** 会计制度 */
    String ACCOUNTING_SYSTEM = FINANCE_DICT_PREFIX + "accounting_system";

    /** 货币类型 */
    String CURRENCY = FINANCE_DICT_PREFIX + "currency";

    /** 流水类型 */
    String TRANSACTION_TYPE = FINANCE_DICT_PREFIX + "transaction_type";

    /** 支付方式 */
    String PAYMENT_METHOD = FINANCE_DICT_PREFIX + "payment_method";

    /** 凭证类型 */
    String VOUCHER_TYPE = FINANCE_DICT_PREFIX + "voucher_type";

    /** 凭证状态 */
    String VOUCHER_STATUS = FINANCE_DICT_PREFIX + "voucher_status";

    /** 结账状态 */
    String CLOSING_STATUS = FINANCE_DICT_PREFIX + "closing_status";
}
