package com.kyx.service.erp.service.asset;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.erp.controller.admin.asset.vo.attachment.ErpAssetAttachmentBatchUploadReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.attachment.ErpAssetAttachmentBatchUploadRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.attachment.ErpAssetAttachmentPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.attachment.ErpAssetAttachmentSaveReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.attachment.ErpAssetAttachmentUploadReqVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetAttachmentDO;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

/**
 * ERP 资产附件 Service 接口
 *
 * @author kyx
 */
public interface ErpAssetAttachmentService {

    /**
     * 创建资产附件
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createAssetAttachment(@Valid ErpAssetAttachmentSaveReqVO createReqVO);

    /**
     * 上传资产附件文件
     *
     * @param uploadReqVO 上传信息
     * @return 附件编号
     */
    Long uploadAssetAttachment(@Valid ErpAssetAttachmentUploadReqVO uploadReqVO) throws Exception;

    /**
     * 批量上传资产附件文件
     *
     * @param batchUploadReqVO 批量上传信息
     * @return 批量上传结果
     */
    ErpAssetAttachmentBatchUploadRespVO batchUploadAssetAttachment(@Valid ErpAssetAttachmentBatchUploadReqVO batchUploadReqVO);

    /**
     * 更新资产附件
     *
     * @param updateReqVO 更新信息
     */
    void updateAssetAttachment(@Valid ErpAssetAttachmentSaveReqVO updateReqVO);

    /**
     * 删除资产附件
     *
     * @param id 编号
     */
    void deleteAssetAttachment(Long id);

    /**
     * 获得资产附件
     *
     * @param id 编号
     * @return 资产附件
     */
    ErpAssetAttachmentDO getAssetAttachment(Long id);

    /**
     * 根据资产ID获取附件列表
     *
     * @param assetId 资产ID
     * @return 附件列表
     */
    List<ErpAssetAttachmentDO> getAssetAttachmentListByAssetId(Long assetId);

    /**
     * 分页查询资产附件
     *
     * @param pageReqVO 分页查询参数
     * @return 分页结果
     */
    PageResult<ErpAssetAttachmentDO> getAssetAttachmentPage(ErpAssetAttachmentPageReqVO pageReqVO);

    /**
     * 批量删除资产的所有附件
     *
     * @param assetId 资产ID
     */
    void deleteAssetAttachmentsByAssetId(Long assetId);

    /**
     * 校验资产附件是否存在
     *
     * @param id 编号
     * @return 资产附件
     */
    ErpAssetAttachmentDO validateAssetAttachmentExists(Long id);

} 