package com.kyx.service.erp.service.asset;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.common.util.servlet.ServletUtils;
import com.kyx.foundation.web.core.util.WebFrameworkUtils;
import com.kyx.service.erp.controller.admin.asset.vo.barcode.*;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetBarcodePrintDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetBarcodePrintLogDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetDO;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetBarcodePrintMapper;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetBarcodePrintLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.foundation.common.util.collection.CollectionUtils.convertMap;
import static com.kyx.service.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 资产条码打印 Service 实现类
 *
 * @author kyx
 */
@Service
@Validated
public class ErpAssetBarcodePrintServiceImpl implements ErpAssetBarcodePrintService {

    @Resource
    private ErpAssetBarcodePrintMapper assetBarcodePrintMapper;
    
    @Resource
    private ErpAssetBarcodePrintLogMapper assetBarcodePrintLogMapper;
    
    @Resource
    private ErpAssetService assetService;

    @Override
    @Transactional
    public Long createAssetBarcodePrint(ErpAssetBarcodePrintSaveReqVO createReqVO) {
        // 校验资产存在
        assetService.getAsset(createReqVO.getAssetId());
        
        // 校验条码编号唯一性
        validateBarcodeNoUnique(null, createReqVO.getBarcodeNo());
        
        // 插入
        ErpAssetBarcodePrintDO assetBarcodePrint = BeanUtils.toBean(createReqVO, ErpAssetBarcodePrintDO.class);
        assetBarcodePrint.setPrintCount(0);
        assetBarcodePrintMapper.insert(assetBarcodePrint);
        
        // 记录日志
        recordLog(assetBarcodePrint.getId(), assetBarcodePrint.getAssetId(), assetBarcodePrint.getAssetNo(), 
                 assetBarcodePrint.getBarcodeNo(), 1, 0, 0, null, null, 1, null, "生成条码");
        
        return assetBarcodePrint.getId();
    }

    @Override
    public void updateAssetBarcodePrint(ErpAssetBarcodePrintSaveReqVO updateReqVO) {
        // 校验存在
        validateAssetBarcodePrintExists(updateReqVO.getId());
        
        // 校验条码编号唯一性
        validateBarcodeNoUnique(updateReqVO.getId(), updateReqVO.getBarcodeNo());
        
        // 更新
        ErpAssetBarcodePrintDO updateObj = BeanUtils.toBean(updateReqVO, ErpAssetBarcodePrintDO.class);
        assetBarcodePrintMapper.updateById(updateObj);
    }

    @Override
    public void deleteAssetBarcodePrint(Long id) {
        // 校验存在
        validateAssetBarcodePrintExists(id);
        
        // 删除
        assetBarcodePrintMapper.deleteById(id);
    }

    private void validateAssetBarcodePrintExists(Long id) {
        if (assetBarcodePrintMapper.selectById(id) == null) {
            throw exception(ASSET_NOT_EXISTS);
        }
    }

    private void validateBarcodeNoUnique(Long id, String barcodeNo) {
        ErpAssetBarcodePrintDO barcodePrint = assetBarcodePrintMapper.selectByBarcodeNo(barcodeNo);
        if (barcodePrint == null) {
            return;
        }
        if (id == null || !id.equals(barcodePrint.getId())) {
            throw exception(ASSET_NO_DUPLICATE, barcodeNo);
        }
    }

    @Override
    public ErpAssetBarcodePrintDO getAssetBarcodePrint(Long id) {
        return assetBarcodePrintMapper.selectById(id);
    }

    @Override
    public List<ErpAssetBarcodePrintDO> getAssetBarcodePrintList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return assetBarcodePrintMapper.selectBatchIds(ids);
    }

    @Override
    public PageResult<ErpAssetBarcodePrintRespVO> getAssetBarcodePrintPage(ErpAssetBarcodePrintPageReqVO pageReqVO) {
        return getAssetBarcodePrintVOPage(pageReqVO);
    }

    @Override
    public PageResult<ErpAssetBarcodePrintRespVO> getAssetBarcodePrintVOPage(ErpAssetBarcodePrintPageReqVO pageReqVO) {
        PageResult<ErpAssetBarcodePrintDO> pageResult = assetBarcodePrintMapper.selectPage(pageReqVO);
        return buildAssetBarcodePrintVOPageResult(pageResult);
    }

    private PageResult<ErpAssetBarcodePrintRespVO> buildAssetBarcodePrintVOPageResult(PageResult<ErpAssetBarcodePrintDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        
        // 1. 转换 VO
        List<ErpAssetBarcodePrintRespVO> voList = BeanUtils.toBean(pageResult.getList(), ErpAssetBarcodePrintRespVO.class);
        
        // 2. 获取资产信息
        Set<Long> assetIds = pageResult.getList().stream()
                .map(ErpAssetBarcodePrintDO::getAssetId)
                .collect(Collectors.toSet());
        List<ErpAssetDO> assetList = assetService.validAssetList(assetIds);
        Map<Long, ErpAssetDO> assetMap = convertMap(assetList, ErpAssetDO::getId);
        
        // 3. 填充数据
        voList.forEach(vo -> {
            ErpAssetDO asset = assetMap.get(vo.getAssetId());
            if (asset != null) {
                vo.setAssetName(asset.getName());
            }
            
            // 填充条码类型名称
            vo.setBarcodeTypeName(getBarcodeTypeName(vo.getBarcodeType()));
            
            // 填充状态名称
            vo.setStatusName(getStatusName(vo.getStatus()));
        });
        
        return new PageResult<>(voList, pageResult.getTotal());
    }

    private String getBarcodeTypeName(Integer barcodeType) {
        if (barcodeType == null) {
            return "未知";
        }
        switch (barcodeType) {
            case 1: return "一维码";
            case 2: return "二维码";
            default: return "未知";
        }
    }

    private String getStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 1: return "正常";
            case 2: return "失效";
            case 3: return "已作废";
            default: return "未知";
        }
    }

    @Override
    @Transactional
    public int generateAssetBarcodes(ErpAssetBarcodeGenerateReqVO generateReqVO) {
        // 1. 校验资产是否存在
        List<ErpAssetDO> assetList = assetService.validAssetList(generateReqVO.getAssetIds());
        if (assetList.size() != generateReqVO.getAssetIds().size()) {
            throw exception(ASSET_NOT_EXISTS);
        }
        
        // 2. 生成条码记录
        int count = 0;
        String barcodePrefix = StrUtil.isNotBlank(generateReqVO.getBarcodePrefix()) ? generateReqVO.getBarcodePrefix() : "BC";
        String serialPrefix = StrUtil.isNotBlank(generateReqVO.getSerialPrefix()) ? generateReqVO.getSerialPrefix() : "SN";
        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        for (ErpAssetDO asset : assetList) {
            // 检查是否已经生成过条码
            List<ErpAssetBarcodePrintDO> existingBarcodes = assetBarcodePrintMapper.selectListByAssetId(asset.getId());
            if (CollUtil.isNotEmpty(existingBarcodes)) {
                continue; // 跳过已经生成过条码的资产
            }
            
            // 生成条码编号和发放序号
            String barcodeNo = generateBarcodeNo(barcodePrefix, asset.getId());
            String serialNo = generateSerialNo(serialPrefix, currentDate, count + 1);
            
            // 生成条码内容
            String barcodeContent = generateBarcodeContent(asset, generateReqVO.getBarcodeType());
            
            ErpAssetBarcodePrintDO barcodePrint = ErpAssetBarcodePrintDO.builder()
                    .assetId(asset.getId())
                    .assetNo(asset.getAssetNo())
                    .barcodeNo(barcodeNo)
                    .printSerialNo(serialNo)
                    .issueDate(LocalDateTime.now())
                    .barcodeType(generateReqVO.getBarcodeType())
                    .barcodeContent(barcodeContent)
                    .printCount(0)
                    .status(1)
                    .remark(generateReqVO.getRemark())
                    .build();
            
            assetBarcodePrintMapper.insert(barcodePrint);
            
            // 记录日志
            recordLog(barcodePrint.getId(), asset.getId(), asset.getAssetNo(), barcodeNo, 1, 0, 0, 
                     null, null, 1, null, "批量生成条码");
            
            count++;
        }
        
        return count;
    }

    private String generateBarcodeNo(String prefix, Long assetId) {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        return prefix + timestamp.substring(timestamp.length() - 6) + String.format("%03d", assetId % 1000);
    }

    private String generateSerialNo(String prefix, String date, int sequence) {
        return prefix + date + String.format("%03d", sequence);
    }

    private String generateBarcodeContent(ErpAssetDO asset, Integer barcodeType) {
        if (barcodeType == 2) {
            // 二维码内容为JSON格式
            return String.format("{\"id\":%d,\"assetNo\":\"%s\",\"name\":\"%s\",\"type\":\"%s\",\"timestamp\":\"%s\"}", 
                    asset.getId(), asset.getAssetNo(), asset.getName(), asset.getType(), LocalDateTime.now());
        } else {
            // 一维码内容为资产编码
            return asset.getAssetNo();
        }
    }

    @Override
    @Transactional
    public boolean printAssetBarcodes(ErpAssetBarcodePrintReqVO printReqVO) {
        try {
            List<ErpAssetBarcodePrintDO> barcodePrintList = assetBarcodePrintMapper.selectBatchIds(printReqVO.getBarcodePrintIds());
            if (barcodePrintList.size() != printReqVO.getBarcodePrintIds().size()) {
                throw exception(ASSET_NOT_EXISTS);
            }
            
            // 更新打印次数和最后打印时间
            LocalDateTime now = LocalDateTime.now();
            for (ErpAssetBarcodePrintDO barcodePrint : barcodePrintList) {
                barcodePrint.setPrintCount(barcodePrint.getPrintCount() + printReqVO.getPrintCount());
                barcodePrint.setLastPrintTime(now);
                assetBarcodePrintMapper.updateById(barcodePrint);
                
                // 记录打印日志
                recordLog(barcodePrint.getId(), barcodePrint.getAssetId(), barcodePrint.getAssetNo(), 
                         barcodePrint.getBarcodeNo(), 2, printReqVO.getPrintCount(), 
                         barcodePrint.getPrintCount(), printReqVO.getPrinterName(), 
                         WebFrameworkUtils.getLoginUserId(), 1, null, printReqVO.getRemark());
            }
            
            return true;
        } catch (Exception e) {
            // 记录失败日志
            for (Long barcodePrintId : printReqVO.getBarcodePrintIds()) {
                ErpAssetBarcodePrintDO barcodePrint = assetBarcodePrintMapper.selectById(barcodePrintId);
                if (barcodePrint != null) {
                    recordLog(barcodePrint.getId(), barcodePrint.getAssetId(), barcodePrint.getAssetNo(), 
                             barcodePrint.getBarcodeNo(), 2, printReqVO.getPrintCount(), 
                             barcodePrint.getPrintCount(), printReqVO.getPrinterName(), 
                             WebFrameworkUtils.getLoginUserId(), 2, e.getMessage(), printReqVO.getRemark());
                }
            }
            return false;
        }
    }

    @Override
    public List<ErpAssetBarcodePrintDO> getAssetBarcodePrintListByAssetId(Long assetId) {
        return assetBarcodePrintMapper.selectListByAssetId(assetId);
    }

    @Override
    public ErpAssetBarcodePrintDO getAssetBarcodePrintByBarcodeNo(String barcodeNo) {
        return assetBarcodePrintMapper.selectByBarcodeNo(barcodeNo);
    }

    /**
     * 记录操作日志
     */
    private void recordLog(Long barcodePrintId, Long assetId, String assetNo, String barcodeNo, 
                          Integer operationType, Integer printCount, Integer totalPrintCount, 
                          String printerName, Long printUserId, Integer printResult, 
                          String errorMessage, String remark) {
        ErpAssetBarcodePrintLogDO log = ErpAssetBarcodePrintLogDO.builder()
                .barcodePrintId(barcodePrintId)
                .assetId(assetId)
                .assetNo(assetNo)
                .barcodeNo(barcodeNo)
                .operationType(operationType)
                .printCount(printCount)
                .totalPrintCount(totalPrintCount)
                .printerName(printerName)
                .printUserId(printUserId)
                .printResult(printResult)
                .errorMessage(errorMessage)
                .operationIp(ServletUtils.getClientIP())
                .userAgent(ServletUtils.getUserAgent())
                .remark(remark)
                .build();
        
        assetBarcodePrintLogMapper.insert(log);
    }

} 