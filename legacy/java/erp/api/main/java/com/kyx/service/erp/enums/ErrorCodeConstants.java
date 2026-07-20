package com.kyx.service.erp.enums;

import com.kyx.foundation.common.exception.ErrorCode;

/**
 * ERP 错误码枚举类
 * <p>
 * erp 系统，使用 1-030-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== ERP 供应商（1-030-100-000） ==========
    ErrorCode SUPPLIER_NOT_EXISTS = new ErrorCode(1_030_100_000, "供应商不存在");
    ErrorCode SUPPLIER_NOT_ENABLE = new ErrorCode(1_030_100_000, "供应商({})未启用");

    // ========== ERP 采购订单（1-030-101-000） ==========
    ErrorCode PURCHASE_ORDER_NOT_EXISTS = new ErrorCode(1_030_101_000, "采购订单不存在");
    ErrorCode PURCHASE_ORDER_DELETE_FAIL_APPROVE = new ErrorCode(1_030_101_001, "采购订单({})已审核，无法删除");
    ErrorCode PURCHASE_ORDER_PROCESS_FAIL = new ErrorCode(1_030_101_002, "反审核失败，只有已审核的采购订单才能反审核");
    ErrorCode PURCHASE_ORDER_APPROVE_FAIL = new ErrorCode(1_030_101_003, "审核失败，只有未审核的采购订单才能审核");
    ErrorCode PURCHASE_ORDER_NO_EXISTS = new ErrorCode(1_030_101_004, "生成采购单号失败，请重新提交");
    ErrorCode PURCHASE_ORDER_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_101_005, "采购订单({})已审核，无法修改");
    ErrorCode PURCHASE_ORDER_NOT_APPROVE = new ErrorCode(1_030_101_006, "采购订单未审核，无法操作");
    ErrorCode PURCHASE_ORDER_ITEM_IN_FAIL_PRODUCT_EXCEED = new ErrorCode(1_030_101_007, "采购订单项({})超过最大允许入库数量({})");
    ErrorCode PURCHASE_ORDER_PROCESS_FAIL_EXISTS_IN = new ErrorCode(1_030_101_008, "反审核失败，已存在对应的采购入库单");
ErrorCode PURCHASE_ORDER_ITEM_RETURN_FAIL_IN_EXCEED = new ErrorCode(1_030_101_009, "采购订单项({})超过最大允许退货数量({})");
    ErrorCode PURCHASE_ORDER_PROCESS_FAIL_EXISTS_RETURN = new ErrorCode(1_030_101_010, "反审核失败，已存在对应的采购退货单");
    ErrorCode PURCHASE_ORDER_SUBMIT_FAIL_STATUS_NOT_PROCESS = new ErrorCode(1_030_101_011, "采购订单提交失败，只有待审核状态的订单才能提交");

    // ========== ERP 采购组织（1-030-104-000） ==========
    ErrorCode PR_ORGANIZATION_NOT_EXISTS = new ErrorCode(1_030_104_000, "采购组织不存在");
    ErrorCode PR_ORGANIZATION_CODE_DUPLICATE = new ErrorCode(1_030_104_001, "组织编码已存在");
    ErrorCode PR_ORGANIZATION_EXITS_CHILDREN = new ErrorCode(1_030_104_002, "存在子组织，无法删除");
    ErrorCode PR_ORGANIZATION_PARENT_NOT_EXISTS = new ErrorCode(1_030_104_003, "上级组织不存在");
    ErrorCode PR_ORGANIZATION_PARENT_DISABLED = new ErrorCode(1_030_104_004, "上级组织已禁用");
    ErrorCode PR_ORGANIZATION_PARENT_ERROR = new ErrorCode(1_030_104_005, "不能设置自己或子组织为上级组织");

    // ========== ERP 采购入库（1-030-102-000） ==========
    ErrorCode PURCHASE_IN_NOT_EXISTS = new ErrorCode(1_030_102_000, "采购入库单不存在");
    ErrorCode PURCHASE_IN_DELETE_FAIL_APPROVE = new ErrorCode(1_030_102_001, "采购入库单({})已审核，无法删除");
    ErrorCode PURCHASE_IN_PROCESS_FAIL = new ErrorCode(1_030_102_002, "反审核失败，只有已审核的入库单才能反审核");
    ErrorCode PURCHASE_IN_APPROVE_FAIL = new ErrorCode(1_030_102_003, "审核失败，只有未审核的入库单才能审核");
    ErrorCode PURCHASE_IN_NO_EXISTS = new ErrorCode(1_030_102_004, "生成入库单失败，请重新提交");
    ErrorCode PURCHASE_IN_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_102_005, "采购入库单({})已审核，无法修改");
    ErrorCode PURCHASE_IN_NOT_APPROVE = new ErrorCode(1_030_102_006, "采购入库单未审核，无法操作");
    ErrorCode PURCHASE_IN_FAIL_PAYMENT_PRICE_EXCEED = new ErrorCode(1_030_102_007, "付款金额({})超过采购入库单总金额({})");
    ErrorCode PURCHASE_IN_PROCESS_FAIL_EXISTS_PAYMENT = new ErrorCode(1_030_102_008, "反审核失败，已存在对应的付款单");

    // ========== ERP 采购退货（1-030-103-000） ==========
    ErrorCode PURCHASE_RETURN_NOT_EXISTS = new ErrorCode(1_030_103_000, "采购退货单不存在");
    ErrorCode PURCHASE_RETURN_DELETE_FAIL_APPROVE = new ErrorCode(1_030_103_001, "采购退货单({})已审核，无法删除");
    ErrorCode PURCHASE_RETURN_PROCESS_FAIL = new ErrorCode(1_030_103_002, "反审核失败，只有已审核的退货单才能反审核");
    ErrorCode PURCHASE_RETURN_APPROVE_FAIL = new ErrorCode(1_030_103_003, "审核失败，只有未审核的退货单才能审核");
    ErrorCode PURCHASE_RETURN_NO_EXISTS = new ErrorCode(1_030_103_004, "生成退货单失败，请重新提交");
    ErrorCode PURCHASE_RETURN_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_103_005, "采购退货单({})已审核，无法修改");
    ErrorCode PURCHASE_RETURN_NOT_APPROVE = new ErrorCode(1_030_103_006, "采购退货单未审核，无法操作");
    ErrorCode PURCHASE_RETURN_FAIL_REFUND_PRICE_EXCEED = new ErrorCode(1_030_103_007, "退款金额({})超过采购退货单总金额({})");
    ErrorCode PURCHASE_RETURN_PROCESS_FAIL_EXISTS_REFUND = new ErrorCode(1_030_103_008, "反审核失败，已存在对应的退款单");

    // ========== ERP 客户（1-030-200-000）==========
    ErrorCode CUSTOMER_NOT_EXISTS = new ErrorCode(1_020_200_000, "客户不存在");
    ErrorCode CUSTOMER_NOT_ENABLE = new ErrorCode(1_020_200_001, "客户({})未启用");

    // ========== ERP 销售订单（1-030-201-000） ==========
    ErrorCode SALE_ORDER_NOT_EXISTS = new ErrorCode(1_020_201_000, "销售订单不存在");
    ErrorCode SALE_ORDER_DELETE_FAIL_APPROVE = new ErrorCode(1_020_201_001, "销售订单({})已审核，无法删除");
    ErrorCode SALE_ORDER_PROCESS_FAIL = new ErrorCode(1_020_201_002, "反审核失败，只有已审核的销售订单才能反审核");
    ErrorCode SALE_ORDER_APPROVE_FAIL = new ErrorCode(1_020_201_003, "审核失败，只有未审核的销售订单才能审核");
    ErrorCode SALE_ORDER_NO_EXISTS = new ErrorCode(1_020_201_004, "生成销售单号失败，请重新提交");
    ErrorCode SALE_ORDER_UPDATE_FAIL_APPROVE = new ErrorCode(1_020_201_005, "销售订单({})已审核，无法修改");
    ErrorCode SALE_ORDER_NOT_APPROVE = new ErrorCode(1_020_201_006, "销售订单未审核，无法操作");
    ErrorCode SALE_ORDER_ITEM_OUT_FAIL_PRODUCT_EXCEED = new ErrorCode(1_020_201_007, "销售订单项({})超过最大允许出库数量({})");
    ErrorCode SALE_ORDER_PROCESS_FAIL_EXISTS_OUT = new ErrorCode(1_020_201_008, "反审核失败，已存在对应的销售出库单");
    ErrorCode SALE_ORDER_ITEM_RETURN_FAIL_OUT_EXCEED = new ErrorCode(1_020_201_009, "销售订单项({})超过最大允许退货数量({})");
    ErrorCode SALE_ORDER_PROCESS_FAIL_EXISTS_RETURN = new ErrorCode(1_020_201_010, "反审核失败，已存在对应的销售退货单");

    // ========== ERP 销售出库（1-030-202-000） ==========
    ErrorCode SALE_OUT_NOT_EXISTS = new ErrorCode(1_020_202_000, "销售出库单不存在");
    ErrorCode SALE_OUT_DELETE_FAIL_APPROVE = new ErrorCode(1_020_202_001, "销售出库单({})已审核，无法删除");
    ErrorCode SALE_OUT_PROCESS_FAIL = new ErrorCode(1_020_202_002, "反审核失败，只有已审核的出库单才能反审核");
    ErrorCode SALE_OUT_APPROVE_FAIL = new ErrorCode(1_020_202_003, "审核失败，只有未审核的出库单才能审核");
    ErrorCode SALE_OUT_NO_EXISTS = new ErrorCode(1_020_202_004, "生成出库单失败，请重新提交");
    ErrorCode SALE_OUT_UPDATE_FAIL_APPROVE = new ErrorCode(1_020_202_005, "销售出库单({})已审核，无法修改");
    ErrorCode SALE_OUT_NOT_APPROVE = new ErrorCode(1_020_202_006, "销售出库单未审核，无法操作");
    ErrorCode SALE_OUT_FAIL_RECEIPT_PRICE_EXCEED = new ErrorCode(1_020_202_007, "收款金额({})超过销售出库单总金额({})");
    ErrorCode SALE_OUT_PROCESS_FAIL_EXISTS_RECEIPT = new ErrorCode(1_020_202_008, "反审核失败，已存在对应的收款单");

    // ========== ERP 销售退货（1-030-203-000） ==========
    ErrorCode SALE_RETURN_NOT_EXISTS = new ErrorCode(1_020_203_000, "销售退货单不存在");
    ErrorCode SALE_RETURN_DELETE_FAIL_APPROVE = new ErrorCode(1_020_203_001, "销售退货单({})已审核，无法删除");
    ErrorCode SALE_RETURN_PROCESS_FAIL = new ErrorCode(1_020_203_002, "反审核失败，只有已审核的退货单才能反审核");
    ErrorCode SALE_RETURN_APPROVE_FAIL = new ErrorCode(1_020_203_003, "审核失败，只有未审核的退货单才能审核");
    ErrorCode SALE_RETURN_NO_EXISTS = new ErrorCode(1_020_203_004, "生成退货单失败，请重新提交");
    ErrorCode SALE_RETURN_UPDATE_FAIL_APPROVE = new ErrorCode(1_020_203_005, "销售退货单({})已审核，无法修改");
    ErrorCode SALE_RETURN_NOT_APPROVE = new ErrorCode(1_020_203_006, "销售退货单未审核，无法操作");
    ErrorCode SALE_RETURN_FAIL_REFUND_PRICE_EXCEED = new ErrorCode(1_020_203_007, "退款金额({})超过销售退货单总金额({})");
    ErrorCode SALE_RETURN_PROCESS_FAIL_EXISTS_REFUND = new ErrorCode(1_020_203_008, "反审核失败，已存在对应的退款单");

    // ========== ERP 仓库 1-030-400-000 ==========
    ErrorCode WAREHOUSE_NOT_EXISTS = new ErrorCode(1_030_400_000, "仓库不存在");
    ErrorCode WAREHOUSE_NOT_ENABLE = new ErrorCode(1_030_400_001, "仓库({})未启用");

    // ========== ERP 其它入库单 1-030-401-000 ==========
    ErrorCode STOCK_IN_NOT_EXISTS = new ErrorCode(1_030_401_000, "其它入库单不存在");
    ErrorCode STOCK_IN_DELETE_FAIL_APPROVE = new ErrorCode(1_030_401_001, "其它入库单({})已审核，无法删除");
    ErrorCode STOCK_IN_PROCESS_FAIL = new ErrorCode(1_030_401_002, "反审核失败，只有已审核的入库单才能反审核");
    ErrorCode STOCK_IN_APPROVE_FAIL = new ErrorCode(1_030_401_003, "审核失败，只有未审核的入库单才能审核");
    ErrorCode STOCK_IN_NO_EXISTS = new ErrorCode(1_030_401_004, "生成入库单失败，请重新提交");
    ErrorCode STOCK_IN_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_401_005, "其它入库单({})已审核，无法修改");

    // ========== ERP 其它出库单 1-030-402-000 ==========
    ErrorCode STOCK_OUT_NOT_EXISTS = new ErrorCode(1_030_402_000, "其它出库单不存在");
    ErrorCode STOCK_OUT_DELETE_FAIL_APPROVE = new ErrorCode(1_030_402_001, "其它出库单({})已审核，无法删除");
    ErrorCode STOCK_OUT_PROCESS_FAIL = new ErrorCode(1_030_402_002, "反审核失败，只有已审核的出库单才能反审核");
    ErrorCode STOCK_OUT_APPROVE_FAIL = new ErrorCode(1_030_402_003, "审核失败，只有未审核的出库单才能审核");
    ErrorCode STOCK_OUT_NO_EXISTS = new ErrorCode(1_030_402_004, "生成出库单失败，请重新提交");
    ErrorCode STOCK_OUT_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_402_005, "其它出库单({})已审核，无法修改");

    // ========== ERP 库存调拨单 1-030-403-000 ==========
    ErrorCode STOCK_MOVE_NOT_EXISTS = new ErrorCode(1_030_402_000, "库存调拨单不存在");
    ErrorCode STOCK_MOVE_DELETE_FAIL_APPROVE = new ErrorCode(1_030_402_001, "库存调拨单({})已审核，无法删除");
    ErrorCode STOCK_MOVE_PROCESS_FAIL = new ErrorCode(1_030_402_002, "反审核失败，只有已审核的调拨单才能反审核");
    ErrorCode STOCK_MOVE_APPROVE_FAIL = new ErrorCode(1_030_402_003, "审核失败，只有未审核的调拨单才能审核");
    ErrorCode STOCK_MOVE_NO_EXISTS = new ErrorCode(1_030_402_004, "生成调拨号失败，请重新提交");
    ErrorCode STOCK_MOVE_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_402_005, "库存调拨单({})已审核，无法修改");

    // ========== ERP 库存盘点单 1-030-403-000 ==========
    ErrorCode STOCK_CHECK_NOT_EXISTS = new ErrorCode(1_030_403_000, "库存盘点单不存在");
    ErrorCode STOCK_CHECK_DELETE_FAIL_APPROVE = new ErrorCode(1_030_403_001, "库存盘点单({})已审核，无法删除");
    ErrorCode STOCK_CHECK_PROCESS_FAIL = new ErrorCode(1_030_403_002, "反审核失败，只有已审核的盘点单才能反审核");
    ErrorCode STOCK_CHECK_APPROVE_FAIL = new ErrorCode(1_030_403_003, "审核失败，只有未审核的盘点单才能审核");
    ErrorCode STOCK_CHECK_NO_EXISTS = new ErrorCode(1_030_403_004, "生成盘点号失败，请重新提交");
    ErrorCode STOCK_CHECK_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_403_005, "库存盘点单({})已审核，无法修改");

    // ========== ERP 产品库存 1-030-404-000 ==========
    ErrorCode STOCK_COUNT_NEGATIVE = new ErrorCode(1_030_404_000, "操作失败，产品({})所在仓库({})的库存：{}，小于变更数量：{}");
    ErrorCode STOCK_COUNT_NEGATIVE2 = new ErrorCode(1_030_404_001, "操作失败，产品({})所在仓库({})的库存不足");

    // ========== ERP 产品 1-030-500-000 ==========
    ErrorCode PRODUCT_NOT_EXISTS = new ErrorCode(1_030_500_000, "产品不存在");
    ErrorCode PRODUCT_NOT_ENABLE = new ErrorCode(1_030_500_001, "产品({})未启用");

    // ========== ERP 产品分类 1-030-501-000 ==========
    ErrorCode PRODUCT_CATEGORY_NOT_EXISTS = new ErrorCode(1_030_501_000, "产品分类不存在");
    ErrorCode PRODUCT_CATEGORY_EXITS_CHILDREN = new ErrorCode(1_030_501_001, "存在存在子产品分类，无法删除");
    ErrorCode PRODUCT_CATEGORY_PARENT_NOT_EXITS = new ErrorCode(1_030_501_002,"父级产品分类不存在");
    ErrorCode PRODUCT_CATEGORY_PARENT_ERROR = new ErrorCode(1_030_501_003, "不能设置自己为父产品分类");
    ErrorCode PRODUCT_CATEGORY_NAME_DUPLICATE = new ErrorCode(1_030_501_004, "已经存在该分类名称的产品分类");
    ErrorCode PRODUCT_CATEGORY_PARENT_IS_CHILD = new ErrorCode(1_030_501_005, "不能设置自己的子分类为父分类");
    ErrorCode PRODUCT_CATEGORY_EXITS_PRODUCT = new ErrorCode(1_030_502_002, "存在产品使用该分类，无法删除");

    // ========== ERP 产品单位 1-030-502-000 ==========
    ErrorCode PRODUCT_UNIT_NOT_EXISTS = new ErrorCode(1_030_502_000, "产品单位不存在");
    ErrorCode PRODUCT_UNIT_NAME_DUPLICATE = new ErrorCode(1_030_502_001, "已存在该名字的产品单位");
    ErrorCode PRODUCT_UNIT_EXITS_PRODUCT = new ErrorCode(1_030_502_002, "存在产品使用该单位，无法删除");

    // ========== ERP 结算账户 1-030-600-000 ==========
    ErrorCode ACCOUNT_NOT_EXISTS = new ErrorCode(1_030_600_000, "结算账户不存在");
    ErrorCode ACCOUNT_NOT_ENABLE = new ErrorCode(1_030_600_001, "结算账户({})未启用");

    // ========== ERP 付款单 1-030-601-000 ==========
    ErrorCode FINANCE_PAYMENT_NOT_EXISTS = new ErrorCode(1_030_601_000, "付款单不存在");
    ErrorCode FINANCE_PAYMENT_DELETE_FAIL_APPROVE = new ErrorCode(1_030_601_001, "付款单({})已审核，无法删除");
    ErrorCode FINANCE_PAYMENT_PROCESS_FAIL = new ErrorCode(1_030_601_002, "反审核失败，只有已审核的付款单才能反审核");
    ErrorCode FINANCE_PAYMENT_APPROVE_FAIL = new ErrorCode(1_030_601_003, "审核失败，只有未审核的付款单才能审核");
    ErrorCode FINANCE_PAYMENT_NO_EXISTS = new ErrorCode(1_030_601_004, "生成付款单号失败，请重新提交");
    ErrorCode FINANCE_PAYMENT_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_601_005, "付款单({})已审核，无法修改");

    // ========== ERP 收款单 1-030-602-000 ==========
    ErrorCode FINANCE_RECEIPT_NOT_EXISTS = new ErrorCode(1_030_602_000, "收款单不存在");
    ErrorCode FINANCE_RECEIPT_DELETE_FAIL_APPROVE = new ErrorCode(1_030_602_001, "收款单({})已审核，无法删除");
    ErrorCode FINANCE_RECEIPT_PROCESS_FAIL = new ErrorCode(1_030_602_002, "反审核失败，只有已审核的收款单才能反审核");
    ErrorCode FINANCE_RECEIPT_APPROVE_FAIL = new ErrorCode(1_030_602_003, "审核失败，只有未审核的收款单才能审核");
    ErrorCode FINANCE_RECEIPT_NO_EXISTS = new ErrorCode(1_030_602_004, "生成收款单号失败，请重新提交");
    ErrorCode FINANCE_RECEIPT_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_602_005, "收款单({})已审核，无法修改");

    // ========== ERP 资产 1-030-700-000 ==========
    ErrorCode ASSET_NOT_EXISTS = new ErrorCode(1_030_700_000, "资产不存在");
    ErrorCode ASSET_NO_DUPLICATE = new ErrorCode(1_030_700_001, "资产编码({})已存在");
    
    // ========== ERP 资产转移 1-030-702-000 ==========
    ErrorCode ASSET_TRANSFER_NOT_EXISTS = new ErrorCode(1_030_702_000, "资产转移记录不存在");
    ErrorCode ASSET_TRANSFER_NOT_ALLOWED = new ErrorCode(1_030_702_001, "资产不允许转移");
    
    // ========== ERP 资产调拨 1-030-703-000 ==========
    ErrorCode ASSET_REDISTRIBUTION_NOT_EXISTS = new ErrorCode(1_030_703_000, "资产调拨记录不存在");
    ErrorCode ASSET_REDISTRIBUTION_NOT_ALLOWED = new ErrorCode(1_030_703_001, "资产不允许调拨");
    ErrorCode ASSET_REDISTRIBUTION_ASSETS_EMPTY = new ErrorCode(1_030_703_002, "调拨资产列表不能为空");

    // ========== ERP 资产分类 1-030-701-000 ==========
    ErrorCode ASSET_CATEGORY_NOT_EXISTS = new ErrorCode(1_030_701_000, "资产分类不存在");
    ErrorCode ASSET_CATEGORY_EXITS_CHILDREN = new ErrorCode(1_030_701_001, "存在子资产分类，无法删除");
    ErrorCode ASSET_CATEGORY_PARENT_NOT_EXITS = new ErrorCode(1_030_701_002,"父级资产分类不存在");
    ErrorCode ASSET_CATEGORY_PARENT_ERROR = new ErrorCode(1_030_701_003, "不能设置自己为父资产分类");
    ErrorCode ASSET_CATEGORY_NAME_DUPLICATE = new ErrorCode(1_030_701_004, "已存在该分类名称的资产分类");
    ErrorCode ASSET_CATEGORY_CODE_DUPLICATE = new ErrorCode(1_030_701_005, "已存在该分类编码的资产分类");
    ErrorCode ASSET_CATEGORY_PARENT_IS_CHILD = new ErrorCode(1_030_701_006, "不能设置自己的子分类为父分类");
    ErrorCode ASSET_CATEGORY_EXITS_ASSET = new ErrorCode(1_030_701_007, "存在资产使用该分类，无法删除");

    // ========== ERP 资产类型 1-030-702-000 ==========
    ErrorCode ASSET_TYPE_NOT_EXISTS = new ErrorCode(1_030_702_000, "资产类型不存在");
    ErrorCode ASSET_TYPE_EXITS_CHILDREN = new ErrorCode(1_030_702_001, "存在子资产类型，无法删除");
    ErrorCode ASSET_TYPE_PARENT_NOT_EXITS = new ErrorCode(1_030_702_002,"父级资产类型不存在");
    ErrorCode ASSET_TYPE_PARENT_ERROR = new ErrorCode(1_030_702_003, "不能设置自己为父资产类型");
    ErrorCode ASSET_TYPE_NAME_DUPLICATE = new ErrorCode(1_030_702_004, "已存在该类型名称的资产类型");
    ErrorCode ASSET_TYPE_CODE_DUPLICATE = new ErrorCode(1_030_702_005, "已存在该类型编码的资产类型");
    ErrorCode ASSET_TYPE_PARENT_IS_CHILD = new ErrorCode(1_030_702_006, "不能设置自己的子类型为父类型");

    // ========== ERP 资产附件 1-030-703-000 ==========
    ErrorCode ASSET_ATTACHMENT_NOT_EXISTS = new ErrorCode(1_030_703_000, "资产附件不存在");
    ErrorCode ASSET_ATTACHMENT_FILE_EMPTY = new ErrorCode(1_030_703_001, "上传文件不能为空");
    ErrorCode ASSET_ATTACHMENT_FILE_SIZE_EXCEED = new ErrorCode(1_030_703_002, "文件大小超过限制");
    ErrorCode ASSET_ATTACHMENT_FILE_TYPE_NOT_SUPPORT = new ErrorCode(1_030_703_003, "不支持的文件类型");
    ErrorCode ASSET_ATTACHMENT_UPLOAD_FAIL = new ErrorCode(1_030_703_004, "文件上传失败");
    ErrorCode ASSET_ATTACHMENT_BATCH_FILES_EMPTY = new ErrorCode(1_030_703_005, "批量上传文件列表不能为空");
    ErrorCode ASSET_ATTACHMENT_BATCH_FILES_COUNT_EXCEED = new ErrorCode(1_030_703_006, "批量上传文件数量超过限制(最大{}个)");
    ErrorCode ASSET_ATTACHMENT_BATCH_UPLOAD_ALL_FAIL = new ErrorCode(1_030_703_007, "批量上传失败，所有文件都上传失败");

    // ========== ERP 资产领用 1-030-704-000 ==========
    ErrorCode ASSET_CHECKOUT_NOT_EXISTS = new ErrorCode(1_030_704_000, "资产领用记录不存在");
    ErrorCode ASSET_CHECKOUT_NOT_AVAILABLE = new ErrorCode(1_030_704_001, "资产不可领用，可能已被他人领用或状态不正常");
    ErrorCode ASSET_CHECKOUT_DELETE_FAIL_STATUS_ERROR = new ErrorCode(1_030_704_002, "资产领用记录删除失败，只有待审批状态才能删除");
    ErrorCode ASSET_CHECKOUT_APPROVE_FAIL_STATUS_ERROR = new ErrorCode(1_030_704_003, "资产领用审批失败，只有待审批状态才能审批");
    ErrorCode ASSET_RETURN_FAIL_STATUS_ERROR = new ErrorCode(1_030_704_004, "资产归还失败，只有领用中状态才能归还");
    ErrorCode USER_NOT_EXISTS = new ErrorCode(1_030_704_005, "用户不存在");
    ErrorCode DEPT_NOT_EXISTS = new ErrorCode(1_030_704_006, "部门不存在");

    // ========== ERP 资产归还 1-030-705-000 ==========
    ErrorCode ASSET_RETURN_NOT_EXISTS = new ErrorCode(1_030_705_000, "资产归还记录不存在");
    ErrorCode ASSET_RETURN_ALREADY_EXISTS = new ErrorCode(1_030_705_001, "该领用记录已存在归还记录");
    ErrorCode ASSET_RETURN_DELETE_FAIL_STATUS_ERROR = new ErrorCode(1_030_705_002, "资产归还记录删除失败，只有已归还状态才能删除");
    ErrorCode ASSET_RETURN_RECEIVE_FAIL_STATUS_ERROR = new ErrorCode(1_030_705_003, "资产归还接收失败，只有已归还状态才能接收确认");

    // ========== ERP 资产借用 1-030-706-000 ==========
    ErrorCode ASSET_BORROW_NOT_EXISTS = new ErrorCode(1_030_706_000, "资产借用记录不存在");
    ErrorCode ASSET_BORROW_NOT_AVAILABLE = new ErrorCode(1_030_706_001, "资产不可借用，可能已被他人借用或状态不正常");
    ErrorCode ASSET_BORROW_DELETE_FAIL_STATUS_ERROR = new ErrorCode(1_030_706_002, "资产借用记录删除失败，只有申请中状态才能删除");
    ErrorCode ASSET_BORROW_APPROVE_FAIL_STATUS_ERROR = new ErrorCode(1_030_706_003, "资产借用审批失败，只有申请中状态才能审批");
    ErrorCode ASSET_BORROW_RETURN_FAIL_STATUS_ERROR = new ErrorCode(1_030_706_004, "资产归还失败，只有借用中状态才能归还");

    // ========== ERP 资产报废 1-030-707-000 ==========
    ErrorCode ASSET_SCRAPPED_NOT_EXISTS = new ErrorCode(1_030_707_000, "资产报废记录不存在");
    ErrorCode ASSET_ALREADY_SCRAPPED = new ErrorCode(1_030_707_001, "资产已报废，不能重复报废");
    ErrorCode ASSET_SCRAPPED_NOT_APPROVED = new ErrorCode(1_030_707_002, "资产报废未审批通过，无法完成处理");
    ErrorCode ASSET_SCRAPPED_DELETE_FAIL_STATUS_ERROR = new ErrorCode(1_030_707_003, "资产报废记录删除失败，只有申请中状态才能删除");
    ErrorCode ASSET_SCRAPPED_APPROVE_FAIL_STATUS_ERROR = new ErrorCode(1_030_707_004, "资产报废审批失败，只有申请中状态才能审批");

    // ========== ERP 资产挂失 1-030-708-000 ==========
    ErrorCode ASSET_LOST_NOT_EXISTS = new ErrorCode(1_030_708_000, "资产挂失记录不存在");
    ErrorCode ASSET_LOST_NOT_PENDING = new ErrorCode(1_030_708_001, "资产挂失记录不是申请中状态，无法操作");
    ErrorCode ASSET_LOST_NOT_APPROVED = new ErrorCode(1_030_708_002, "资产挂失未审批通过，无法处理");
    ErrorCode ASSET_LOST_DELETE_FAIL_STATUS_ERROR = new ErrorCode(1_030_708_003, "资产挂失记录删除失败，只有申请中状态才能删除");
    ErrorCode ASSET_LOST_APPROVE_FAIL_STATUS_ERROR = new ErrorCode(1_030_708_004, "资产挂失审批失败，只有申请中状态才能审批");

    // ========== ERP 资产录入申请 1-030-709-000 ==========
    ErrorCode ASSET_INPUT_NOT_EXISTS = new ErrorCode(1_030_709_000, "资产录入申请不存在");
    ErrorCode ASSET_INPUT_ASSET_NO_DUPLICATE = new ErrorCode(1_030_709_001, "资产编码({})已在申请中，请勿重复申请");
    ErrorCode ASSET_INPUT_DELETE_FAIL_STATUS_ERROR = new ErrorCode(1_030_709_002, "资产录入申请删除失败，只有待审批状态才能删除");
    ErrorCode ASSET_INPUT_APPROVE_FAIL_STATUS_ERROR = new ErrorCode(1_030_709_003, "资产录入申请审批失败，只有待审批状态才能审批");

    // ========== 盘点计划（1-030-709-000） ==========
    ErrorCode INVENTORY_PLAN_NOT_EXISTS = new ErrorCode(1_030_709_000, "盘点计划不存在");
    ErrorCode INVENTORY_PLAN_NAME_EXISTS = new ErrorCode(1_030_709_001, "盘点计划名称已存在");
    ErrorCode INVENTORY_PLAN_STATUS_NOT_DRAFT = new ErrorCode(1_030_709_002, "只有草稿状态的计划才能进行此操作");
    ErrorCode INVENTORY_PLAN_STATUS_NOT_PENDING_APPROVAL = new ErrorCode(1_030_709_003, "只有待审批状态的计划才能进行审批");
    ErrorCode INVENTORY_PLAN_STATUS_NOT_APPROVED = new ErrorCode(1_030_709_004, "只有已审批状态的计划才能开始执行");
    ErrorCode INVENTORY_PLAN_STATUS_NOT_EXECUTING = new ErrorCode(1_030_709_005, "只有执行中状态的计划才能进行此操作");
    ErrorCode INVENTORY_PLAN_STATUS_COMPLETED = new ErrorCode(1_030_709_006, "已完成的计划不能取消");
    ErrorCode INVENTORY_PLAN_TIME_RANGE_INVALID = new ErrorCode(1_030_709_007, "计划时间范围无效：开始时间不能晚于结束时间");
    ErrorCode INVENTORY_PLAN_STATUS_NOT_COMPLETED = new ErrorCode(1_030_709_008, "只有已完成状态的计划才能提交");
    ErrorCode INVENTORY_PLAN_STATUS_NOT_PENDING_AUDIT = new ErrorCode(1_030_709_009, "只有待审核状态的计划才能进行审核");

    // ========== 采购申请 2-004-000-000 ==========
    ErrorCode PURCHASE_REQUEST_NOT_EXISTS = new ErrorCode(2_004_000_001, "采购申请不存在");
    ErrorCode PURCHASE_REQUEST_UPDATE_FAIL_STATUS_NOT_DRAFT = new ErrorCode(2_004_000_002, "采购申请更新失败，只有草稿状态才能修改");
    ErrorCode PURCHASE_REQUEST_DELETE_FAIL_STATUS_NOT_DRAFT = new ErrorCode(2_004_000_003, "采购申请删除失败，只有草稿状态才能删除");
    ErrorCode PURCHASE_REQUEST_SUBMIT_FAIL_STATUS_NOT_DRAFT = new ErrorCode(2_004_000_004, "采购申请提交失败，只有草稿状态才能提交");
    ErrorCode PURCHASE_REQUEST_APPLY_DATE_NOT_NULL = new ErrorCode(2_004_000_005, "申请日期不能为空");
    ErrorCode PURCHASE_REQUEST_REQUIRED_DATE_NOT_NULL = new ErrorCode(2_004_000_006, "需求日期不能为空");
    ErrorCode PURCHASE_REQUEST_APPLY_DATE_INVALID = new ErrorCode(2_004_000_007, "申请日期格式无效，请重新选择");
    ErrorCode PURCHASE_REQUEST_REQUIRED_DATE_INVALID = new ErrorCode(2_004_000_008, "需求日期格式无效，请重新选择");
    ErrorCode PURCHASE_REQUEST_REQUIRED_DATE_BEFORE_APPLY_DATE = new ErrorCode(2_004_000_009, "需求日期不能早于申请日期");

    // ========== ERP 打印模版（1-030-800-000） ==========
    ErrorCode PRINT_TEMPLATE_NOT_EXISTS = new ErrorCode(1_030_800_000, "打印模版不存在");
    ErrorCode PRINT_TEMPLATE_NAME_DUPLICATE = new ErrorCode(1_030_800_001, "已经存在该名称的打印模版");
    ErrorCode ASSET_INVENTORY_RECORD_NOT_EXISTS = new ErrorCode(1_030_800_002, "资产盘点记录不存在");

}
