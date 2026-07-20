package com.kyx.service.finance.controller.admin.init;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.finance.controller.admin.init.vo.contact.FinanceContactBatchDeleteReqVO;
import com.kyx.service.finance.controller.admin.init.vo.contact.FinanceContactBatchStatusUpdateReqVO;
import com.kyx.service.finance.controller.admin.init.vo.contact.FinanceContactGroupSaveReqVO;
import com.kyx.service.finance.controller.admin.init.vo.contact.FinanceContactGroupTreeRespVO;
import com.kyx.service.finance.controller.admin.init.vo.contact.FinanceContactPageReqVO;
import com.kyx.service.finance.controller.admin.init.vo.contact.FinanceContactRespVO;
import com.kyx.service.finance.controller.admin.init.vo.contact.FinanceContactSaveReqVO;
import com.kyx.service.finance.controller.admin.init.vo.contact.FinanceContactStatusUpdateReqVO;
import com.kyx.service.finance.service.init.FinanceContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static com.kyx.foundation.common.pojo.CommonResult.success;

/**
 * 往来信息控制器
 */
@RestController
@RequestMapping("/finance/init/contact")
@Tag(name = "财务初始化 - 往来管理")
@Validated
public class FinanceContactController {

    @Resource
    private FinanceContactService financeContactService;

    @Operation(summary = "创建往来")
    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('finance:contact:create')")
    public CommonResult<Long> createContact(@Valid @RequestBody FinanceContactSaveReqVO reqVO) {
        return success(financeContactService.createContact(reqVO));
    }

    @Operation(summary = "更新往来")
    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('finance:contact:update')")
    public CommonResult<Boolean> updateContact(@Valid @RequestBody FinanceContactSaveReqVO reqVO) {
        return success(financeContactService.updateContact(reqVO));
    }

    @Operation(summary = "删除往来")
    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('finance:contact:delete')")
    public CommonResult<Boolean> deleteContact(@RequestParam Long id) {
        return success(financeContactService.deleteContact(id));
    }

    @Operation(summary = "更新往来状态")
    @PutMapping("/update-status")
    @PreAuthorize("@ss.hasPermission('finance:contact:update')")
    public CommonResult<Boolean> updateContactStatus(@Valid @RequestBody FinanceContactStatusUpdateReqVO reqVO) {
        return success(financeContactService.updateContactStatus(reqVO.getId(), reqVO.getStatus()));
    }

    @Operation(summary = "批量更新往来状态")
    @PutMapping("/batch-update-status")
    @PreAuthorize("@ss.hasPermission('finance:contact:update')")
    public CommonResult<Boolean> batchUpdateContactStatus(@Valid @RequestBody FinanceContactBatchStatusUpdateReqVO reqVO) {
        return success(financeContactService.batchUpdateContactStatus(reqVO.getIds(), reqVO.getStatus()));
    }

    @Operation(summary = "批量删除往来")
    @PostMapping("/batch-delete")
    @PreAuthorize("@ss.hasPermission('finance:contact:delete')")
    public CommonResult<Boolean> batchDeleteContact(@Valid @RequestBody FinanceContactBatchDeleteReqVO reqVO) {
        return success(financeContactService.batchDeleteContact(reqVO.getIds()));
    }

    @Operation(summary = "获取往来详情")
    @GetMapping("/get")
    @PreAuthorize("@ss.hasPermission('finance:contact:query')")
    public CommonResult<FinanceContactRespVO> getContact(@RequestParam Long id) {
        return success(BeanUtils.toBean(financeContactService.getContact(id), FinanceContactRespVO.class));
    }

    @Operation(summary = "分页查询往来")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasAnyPermissions('finance:contact:list,finance:receivable-payable:list')")
    public CommonResult<PageResult<FinanceContactRespVO>> pageContact(@Valid FinanceContactPageReqVO reqVO) {
        return success(BeanUtils.toBean(financeContactService.pageContact(reqVO), FinanceContactRespVO.class));
    }

    @Operation(summary = "查询往来分组树")
    @GetMapping("/group-tree")
    @PreAuthorize("@ss.hasAnyPermissions('finance:contact:query,finance:contact:list,finance:receivable-payable:list')")
    public CommonResult<List<FinanceContactGroupTreeRespVO>> getContactGroupTree() {
        return success(financeContactService.listContactGroupTree());
    }

    @Operation(summary = "新增往来分组")
    @PostMapping("/group/create")
    @PreAuthorize("@ss.hasPermission('finance:contact:create')")
    public CommonResult<Long> createContactGroup(@Valid @RequestBody FinanceContactGroupSaveReqVO reqVO) {
        return success(financeContactService.createContactGroup(reqVO));
    }

    @Operation(summary = "更新往来分组")
    @PutMapping("/group/update")
    @PreAuthorize("@ss.hasPermission('finance:contact:update')")
    public CommonResult<Boolean> updateContactGroup(@Valid @RequestBody FinanceContactGroupSaveReqVO reqVO) {
        return success(financeContactService.updateContactGroup(reqVO));
    }

    @Operation(summary = "删除往来分组")
    @DeleteMapping("/group/delete")
    @PreAuthorize("@ss.hasPermission('finance:contact:delete')")
    public CommonResult<Boolean> deleteContactGroup(@RequestParam Long id) {
        return success(financeContactService.deleteContactGroup(id));
    }
}
