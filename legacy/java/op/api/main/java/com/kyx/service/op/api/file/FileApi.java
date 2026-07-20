package com.kyx.service.op.api.file;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.service.op.api.file.dto.FileCreateReqDTO;
import com.kyx.service.op.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

@FeignClient(name = ApiConstants.NAME) // TODO ：fallbackFactory =
@Tag(name = "RPC 服务 - 文件")
public interface FileApi {

    String PREFIX = ApiConstants.PREFIX + "/file";

    /**
     * 保存文件，并返回文件的访问路径
     *
     * @param content 文件内容
     * @return 文件路径
     */
    default String createFile(byte[] content) {
        return createFile(content, null, null, null);
    }

    /**
     * 保存文件，并返回文件的访问路径
     *
     * @param content 文件内容
     * @param name 文件名称，允许空
     * @return 文件路径
     */
    default String createFile(byte[] content, String name) {
        return createFile(content, name, null, null);
    }

    /**
     * 保存文件，并返回文件的访问路径
     *
     * @param content 文件内容
     * @param name 文件名称，允许空
     * @param directory 目录，允许空
     * @param type 文件的 MIME 类型，允许空
     * @return 文件路径
     */
    default String createFile(@NotEmpty(message = "文件内容不能为空") byte[] content,
                              String name, String directory, String type) {
        CommonResult<String> result = createFile(FileCreateReqDTO.builder().name(name).directory(directory).type(type).content(content).build());
        return result.getCheckedData();
    }

    /**
     * 根据文件ID获取文件信息
     *
     * @param fileId 文件唯一标识
     * @return 文件信息
     */
    @PostMapping(PREFIX + "/get-by-file-id")
    @Operation(summary = "根据文件ID获取文件信息")
    CommonResult<FileCreateReqDTO> getFileByFileId(@RequestParam("fileId") String fileId);

    @PostMapping(PREFIX + "/get-content-by-file-id")
    @Operation(summary = "根据文件ID获取文件内容")
    CommonResult<byte[]> getFileContentByFileId(@RequestParam("fileId") String fileId);

    @PostMapping(PREFIX + "/create")
    @Operation(summary = "创建文件")
    CommonResult<String> createFile(@Valid @RequestBody FileCreateReqDTO createReqDTO);

}
