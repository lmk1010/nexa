package com.kyx.service.erp.service.asset;

import com.kyx.service.erp.dal.dataobject.asset.ErpAssetLostFileDO;

import java.util.List;

/**
 * ERP 资产挂失文件 Service 接口
 *
 * @author kyx
 */
public interface ErpAssetLostFileService {

    /**
     * 保存挂失记录的文件关联
     *
     * @param lostId  挂失记录编号
     * @param fileIds 文件ID列表
     */
    void saveLostFiles(Long lostId, List<String> fileIds);

    /**
     * 获取挂失记录的文件列表
     *
     * @param lostId 挂失记录编号
     * @return 文件列表
     */
    List<ErpAssetLostFileDO> getLostFiles(Long lostId);

    /**
     * 删除挂失记录的文件关联
     *
     * @param lostId 挂失记录编号
     */
    void deleteLostFiles(Long lostId);
} 