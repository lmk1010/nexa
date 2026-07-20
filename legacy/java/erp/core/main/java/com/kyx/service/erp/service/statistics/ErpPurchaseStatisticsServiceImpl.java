package com.kyx.service.erp.service.statistics;

import com.kyx.service.erp.dal.mysql.statistics.ErpPurchaseStatisticsMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 采购统计 Service 实现类
 *
 * @author MK
 */
@Service
public class ErpPurchaseStatisticsServiceImpl implements ErpPurchaseStatisticsService {

    @Resource
    private ErpPurchaseStatisticsMapper purchaseStatisticsMapper;

    @Override
    public BigDecimal getPurchasePrice(LocalDateTime beginTime, LocalDateTime endTime) {
        return purchaseStatisticsMapper.getPurchasePrice(beginTime, endTime);
    }

}
