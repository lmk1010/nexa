package com.kyx.service.finance.controller.admin.init;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.common.validation.group.SaveGroup;
import com.kyx.foundation.common.validation.group.UpdateGroup;
import com.kyx.service.finance.controller.admin.init.vo.subject.FinanceSubjectTemplatePageReqVO;
import com.kyx.service.finance.controller.admin.init.vo.subject.FinanceSubjectTemplateRespVO;
import com.kyx.service.finance.controller.admin.init.vo.subject.FinanceSubjectTemplateSaveReqVO;
import com.kyx.service.finance.controller.admin.init.vo.subject.FinanceSubjectTemplateTreeNodeVO;
import com.kyx.service.finance.dal.dataobject.init.FinanceSubjectTemplateDO;
import com.kyx.service.finance.service.init.FinanceSubjectTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

import static com.kyx.foundation.common.pojo.CommonResult.success;

/**
 * 科目模板控制器
 *
 * @author Trae AI
 */
@RestController
@RequestMapping("/finance/init/subject-template")
@Tag(name = "财务初始化 - 科目模板管理")
@Validated
public class FinanceSubjectTemplateController {

    @Resource
    private FinanceSubjectTemplateService financeSubjectTemplateService;

    @Operation(summary = "创建科目模板")
    @PostMapping("/create")
    @PreAuthorize("@ss.hasAnyPermissions('finance:subject-template:create,finance:subject:create')")
    public CommonResult<Long> createSubjectTemplate(@Validated(SaveGroup.class) @RequestBody FinanceSubjectTemplateSaveReqVO reqVO) {
        return success(financeSubjectTemplateService.createSubjectTemplate(reqVO));
    }

    @Operation(summary = "更新科目模板")
    @PutMapping("/update")
    @PreAuthorize("@ss.hasAnyPermissions('finance:subject-template:update,finance:subject:update')")
    public CommonResult<Boolean> updateSubjectTemplate(@Validated(UpdateGroup.class) @RequestBody FinanceSubjectTemplateSaveReqVO reqVO) {
        return success(financeSubjectTemplateService.updateSubjectTemplate(reqVO));
    }

    @Operation(summary = "删除科目模板")
    @DeleteMapping("/delete")
    @Parameter(name = "id", description = "模板ID", required = true)
    @PreAuthorize("@ss.hasAnyPermissions('finance:subject-template:delete,finance:subject:delete')")
    public CommonResult<Boolean> deleteSubjectTemplate(@RequestParam("id") Long id) {
        return success(financeSubjectTemplateService.deleteSubjectTemplate(id));
    }

    @Operation(summary = "获取科目模板详情")
    @GetMapping("/get")
    @PreAuthorize("@ss.hasAnyPermissions('finance:subject-template:query,finance:subject:query')")
    public CommonResult<FinanceSubjectTemplateRespVO> getSubjectTemplate(@RequestParam Long id) {
        FinanceSubjectTemplateDO template = financeSubjectTemplateService.getSubjectTemplate(id);
        return success(BeanUtils.toBean(template, FinanceSubjectTemplateRespVO.class));
    }

    @Operation(summary = "查询科目模板列表")
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermission('finance:subject-template:list')")
    public CommonResult<List<FinanceSubjectTemplateRespVO>> listSubjectTemplate(@Validated FinanceSubjectTemplatePageReqVO reqVO) {
        List<FinanceSubjectTemplateDO> list = financeSubjectTemplateService.listSubjectTemplate(reqVO);
        return success(BeanUtils.toBean(list, FinanceSubjectTemplateRespVO.class));
    }


    @Operation(summary = "查询科目模板树")
    @GetMapping("/tree")
    @PreAuthorize("@ss.hasPermission('finance:subject-template:list')")
    public CommonResult<List<FinanceSubjectTemplateTreeNodeVO>> treeSubjectTemplateTree(@Validated FinanceSubjectTemplatePageReqVO reqVO) {
        return success(financeSubjectTemplateService.treeSubjectTemplateTree(reqVO));
    }

}
