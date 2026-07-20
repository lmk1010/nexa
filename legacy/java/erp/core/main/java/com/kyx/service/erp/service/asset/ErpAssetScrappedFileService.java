package com.kyx.service.erp.service.asset;

import com.kyx.service.erp.dal.dataobject.asset.ErpAssetScrappedFileDO;

import java.util.List;

/**
 * ERP 资产报废文件关联 Service 接口
 *
 * @author kyx
 */
public interface ErpAssetScrappedFileService {

    /**
     * 保存资产报废文件关联
     *
     * @param scrappedId 资产报废ID
     * @param fileIds 文件ID列表
     */
    void saveScrappedFiles(Long scrappedId, List<String> fileIds);

    /**
     * 获取资产报废的文件列表
     *
     * @param scrappedId 资产报废ID
     * @return 文件关联列表
     */
    List<ErpAssetScrappedFileDO> getScrappedFiles(Long scrappedId);

    /**
     * 删除资产报废的所有文件关联
     *
     * @param scrappedId 资产报废ID
     */
    void deleteScrappedFiles(Long scrappedId);

    /**
     * 更新资产报废文件关联
     *
     * @param scrappedId 资产报废ID
     * @param fileIds 新的文件ID列表
     */
    void updateScrappedFiles(Long scrappedId, List<String> fileIds);
} 