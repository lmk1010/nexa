package com.kyx.service.hr.controller.admin.sequence;

import com.kyx.foundation.apilog.core.annotation.ApiAccessLog;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.excel.core.util.ExcelUtils;
import com.kyx.service.hr.controller.admin.sequence.vo.SequenceOptionVO;
import com.kyx.service.hr.controller.admin.sequence.vo.SequencePageReqVO;
import com.kyx.service.hr.controller.admin.sequence.vo.SequenceRespVO;
import com.kyx.service.hr.controller.admin.sequence.vo.SequenceSaveReqVO;
import com.kyx.service.hr.dal.dataobject.sequence.SequenceDO;
import com.kyx.service.hr.service.sequence.SequenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

import static com.kyx.foundation.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.kyx.foundation.common.pojo.CommonResult.success;

/**
 * 序列管理 Controller
 *
 * @author MK
 */
@Tag(name = "管理后台 - 序列管理")
@RestController
@RequestMapping("/hr/sequence")
@Validated
public class SequenceController {

    @Resource
    private SequenceService sequenceService;

    @PostMapping("/create")
    @Operation(summary = "创建序列")
    @PreAuthorize("@ss.hasPermission('hr:sequence:create')")
    public CommonResult<Long> createSequence(@Valid @RequestBody SequenceSaveReqVO createReqVO) {
        return success(sequenceService.createSequence(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新序列")
    @PreAuthorize("@ss.hasPermission('hr:sequence:update')")
    public CommonResult<Boolean> updateSequence(@Valid @RequestBody SequenceSaveReqVO updateReqVO) {
        sequenceService.updateSequence(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除序列")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:sequence:delete')")
    public CommonResult<Boolean> deleteSequence(@RequestParam("id") Long id) {
        sequenceService.deleteSequence(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得序列")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:sequence:query')")
    public CommonResult<SequenceRespVO> getSequence(@RequestParam("id") Long id) {
        SequenceDO sequence = sequenceService.getSequence(id);
        return success(BeanUtils.toBean(sequence, SequenceRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得序列分页")
    @PreAuthorize("@ss.hasPermission('hr:sequence:query')")
    public CommonResult<PageResult<SequenceRespVO>> getSequencePage(@Valid SequencePageReqVO pageReqVO) {
        PageResult<SequenceRespVO> pageResult = sequenceService.getSequencePage(pageReqVO);
        return success(pageResult);
    }

    @GetMapping("/tree")
    @Operation(summary = "获得序列树形列表")
    @PreAuthorize("@ss.hasPermission('hr:sequence:query')")
    public CommonResult<List<SequenceRespVO>> getSequenceTree() {
        List<SequenceRespVO> list = sequenceService.getSequenceTree();
        return success(list);
    }

    @GetMapping("/options")
    @Operation(summary = "获得序列选项列表")
    public CommonResult<List<SequenceOptionVO>> getSequenceOptions() {
        List<SequenceOptionVO> list = sequenceService.getSequenceOptions();
        return success(list);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出序列 Excel")
    @PreAuthorize("@ss.hasPermission('hr:sequence:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSequenceExcel(@Valid SequencePageReqVO pageReqVO,
                                   HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(10000);
        List<SequenceRespVO> list = sequenceService.getSequencePage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "序列管理.xls", "数据", SequenceRespVO.class, list);
    }

} 