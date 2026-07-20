package com.kyx.service.hr.controller.admin.exam;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.security.core.service.SecurityFrameworkService;
import com.kyx.service.business.enums.permission.RoleCodeEnum;
import com.kyx.service.hr.controller.admin.exam.vo.ExamPageReqVO;
import com.kyx.service.hr.controller.admin.exam.vo.ExamImportRespVO;
import com.kyx.service.hr.controller.admin.exam.vo.ExamMaterialParseRespVO;
import com.kyx.service.hr.controller.admin.exam.vo.ExamRespVO;
import com.kyx.service.hr.controller.admin.exam.vo.ExamSaveReqVO;
import com.kyx.service.hr.service.exam.ExamExcelParser;
import com.kyx.service.hr.service.exam.ExamService;
import com.kyx.service.hr.service.exam.ExamDocxParser;
import com.kyx.service.hr.service.exam.ExamMaterialParser;
import com.kyx.service.hr.service.exam.ExamViewScopeSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static com.kyx.foundation.common.pojo.CommonResult.success;
import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static com.kyx.service.hr.enums.ErrorCodeConstants.EXAM_IMPORT_FAIL;

/**
 * HR 考试管理 Controller
 *
 * @author MK
 */
@Tag(name = "管理后台 - HR 考试管理")
@RestController
@RequestMapping("/hr/exam")
@Validated
public class ExamController {

    @Resource
    private ExamService examService;
    @Resource
    private SecurityFrameworkService securityFrameworkService;
    @Resource
    private ExamViewScopeSupport examViewScopeSupport;

    @PostMapping("/create")
    @Operation(summary = "创建考试")
    @PreAuthorize("@ss.hasPermission('hr:exam:create')")
    public CommonResult<Long> createExam(@Valid @RequestBody ExamSaveReqVO createReqVO) {
        return success(examService.createExam(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新考试")
    @PreAuthorize("@ss.hasPermission('hr:exam:update')")
    public CommonResult<Boolean> updateExam(@Valid @RequestBody ExamSaveReqVO updateReqVO) {
        examService.updateExam(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除考试")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:exam:delete')")
    public CommonResult<Boolean> deleteExam(@RequestParam("id") Long id) {
        examService.deleteExam(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得考试详情")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:exam:query')")
    public CommonResult<ExamRespVO> getExam(@RequestParam("id") Long id) {
        return success(examService.getExam(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得考试分页")
    @PreAuthorize("@ss.hasPermission('hr:exam:query')")
    public CommonResult<PageResult<ExamRespVO>> getExamPage(@Valid ExamPageReqVO pageVO) {
        if (!canViewAllExamData()) {
            pageVO.setCreator(String.valueOf(getLoginUserId()));
        }
        return success(examService.getExamPage(pageVO));
    }

    @GetMapping("/manage-page")
    @Operation(summary = "获得考试管理分页")
    @PreAuthorize("@ss.hasPermission('hr:exam:query')")
    public CommonResult<PageResult<ExamRespVO>> getManageExamPage(@Valid ExamPageReqVO pageVO) {
        if (!canViewAllExamData()) {
            pageVO.setCreator(String.valueOf(getLoginUserId()));
        }
        return success(examService.getExamPage(pageVO));
    }

    private boolean canViewAllExamData() {
        return securityFrameworkService.hasRole(RoleCodeEnum.SUPER_ADMIN.getCode())
                || securityFrameworkService.hasRole(RoleCodeEnum.TENANT_ADMIN.getCode())
                || examViewScopeSupport.canViewAllData();
    }

    @PostMapping("/import-docx")
    @Operation(summary = "导入考试（docx）")
    @PreAuthorize("@ss.hasPermission('hr:exam:create')")
    public CommonResult<ExamImportRespVO> importDocx(@RequestParam("file") MultipartFile file) {
        try {
            ExamImportRespVO resp = ExamDocxParser.parse(file.getInputStream());
            return success(resp);
        } catch (Exception e) {
            return CommonResult.error(EXAM_IMPORT_FAIL);
        }
    }

    @PostMapping("/import-excel")
    @Operation(summary = "导入考试（Excel）")
    @PreAuthorize("@ss.hasPermission('hr:exam:create')")
    public CommonResult<ExamImportRespVO> importExcel(@RequestParam("file") MultipartFile file) {
        try {
            ExamImportRespVO resp = ExamExcelParser.parse(file.getInputStream(), file.getOriginalFilename());
            return success(resp);
        } catch (Exception e) {
            return CommonResult.error(EXAM_IMPORT_FAIL);
        }
    }

    @PostMapping("/parse-material")
    @Operation(summary = "解析培训资料（Word/Excel）")
    @PreAuthorize("@ss.hasPermission('hr:exam:create')")
    public CommonResult<ExamMaterialParseRespVO> parseMaterial(@RequestParam("file") MultipartFile file) {
        try {
            return success(ExamMaterialParser.parse(file));
        } catch (Exception e) {
            return CommonResult.error(EXAM_IMPORT_FAIL);
        }
    }

    @GetMapping("/export-template")
    @Operation(summary = "导出考试模板（Excel）")
    @PreAuthorize("@ss.hasPermission('hr:exam:query')")
    public void exportTemplate(HttpServletResponse response) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("模板");
            int rowIndex = 0;
            Row header = sheet.createRow(rowIndex++);
            header.createCell(0).setCellValue("题干");
            header.createCell(1).setCellValue("题型(single/multi/judge/blank/short)");
            header.createCell(2).setCellValue("选项(用|分隔)");
            header.createCell(3).setCellValue("答案(多选用|分隔)");
            header.createCell(4).setCellValue("分值");
            header.createCell(5).setCellValue("解析/备注");

            Row sample1 = sheet.createRow(rowIndex++);
            sample1.createCell(0).setCellValue("示例：以下哪项是正确的？");
            sample1.createCell(1).setCellValue("single");
            sample1.createCell(2).setCellValue("选项A|选项B|选项C");
            sample1.createCell(3).setCellValue("选项A");
            sample1.createCell(4).setCellValue("5");

            Row sample2 = sheet.createRow(rowIndex++);
            sample2.createCell(0).setCellValue("示例：判断题");
            sample2.createCell(1).setCellValue("judge");
            sample2.createCell(2).setCellValue("正确|错误");
            sample2.createCell(3).setCellValue("正确");
            sample2.createCell(4).setCellValue("2");

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String filename = URLEncoder.encode("考试模板.xlsx", StandardCharsets.UTF_8.name());
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + filename);
            workbook.write(response.getOutputStream());
        }
    }

}
