package com.kyx.service.op.service.file;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.op.controller.admin.file.vo.file.FileCreateReqVO;
import com.kyx.service.op.controller.admin.file.vo.file.FilePageReqVO;
import com.kyx.service.op.controller.admin.file.vo.file.FilePresignedUrlRespVO;
import com.kyx.service.op.dal.dataobject.file.FileDO;

import javax.validation.constraints.NotEmpty;

/**
 * 文件 Service 接口
 *
 * @author MK
 */
public interface FileService {

    /**
     * 获得文件分页
     *
     * @param pageReqVO 分页查询
     * @return 文件分页
     */
    PageResult<FileDO> getFilePage(FilePageReqVO pageReqVO);

    /**
     * 保存文件，并返回文件的访问标识
     *
     * @param content   文件内容
     * @param name      文件名称，允许空
     * @param directory 目录，允许空
     * @param type      文件的 MIME 类型，允许空
     * @return 文件ID（UUID）
     */
    String createFile(@NotEmpty(message = "文件内容不能为空") byte[] content,
                      String name, String directory, String type);

    /**
     * 生成文件预签名地址信息
     *
     * @param name      文件名
     * @param directory 目录
     * @return 预签名地址信息
     */
    FilePresignedUrlRespVO getFilePresignedUrl(@NotEmpty(message = "文件名不能为空") String name,
                                               String directory);

    /**
     * 创建文件
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createFile(FileCreateReqVO createReqVO);

    /**
     * 删除文件
     *
     * @param id 编号
     */
    void deleteFile(Long id) throws Exception;

    /**
     * 通过文件ID获得文件内容
     *
     * @param fileId 文件唯一标识（UUID）
     * @return 文件内容
     */
    byte[] getFileContentByFileId(String fileId) throws Exception;

    /**
     * 通过文件ID获得文件信息
     *
     * @param fileId 文件唯一标识（UUID）
     * @return 文件信息
     */
    FileDO getFileByFileId(String fileId);

    /**
     * 获得文件内容
     *
     * @param configId 配置编号
     * @param path     文件路径
     * @return 文件内容
     */
    byte[] getFileContent(Long configId, String path) throws Exception;

}
