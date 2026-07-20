package com.kyx.service.hr.controller.admin.employee;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeDocumentRequestApplyReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeDocumentRequestHandleReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeDocumentRequestPageReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeDocumentRequestRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeDocumentTemplatePreviewRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeDocumentTemplateRespVO;
import com.kyx.service.hr.service.employee.EmployeeDocumentRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 员工证明办理")
@RestController
@RequestMapping("/hr/employee/document-request")
@Validated
public class EmployeeDocumentRequestController {

    @Resource
    private EmployeeDocumentRequestService employeeDocumentRequestService;

    @PostMapping("/apply")
    @Operation(summary = "提交证明申请")
    @PreAuthorize("@ss.hasPermission('hr:document-request:apply')")
    public CommonResult<Long> apply(@Valid @RequestBody EmployeeDocumentRequestApplyReqVO reqVO) {
        return success(employeeDocumentRequestService.apply(reqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "获得证明申请分页")
    @PreAuthorize("@ss.hasAnyPermissions('hr:document-request:query,hr:document-request:apply')")
    public CommonResult<PageResult<EmployeeDocumentRequestRespVO>> getPage(@Valid EmployeeDocumentRequestPageReqVO pageReqVO) {
        return success(employeeDocumentRequestService.getPage(pageReqVO));
    }

    @PostMapping("/handle")
    @Operation(summary = "处理证明申请")
    @PreAuthorize("@ss.hasPermission('hr:document-request:handle')")
    public CommonResult<Boolean> handle(@Valid @RequestBody EmployeeDocumentRequestHandleReqVO reqVO) {
        return success(employeeDocumentRequestService.handle(reqVO));
    }

    @PostMapping("/cancel")
    @Operation(summary = "撤销证明申请")
    @PreAuthorize("@ss.hasPermission('hr:document-request:apply')")
    public CommonResult<Boolean> cancel(@RequestParam("id") Long id) {
        return success(employeeDocumentRequestService.cancel(id));
    }

    @GetMapping("/template/list")
    @Operation(summary = "获得证明模板目录")
    @PreAuthorize("@ss.hasAnyPermissions('hr:document-request:query,hr:document-request:apply,hr:document-request:handle')")
    public CommonResult<List<EmployeeDocumentTemplateRespVO>> listTemplates() {
        return success(employeeDocumentRequestService.listTemplates());
    }

    @GetMapping("/template/preview")
    @Operation(summary = "预览证明模板")
    @PreAuthorize("@ss.hasAnyPermissions('hr:document-request:query,hr:document-request:apply,hr:document-request:handle')")
    public CommonResult<EmployeeDocumentTemplatePreviewRespVO> previewTemplate(
            @RequestParam("id") Long id,
            @RequestParam(value = "templateCode", required = false) String templateCode) {
        return success(employeeDocumentRequestService.previewTemplate(id, templateCode));
    }

    @GetMapping("/template/download")
    @Operation(summary = "下载证明模板套打件")
    @PreAuthorize("@ss.hasAnyPermissions('hr:document-request:query,hr:document-request:apply,hr:document-request:handle')")
    public void downloadTemplate(@RequestParam("id") Long id,
                                 @RequestParam(value = "templateCode", required = false) String templateCode,
                                 HttpServletResponse response) throws IOException {
        EmployeeDocumentTemplatePreviewRespVO preview = employeeDocumentRequestService.previewTemplate(id, templateCode);
        byte[] data = employeeDocumentRequestService.exportTemplate(id, templateCode);
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        String filename = URLEncoder.encode(preview.getFileName(), StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + filename);
        response.getOutputStream().write(data);
    }

}
