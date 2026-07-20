package com.kyx.service.erp.service.asset;

import com.kyx.service.erp.dal.dataobject.asset.ErpAssetScrappedFileDO;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetScrappedFileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ERP 资产报废文件关联 Service 实现类
 *
 * @author kyx
 */
@Service
public class ErpAssetScrappedFileServiceImpl implements ErpAssetScrappedFileService {

    @Resource
    private ErpAssetScrappedFileMapper scrappedFileMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveScrappedFiles(Long scrappedId, List<String> fileIds) {
        if (CollectionUtils.isEmpty(fileIds)) {
            return;
        }

        List<ErpAssetScrappedFileDO> fileList = fileIds.stream()
                .map(fileId -> ErpAssetScrappedFileDO.builder()
                        .scrappedId(scrappedId)
                        .fileId(fileId)
                        .build())
                .collect(Collectors.toList());

        scrappedFileMapper.insertBatch(fileList);
    }

    @Override
    public List<ErpAssetScrappedFileDO> getScrappedFiles(Long scrappedId) {
        return scrappedFileMapper.selectListByScrappedId(scrappedId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteScrappedFiles(Long scrappedId) {
        scrappedFileMapper.deleteByScrappedId(scrappedId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateScrappedFiles(Long scrappedId, List<String> fileIds) {
        // 先删除原有的文件关联
        deleteScrappedFiles(scrappedId);
        
        // 保存新的文件关联
        saveScrappedFiles(scrappedId, fileIds);
    }
} 