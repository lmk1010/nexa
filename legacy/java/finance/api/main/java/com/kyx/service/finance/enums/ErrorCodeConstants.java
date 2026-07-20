package com.kyx.service.finance.enums;

import com.kyx.foundation.common.exception.ErrorCode;

/**
 * 错误码常量类
 *
 * @author xyang
 */
public interface ErrorCodeConstants {

    // ========== FINANCE 结算账户 1-031-600-000 ==========
    ErrorCode ACCOUNT_NOT_EXISTS = new ErrorCode(1_031_600_000, "结算账户不存在");
    ErrorCode ACCOUNT_BALANCE_NOT_ENOUGH = new ErrorCode(1_031_600_001, "账户余额不足");
    ErrorCode ACCOUNT_USED = new ErrorCode(1_031_600_002, "结算账户已被业务单据引用，不能删除");
    ErrorCode ACCOUNT_OPTION_NOT_EXISTS = new ErrorCode(1_031_600_003, "账户选项不存在");
    ErrorCode ACCOUNT_DISABLED = new ErrorCode(1_031_600_004, "结算账户已停用，不能继续引用");
    ErrorCode ACCOUNT_OPTION_VALUE_EXISTS = new ErrorCode(1_031_600_005, "同类型选项值已存在");

    // ========== FINANCE 账套信息 1-031-601-000 ==========
    ErrorCode COMPANY_NOT_EXISTS = new ErrorCode(1_031_601_000, "账套不存在");
    ErrorCode COMPANY_CODE_EXISTS = new ErrorCode(1_031_601_001, "账套编码已存在");

    // ========== FINANCE 科目模板 1-031-602-000 ==========
    ErrorCode SUBJECT_TEMPLATE_NOT_EXISTS = new ErrorCode(1_031_602_000, "科目模板不存在");
    ErrorCode SUBJECT_TEMPLATE_CODE_EXISTS = new ErrorCode(1_031_602_001, "科目模板编码已存在");
    ErrorCode SUBJECT_TEMPLATE_USED = new ErrorCode(1_031_602_003, "科目模板已被使用");
    ErrorCode SUBJECT_TEMPLATE_PARENT_CODE_NOT_EXISTS = new ErrorCode(1_031_602_004, "科目模板父级编码不存在");
    ErrorCode SUBJECT_TEMPLATE_HAS_CHILDREN = new ErrorCode(1_031_602_005, "科目模板存在子项，不能删除");
    ErrorCode SUBJECT_COMPANY_LEVEL_RESTRICT = new ErrorCode(1_031_602_006, "一级/二级科目由系统导入，不允许手动新增或删除");
    ErrorCode SUBJECT_COMPANY_NOT_EXISTS = new ErrorCode(1_031_602_007, "账套科目不存在");
    ErrorCode SUBJECT_COMPANY_REFERENCED = new ErrorCode(1_031_602_008, "科目已被凭证或流水引用，不能删除");

    // ========== FINANCE 通用 1-031-603-000 ==========
    ErrorCode STATUS_DISABLE_ERROR = new ErrorCode(1_031_603_000, "账套已禁用，不能操作");
    ErrorCode STATUS_ENABLE_ERROR = new ErrorCode(1_031_603_001, "账套已启用，不能操作");

    // ========== FINANCE 往来单位 1-031-604-000 ==========
    ErrorCode CONTACT_NOT_EXISTS = new ErrorCode(1_031_604_000, "往来单位不存在");
    ErrorCode CONTACT_USED = new ErrorCode(1_031_604_002, "往来单位已被业务单据引用，不能删除");
    ErrorCode CONTACT_DISABLED = new ErrorCode(1_031_604_003, "往来单位已停用，不能继续引用");
    ErrorCode CONTACT_GROUP_NOT_EXISTS = new ErrorCode(1_031_604_004, "往来分组不存在");
    ErrorCode CONTACT_GROUP_NOT_EDITABLE = new ErrorCode(1_031_604_005, "当前分组不允许编辑");
    ErrorCode CONTACT_GROUP_NAME_EXISTS = new ErrorCode(1_031_604_006, "同级分组名称已存在");
    ErrorCode CONTACT_GROUP_HAS_CONTACT = new ErrorCode(1_031_604_007, "分组下存在往来单位，不能删除");

    // ========== FINANCE 期初余额 1-031-605-000 ==========
    ErrorCode OPENING_BALANCE_NOT_EXISTS = new ErrorCode(1_031_605_000, "期初余额记录不存在");
    ErrorCode OPENING_BALANCE_PERIOD_LOCKED = new ErrorCode(1_031_605_001, "当前期间期初余额已锁定，不能修改");    ErrorCode OPENING_BALANCE_ROLL_PERIOD_INVALID = new ErrorCode(1_031_605_003, "滚动目标期间必须晚于来源期间");

    // ========== FINANCE 流水 1-031-606-000 ==========
    ErrorCode TRANSACTION_NOT_EXISTS = new ErrorCode(1_031_606_000, "流水不存在");
    ErrorCode TRANSACTION_NO_EXISTS = new ErrorCode(1_031_606_001, "交易单号已存在");
    ErrorCode TRANSACTION_OPPOSITE_ACCOUNT_REQUIRED = new ErrorCode(1_031_606_002, "转账流水必须填写对方账户");
    ErrorCode TRANSACTION_TYPE_INVALID = new ErrorCode(1_031_606_003, "交易类型非法");
    ErrorCode TRANSACTION_TRANSFER_ACCOUNT_SAME = new ErrorCode(1_031_606_004, "转账流水的转出账户和转入账户不能相同");
    ErrorCode TRANSACTION_STATUS_INVALID = new ErrorCode(1_031_606_005, "流水状态非法");
    ErrorCode TRANSACTION_STATUS_TRANSITION_INVALID = new ErrorCode(1_031_606_006, "流水状态流转不合法");
    ErrorCode TRANSACTION_REVERSED_NOT_OPERABLE = new ErrorCode(1_031_606_007, "已作废流水不允许继续操作");
    ErrorCode TRANSACTION_EDIT_ONLY_PENDING = new ErrorCode(1_031_606_008, "仅草稿流水允许编辑");
    ErrorCode TRANSACTION_DELETE_ONLY_PENDING = new ErrorCode(1_031_606_009, "仅草稿流水允许删除");
    ErrorCode TRANSACTION_REVERSE_ONLY_SUCCESS = new ErrorCode(1_031_606_010, "仅成功流水允许作废");
    ErrorCode TRANSACTION_SUBJECT_REQUIRED = new ErrorCode(1_031_606_011, "收入/支出流水必须填写有效科目");
    ErrorCode TRANSACTION_SUBJECT_TYPE_MISMATCH = new ErrorCode(1_031_606_012, "科目类型与流水类型不匹配");

    // ========== FINANCE 往来账 1-031-607-000 ==========
    ErrorCode RECEIVABLE_PAYABLE_NOT_EXISTS = new ErrorCode(1_031_607_000, "往来账不存在");
    ErrorCode RECEIVABLE_PAYABLE_BILL_NO_EXISTS = new ErrorCode(1_031_607_001, "往来账单号已存在");
    ErrorCode RECEIVABLE_PAYABLE_BALANCE_INVALID = new ErrorCode(1_031_607_002, "往来账余额非法");
    ErrorCode RECEIVABLE_PAYABLE_WRITE_OFF_AMOUNT_INVALID = new ErrorCode(1_031_607_003, "核销金额非法");
    ErrorCode RECEIVABLE_PAYABLE_WRITE_OFF_ACCOUNT_REQUIRED = new ErrorCode(1_031_607_004, "自动生成流水时账户不能为空");
    ErrorCode RECEIVABLE_PAYABLE_WRITE_OFF_TRANSACTION_INVALID = new ErrorCode(1_031_607_005, "关联流水不符合核销要求");
    ErrorCode RECEIVABLE_PAYABLE_WRITE_OFF_TRANSACTION_AMOUNT_EXCEED = new ErrorCode(1_031_607_006, "核销金额超过关联流水可核销金额");
    ErrorCode RECEIVABLE_PAYABLE_TYPE_INVALID = new ErrorCode(1_031_607_007, "往来账类型非法");
    ErrorCode RECEIVABLE_PAYABLE_DELETE_NOT_ALLOWED = new ErrorCode(1_031_607_008, "已发生核销的单据不允许删除");
    ErrorCode RECEIVABLE_PAYABLE_WRITE_OFF_CONCURRENT_CONFLICT = new ErrorCode(1_031_607_009, "核销失败，单据状态或余额已变化，请刷新后重试");

    // ========== FINANCE 凭证 1-031-608-000 ==========
    ErrorCode VOUCHER_NOT_EXISTS = new ErrorCode(1_031_608_000, "凭证不存在");
    ErrorCode VOUCHER_NO_EXISTS = new ErrorCode(1_031_608_001, "凭证号已存在");
    ErrorCode VOUCHER_STATUS_INVALID = new ErrorCode(1_031_608_002, "凭证状态非法");
    ErrorCode VOUCHER_STATUS_TRANSITION_INVALID = new ErrorCode(1_031_608_003, "凭证状态流转不合法");
    ErrorCode VOUCHER_EDIT_ONLY_DRAFT = new ErrorCode(1_031_608_004, "仅草稿凭证允许编辑");
    ErrorCode VOUCHER_DELETE_ONLY_DRAFT = new ErrorCode(1_031_608_005, "仅草稿凭证允许删除");
    ErrorCode VOUCHER_POST_NOT_ALLOWED = new ErrorCode(1_031_608_006, "当前凭证状态不允许过账");
    ErrorCode VOUCHER_VOID_NOT_ALLOWED = new ErrorCode(1_031_608_007, "当前凭证状态不允许作废");
    ErrorCode VOUCHER_DETAIL_EMPTY = new ErrorCode(1_031_608_008, "凭证明细不能为空");
    ErrorCode VOUCHER_DETAIL_AMOUNT_INVALID = new ErrorCode(1_031_608_009, "凭证明细借贷金额非法");
    ErrorCode VOUCHER_DEBIT_CREDIT_NOT_BALANCED = new ErrorCode(1_031_608_010, "凭证借贷不平衡");

    // ========== FINANCE 月末结账 1-031-609-000 ==========
    ErrorCode CLOSING_NOT_EXISTS = new ErrorCode(1_031_609_000, "结账记录不存在");
    ErrorCode CLOSING_PERIOD_INVALID = new ErrorCode(1_031_609_001, "结账期间格式非法，应为 yyyyMM");
    ErrorCode CLOSING_STATUS_INVALID = new ErrorCode(1_031_609_002, "结账状态非法");
    ErrorCode CLOSING_ALREADY_SUCCESS = new ErrorCode(1_031_609_003, "该期间已结账");
    ErrorCode CLOSING_REVERSE_ONLY_SUCCESS = new ErrorCode(1_031_609_004, "仅成功结账允许反结账");
    ErrorCode CLOSING_TYPE_INVALID = new ErrorCode(1_031_609_005, "结账类型非法");
    ErrorCode CLOSING_PRECHECK_UNPOSTED_VOUCHER = new ErrorCode(1_031_609_006, "存在未过账凭证，结账前请先处理");
    ErrorCode CLOSING_PERIOD_SEQUENCE_INVALID = new ErrorCode(1_031_609_007, "结账期间必须为当前期间或下一期间");
    ErrorCode CLOSING_REVERSE_SEQUENCE_INVALID = new ErrorCode(1_031_609_008, "仅允许按倒序逐期反结账");
    ErrorCode PERIOD_LOCKED_NOT_ALLOWED = new ErrorCode(1_031_609_009, "该期间已结账或锁定，不允许变更");
    ErrorCode CLOSING_CONCURRENT_CONFLICT = new ErrorCode(1_031_609_010, "结账状态已变化，请刷新后重试");
    ErrorCode CLOSING_PRECHECK_DRAFT_TRANSACTION = new ErrorCode(1_031_609_011, "存在草稿流水，结账前请先处理");
    ErrorCode CLOSING_PROFIT_TRANSFER_SUBJECT_INVALID = new ErrorCode(1_031_609_012, "损益结转失败：科目不存在或已停用");
    ErrorCode CLOSING_PROFIT_TRANSFER_VOUCHER_NO_CONFLICT = new ErrorCode(1_031_609_013, "损益结转失败：自动凭证号生成冲突");
    ErrorCode CLOSING_PROFIT_TRANSFER_BALANCE_INVALID = new ErrorCode(1_031_609_014, "损益结转失败：自动凭证借贷不平衡");
}
