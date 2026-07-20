package com.kyx.service.op.api.file;

import com.kyx.foundation.common.exception.ServiceException;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.op.api.file.dto.FileCreateReqDTO;
import com.kyx.service.op.dal.dataobject.file.FileDO;
import com.kyx.service.op.service.file.FileService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

import static com.kyx.foundation.common.pojo.CommonResult.error;
import static com.kyx.foundation.common.pojo.CommonResult.success;

@RestController // 提供 RESTful API 接口，给 Feign 调用
@Validated
public class FileApiImpl implements FileApi {

    @Resource
    private FileService fileService;

    @Override
    public CommonResult<String> createFile(FileCreateReqDTO createReqDTO) {
        return success(fileService.createFile(createReqDTO.getContent(), createReqDTO.getName(),
                createReqDTO.getDirectory(), createReqDTO.getType()));
    }

    @Override
    public CommonResult<FileCreateReqDTO> getFileByFileId(String fileId) {
        FileDO fileDO = fileService.getFileByFileId(fileId);
        if (fileDO == null) {
            return success(null);
        }
        FileCreateReqDTO result = FileCreateReqDTO.builder()
                .name(fileDO.getName())
                .type(fileDO.getType())
                .size(fileDO.getSize())
                .build();
        return success(result);
    }

    @Override
    public CommonResult<byte[]> getFileContentByFileId(String fileId) {
        try {
            return success(fileService.getFileContentByFileId(fileId));
        } catch (ServiceException e) {
            return error(e);
        } catch (Exception e) {
            return error(500, "读取文件内容失败：" + e.getMessage());
        }
    }

}
