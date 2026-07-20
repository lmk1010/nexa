package com.kyx.service.erp.dal.redis.no;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.kyx.service.erp.dal.redis.RedisKeyConstants;
import javax.annotation.Resource;

import com.kyx.service.erp.dal.dataobject.finance.ErpFinancePaymentDO;
import com.kyx.service.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import com.kyx.service.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import com.kyx.service.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import com.kyx.service.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import com.kyx.service.erp.dal.dataobject.sale.ErpSaleOrderDO;
import com.kyx.service.erp.dal.dataobject.sale.ErpSaleOutDO;
import com.kyx.service.erp.dal.dataobject.sale.ErpSaleReturnDO;
import com.kyx.service.erp.dal.dataobject.stock.ErpStockCheckDO;
import com.kyx.service.erp.dal.dataobject.stock.ErpStockInDO;
import com.kyx.service.erp.dal.dataobject.stock.ErpStockMoveDO;
import com.kyx.service.erp.dal.dataobject.stock.ErpStockOutDO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;


/**
 * Erp 订单序号的 Redis DAO
 *
 * @author HUIHUI
 */
@Repository
public class ErpNoRedisDAO {

    /**
     * 其它入库 {@link ErpStockInDO}
     */
    public static final String STOCK_IN_NO_PREFIX = "QTRK";
    /**
     * 其它出库 {@link ErpStockOutDO}
     */
    public static final String STOCK_OUT_NO_PREFIX = "QCKD";

    /**
     * 库存调拨 {@link ErpStockMoveDO}
     */
    public static final String STOCK_MOVE_NO_PREFIX = "QCDB";

    /**
     * 库存盘点 {@link ErpStockCheckDO}
     */
    public static final String STOCK_CHECK_NO_PREFIX = "QCPD";

    /**
     * 销售订单 {@link ErpSaleOrderDO}
     */
    public static final String SALE_ORDER_NO_PREFIX = "XSDD";
    /**
     * 销售出库 {@link ErpSaleOutDO}
     */
    public static final String SALE_OUT_NO_PREFIX = "XSCK";
    /**
     * 销售退货 {@link ErpSaleReturnDO}
     */
    public static final String SALE_RETURN_NO_PREFIX = "XSTH";

    /**
     * 采购订单 {@link ErpPurchaseOrderDO}
     */
    public static final String PURCHASE_ORDER_NO_PREFIX = "CGDD";
    /**
     * 采购入库 {@link ErpPurchaseInDO}
     */
    public static final String PURCHASE_IN_NO_PREFIX = "CGRK";
    /**
     * 采购退货 {@link ErpPurchaseReturnDO}
     */
    public static final String PURCHASE_RETURN_NO_PREFIX = "CGTH";
    /**
     * 采购申请 {@link com.kyx.service.erp.dal.dataobject.purchase.ErpPurchaseRequestDO}
     */
    public static final String PURCHASE_REQUEST_NO_PREFIX = "PR";
    /**
     * 资产转移 {@link com.kyx.service.erp.dal.dataobject.asset.ErpAssetTransferDO}
     */
    public static final String ASSET_TRANSFER_NO_PREFIX = "TR";
    /**
     * 资产调拨 {@link com.kyx.service.erp.dal.dataobject.asset.ErpAssetRedistributionDO}
     */
    public static final String ASSET_REDISTRIBUTION_NO_PREFIX = "RD";

    /**
     * 付款单 {@link ErpFinancePaymentDO}
     */
    public static final String FINANCE_PAYMENT_NO_PREFIX = "FKD";
    /**
     * 收款单 {@link ErpFinanceReceiptDO}
     */
    public static final String FINANCE_RECEIPT_NO_PREFIX = "SKD";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 生成序号，使用当前日期，格式为 {PREFIX} + yyyyMMdd + 6 位自增
     * 例如说：QTRK 202109 000001 （没有中间空格）
     *
     * @param prefix 前缀
     * @return 序号
     */
    public String generate(String prefix) {
        // 递增序号
        String noPrefix = prefix + DateUtil.format(LocalDateTime.now(), DatePattern.PURE_DATE_PATTERN);
        String key = RedisKeyConstants.NO + noPrefix;
        Long no = stringRedisTemplate.opsForValue().increment(key);
        // 设置过期时间
        stringRedisTemplate.expire(key, Duration.ofDays(1L));
        return noPrefix + String.format("%06d", no);
    }

}
