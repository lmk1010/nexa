package com.kyx.service.finance.controller.admin.init;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.common.validation.group.SaveGroup;
import com.kyx.foundation.common.validation.group.UpdateGroup;
import com.kyx.service.finance.controller.admin.init.vo.company.FinanceCompanySubjectRespVO;
import com.kyx.service.finance.controller.admin.init.vo.company.FinanceCompanySubjectSaveReqVO;
import com.kyx.service.finance.dal.dataobject.init.FinanceCompanySubjectDO;
import com.kyx.service.finance.service.init.FinanceCompanySubjectService;
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
 * 账套科目管理 Controller
 * <p>
 * 提供账套科目的独立 CRUD 接口，与账套管理解耦。
 * 一级/二级科目由账套创建时从模板导入，不允许手动新增/删除。
 * 三级及以下科目可手动新增/修改/删除。
 *
 * @author xyang
 */
@RestController
@RequestMapping("/finance/init/company-subject")
@Tag(name = "财务初始化 - 账套科目管理")
@Validated
public class FinanceCompanySubjectController {

    @Resource
    private FinanceCompanySubjectService companySubjectService;

    // ----------------------------------------------------------------
    // 新增
    // ----------------------------------------------------------------

    @Operation(summary = "新增账套科目（三级及以下）",
            description = "一级/二级科目由账套创建时从模板导入，不允许手动新增")
    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('finance:company-subject:create')")
    public CommonResult<Long> createCompanySubject(
            @Validated(SaveGroup.class) @RequestBody FinanceCompanySubjectSaveReqVO reqVO) {
        return success(companySubjectService.createCompanySubject(reqVO));
    }

    // ----------------------------------------------------------------
    // 修改
    // ----------------------------------------------------------------

    @Operation(summary = "修改账套科目",
            description = "仅允许修改：科目名称 / 备注 / 状态 / 排序")
    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('finance:company-subject:update')")
    public CommonResult<Boolean> updateCompanySubject(
            @Validated(UpdateGroup.class) @RequestBody FinanceCompanySubjectSaveReqVO reqVO) {
        return success(companySubjectService.updateCompanySubject(reqVO));
    }

    // ----------------------------------------------------------------
    // 删除
    // ----------------------------------------------------------------

    @Operation(summary = "删除账套科目（三级及以下、未被引用）")
    @DeleteMapping("/delete")
    @Parameter(name = "id", description = "科目ID", required = true)
    @PreAuthorize("@ss.hasPermission('finance:company-subject:delete')")
    public CommonResult<Boolean> deleteCompanySubject(@RequestParam("id") Long id) {
        return success(companySubjectService.deleteCompanySubject(id));
    }

    // ----------------------------------------------------------------
    // 启用/停用
    // ----------------------------------------------------------------

    @Operation(summary = "启用/停用账套科目")
    @PutMapping("/status")
    @Parameter(name = "id", description = "科目ID", required = true)
    @Parameter(name = "status", description = "状态：0-启用，1-停用", required = true)
    @PreAuthorize("@ss.hasPermission('finance:company-subject:update')")
    public CommonResult<Boolean> updateStatus(
            @RequestParam("id") Long id,
            @RequestParam("status") Integer status) {
        return success(companySubjectService.updateCompanySubjectStatus(id, status));
    }

    // ----------------------------------------------------------------
    // 查询
    // ----------------------------------------------------------------

    @Operation(summary = "获取账套科目详情")
    @GetMapping("/get")
    @PreAuthorize("@ss.hasAnyPermissions('finance:company-subject:query,finance:company:list')")
    public CommonResult<FinanceCompanySubjectRespVO> getCompanySubject(@RequestParam Long id) {
        FinanceCompanySubjectDO subject = companySubjectService.getCompanySubject(id);
        return success(BeanUtils.toBean(subject, FinanceCompanySubjectRespVO.class));
    }

    @Operation(summary = "查询账套科目树（含层级结构）",
            description = "返回完整的科目树，含停用科目，用于科目管理页面")
    @GetMapping("/tree")
    @PreAuthorize("@ss.hasAnyPermissions('finance:company-subject:query,finance:company:list,finance:voucher:list,finance:opening-balance:list')")
    public CommonResult<List<FinanceCompanySubjectRespVO>> listCompanySubjectTree(
            @RequestParam Long companyId) {
        return success(companySubjectService.listCompanySubjectTree(companyId));
    }

    @Operation(summary = "查询账套末级启用科目列表",
            description = "仅返回末级（isLeaf=true）且启用的科目，用于凭证/流水选择科目")
    @GetMapping("/leaf-list")
    @PreAuthorize("@ss.hasAnyPermissions('finance:company-subject:query,finance:voucher:list,finance:transaction:list')")
    public CommonResult<List<FinanceCompanySubjectRespVO>> listLeafSubjects(
            @RequestParam Long companyId) {
        List<FinanceCompanySubjectDO> list = companySubjectService.listLeafSubjects(companyId);
        return success(BeanUtils.toBean(list, FinanceCompanySubjectRespVO.class));
    }
}
