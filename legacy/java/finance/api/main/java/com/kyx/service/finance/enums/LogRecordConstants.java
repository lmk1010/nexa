package com.kyx.service.finance.enums;

/**
 * 日志记录常量类
 *
 * @author xyang
 */
public interface LogRecordConstants {

    // ======================= FINANCE 科目模板 =======================

    String FINANCE_SUBJECT_TEMPLATE_TYPE = "FINANCE 科目模板";
    String FINANCE_SUBJECT_TEMPLATE_CREATE_SUB_TYPE = "创建科目模板";
    String FINANCE_SUBJECT_TEMPLATE_CREATE_SUCCESS = "创建了科目模板【{{#reqVO.subjectCode}}】";
    String FINANCE_SUBJECT_TEMPLATE_UPDATE_SUB_TYPE = "更新科目模板";
    String FINANCE_SUBJECT_TEMPLATE_UPDATE_SUCCESS = "更新了科目模板【{{#reqVO.subjectCode}}】: {_DIFF{#reqVO}}";
    String FINANCE_SUBJECT_TEMPLATE_DELETE_SUB_TYPE = "删除科目模板";
    String FINANCE_SUBJECT_TEMPLATE_DELETE_SUCCESS = "删除了科目模板【{{#id}}】";


    // ======================= COMPANY 账套信息 =======================

    String FINANCE_COMPANY_TEMPLATE_TYPE = "FINANCE 账套信息";
    String FINANCE_COMPANY_TEMPLATE_CREATE_SUB_TYPE = "创建账套信息";
    String FINANCE_COMPANY_TEMPLATE_CREATE_SUCCESS = "创建了账套信息【{{#reqVO.companyCode}}】";
    String FINANCE_COMPANY_TEMPLATE_UPDATE_SUB_TYPE = "更新账套信息";
    String FINANCE_COMPANY_TEMPLATE_UPDATE_SUCCESS = "更新了账套信息【{{#reqVO.companyCode}}】: {_DIFF{#reqVO}}";
    String FINANCE_COMPANY_TEMPLATE_DELETE_SUB_TYPE = "删除账套信息";
    String FINANCE_COMPANY_TEMPLATE_DELETE_SUCCESS = "删除了账套信息【{{#id}}】";

    // ======================= ACCOUNT 账户信息 =======================

    String FINANCE_ACCOUNT_TEMPLATE_TYPE = "FINANCE 账户信息";
    String FINANCE_ACCOUNT_TEMPLATE_CREATE_SUB_TYPE = "创建账户信息";
    String FINANCE_ACCOUNT_TEMPLATE_CREATE_SUCCESS = "创建了账户信息【{{#reqVO.bankName}}】";
    String FINANCE_ACCOUNT_TEMPLATE_UPDATE_SUB_TYPE = "更新账户信息";
    String FINANCE_ACCOUNT_TEMPLATE_UPDATE_SUCCESS = "更新了账户信息【{{#reqVO.bankName}}】: {_DIFF{#reqVO}}";
    String FINANCE_ACCOUNT_TEMPLATE_DELETE_SUB_TYPE = "删除账户信息";
    String FINANCE_ACCOUNT_TEMPLATE_DELETE_SUCCESS = "删除了账户信息【{{#id}}】";

    // ======================= CONTACT 往来单位信息 =======================

    String FINANCE_CONTACT_TEMPLATE_TYPE = "FINANCE 往来单位信息";
    String FINANCE_CONTACT_TEMPLATE_CREATE_SUB_TYPE = "创建往来单位信息";
    String FINANCE_CONTACT_TEMPLATE_CREATE_SUCCESS = "创建了往来单位信息【{{#reqVO.contactName}}】";
    String FINANCE_CONTACT_TEMPLATE_UPDATE_SUB_TYPE = "更新往来单位信息";
    String FINANCE_CONTACT_TEMPLATE_UPDATE_SUCCESS = "更新了往来单位信息【{{#reqVO.contactName}}】: {_DIFF{#reqVO}}";
    String FINANCE_CONTACT_TEMPLATE_DELETE_SUB_TYPE = "删除往来单位信息";
    String FINANCE_CONTACT_TEMPLATE_DELETE_SUCCESS = "删除了往来单位信息【{{#id}}】";

    // ======================= OPENING BALANCE 期初余额 =======================

    String FINANCE_OPENING_BALANCE_TEMPLATE_TYPE = "FINANCE 期初余额";
    String FINANCE_OPENING_BALANCE_TEMPLATE_BATCH_SAVE_SUB_TYPE = "批量保存期初余额";
    String FINANCE_OPENING_BALANCE_TEMPLATE_BATCH_SAVE_SUCCESS = "批量保存了期初余额，账套【{{#reqVO.companyId}}】期间【{{#reqVO.period}}】";
    String FINANCE_OPENING_BALANCE_TEMPLATE_LOCK_SUB_TYPE = "锁定/解锁期初余额";
    String FINANCE_OPENING_BALANCE_TEMPLATE_LOCK_SUCCESS = "更新了期初余额锁定状态，账套【{{#reqVO.companyId}}】期间【{{#reqVO.period}}】";
    String FINANCE_OPENING_BALANCE_TEMPLATE_ROLL_SUB_TYPE = "滚动期初余额";
    String FINANCE_OPENING_BALANCE_TEMPLATE_ROLL_SUCCESS = "滚动了期初余额，账套【{{#reqVO.companyId}}】来源期间【{{#reqVO.fromPeriod}}】目标期间【{{#reqVO.toPeriod}}】";

    // ======================= TRANSACTION 流水 =======================

    String FINANCE_TRANSACTION_TEMPLATE_TYPE = "FINANCE 流水";
    String FINANCE_TRANSACTION_TEMPLATE_CREATE_SUB_TYPE = "创建流水";
    String FINANCE_TRANSACTION_TEMPLATE_CREATE_SUCCESS = "创建了流水【{{#reqVO.transactionNo}}】";
    String FINANCE_TRANSACTION_TEMPLATE_UPDATE_SUB_TYPE = "更新流水";
    String FINANCE_TRANSACTION_TEMPLATE_UPDATE_SUCCESS = "更新了流水【{{#reqVO.transactionNo}}】: {_DIFF{#reqVO}}";
    String FINANCE_TRANSACTION_TEMPLATE_DELETE_SUB_TYPE = "删除流水";
    String FINANCE_TRANSACTION_TEMPLATE_DELETE_SUCCESS = "删除了流水【{{#id}}】";
    String FINANCE_TRANSACTION_TEMPLATE_REVERSE_SUB_TYPE = "作废流水";
    String FINANCE_TRANSACTION_TEMPLATE_REVERSE_SUCCESS = "作废了流水【{{#id}}】";

    // ======================= RECEIVABLE PAYABLE 往来账 =======================

    String FINANCE_RECEIVABLE_PAYABLE_TEMPLATE_TYPE = "FINANCE 往来账";
    String FINANCE_RECEIVABLE_PAYABLE_TEMPLATE_CREATE_SUB_TYPE = "创建往来账";
    String FINANCE_RECEIVABLE_PAYABLE_TEMPLATE_CREATE_SUCCESS = "创建了往来账【{{#reqVO.billNo}}】";
    String FINANCE_RECEIVABLE_PAYABLE_TEMPLATE_UPDATE_SUB_TYPE = "更新往来账";
    String FINANCE_RECEIVABLE_PAYABLE_TEMPLATE_UPDATE_SUCCESS = "更新了往来账【{{#reqVO.billNo}}】: {_DIFF{#reqVO}}";
    String FINANCE_RECEIVABLE_PAYABLE_TEMPLATE_DELETE_SUB_TYPE = "删除往来账";
    String FINANCE_RECEIVABLE_PAYABLE_TEMPLATE_DELETE_SUCCESS = "删除了往来账【{{#id}}】";
    String FINANCE_RECEIVABLE_PAYABLE_TEMPLATE_WRITE_OFF_SUB_TYPE = "往来账核销";
    String FINANCE_RECEIVABLE_PAYABLE_TEMPLATE_WRITE_OFF_SUCCESS = "对往来账【{{#reqVO.arpId}}】执行核销，金额【{{#reqVO.amount}}】";

}
