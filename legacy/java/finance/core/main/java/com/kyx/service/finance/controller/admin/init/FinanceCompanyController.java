package com.kyx.service.finance.controller.admin.init;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.common.validation.group.SaveGroup;
import com.kyx.foundation.common.validation.group.UpdateGroup;
import com.kyx.service.finance.controller.admin.init.vo.company.FinanceCompanyPageReqVO;
import com.kyx.service.finance.controller.admin.init.vo.company.FinanceCompanyRespVO;
import com.kyx.service.finance.controller.admin.init.vo.company.FinanceCompanySaveReqVO;
import com.kyx.service.finance.controller.admin.init.vo.company.FinanceCompanySubjectRespVO;
import com.kyx.service.finance.dal.dataobject.init.FinanceCompanyDO;
import com.kyx.service.finance.service.init.FinanceCompanyService;
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
 * 账套控制器
 *
 * @author Trae AI
 */
@RestController
@RequestMapping("/finance/init/company")
@Tag(name = "财务初始化 - 账套管理")
@Validated
public class FinanceCompanyController {

    @Resource
    private FinanceCompanyService financeCompanyService;

    @Operation(summary = "创建账套")
    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('finance:company:create')")
    public CommonResult<Long> createCompany(@Validated(SaveGroup.class) @RequestBody FinanceCompanySaveReqVO reqVO) {
        return success(financeCompanyService.createCompany(reqVO));
    }

    @Operation(summary = "更新账套")
    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('finance:company:update')")
    public CommonResult<Boolean> updateCompany(@Validated(UpdateGroup.class) @RequestBody FinanceCompanySaveReqVO reqVO) {
        return success(financeCompanyService.updateCompany(reqVO));
    }

    @Operation(summary = "删除账套")
    @DeleteMapping("/delete")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('finance:company:delete')")
    public CommonResult<Boolean> deleteCompany(@RequestParam("ids") List<Long> ids) {
        return success(financeCompanyService.deleteCompany(ids));
    }

    @Operation(summary = "获取账套详情")
    @GetMapping("/get")
    @PreAuthorize("@ss.hasPermission('finance:company:query')")
    public CommonResult<FinanceCompanyRespVO> getCompany(@RequestParam Long id) {
        FinanceCompanyDO company = financeCompanyService.getCompany(id);
        return success(BeanUtils.toBean(company, FinanceCompanyRespVO.class));
    }

    @Operation(summary = "分页查询账套")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasAnyPermissions('finance:company:list,finance:transaction:list,finance:voucher:list,finance:receivable-payable:list,finance:closing:list,finance:opening-balance:list')")
    public CommonResult<PageResult<FinanceCompanyRespVO>> pageCompany(@Validated FinanceCompanyPageReqVO reqVO) {
        PageResult<FinanceCompanyDO> paged = financeCompanyService.pageCompany(reqVO);
        return success(BeanUtils.toBean(paged, FinanceCompanyRespVO.class));
    }

    @Operation(summary = "查询账套绑定科目列表")
    @GetMapping("/subject-tree")
    @PreAuthorize("@ss.hasAnyPermissions('finance:company:list,finance:voucher:list,finance:opening-balance:list')")
    public CommonResult<List<FinanceCompanySubjectRespVO>> listCompanySubjectTree(@RequestParam Long companyId) {
        return success(financeCompanyService.listCompanySubjectTree(companyId));
    }

}
