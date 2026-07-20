package com.kyx.service.erp.service.asset;

import com.kyx.service.erp.dal.dataobject.asset.ErpAssetLostFileDO;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetLostFileMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * ERP 资产挂失文件 Service 实现类
 *
 * @author kyx
 */
@Slf4j
@Service
public class ErpAssetLostFileServiceImpl implements ErpAssetLostFileService {

    @Resource
    private ErpAssetLostFileMapper lostFileMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveLostFiles(Long lostId, List<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }

        for (String fileId : fileIds) {
            ErpAssetLostFileDO fileDO = ErpAssetLostFileDO.builder()
                    .lostId(lostId)
                    .fileId(fileId)
                    .fileName("未知文件名")  // TODO: 从OP服务获取文件信息
                    .fileSize(0L)
                    .fileType("unknown")
                    .build();
            lostFileMapper.insert(fileDO);
        }

        log.info("保存挂失记录文件关联成功，挂失ID: {}, 文件数量: {}", lostId, fileIds.size());
    }

    @Override
    public List<ErpAssetLostFileDO> getLostFiles(Long lostId) {
        return lostFileMapper.selectByLostId(lostId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLostFiles(Long lostId) {
        lostFileMapper.deleteByLostId(lostId);
        log.info("删除挂失记录文件关联成功，挂失ID: {}", lostId);
    }
} 