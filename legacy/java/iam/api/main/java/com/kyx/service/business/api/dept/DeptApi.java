package com.kyx.service.business.api.dept;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.util.collection.CollectionUtils;
import com.kyx.service.business.api.dept.dto.DeptCreateReqDTO;
import com.kyx.service.business.api.dept.dto.DeptRespDTO;
import com.kyx.service.business.api.dept.dto.DeptUpsertReqDTO;
import com.kyx.service.business.api.dept.dto.DeptUpsertRespDTO;
import com.kyx.service.business.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@FeignClient(name = ApiConstants.NAME, contextId = "deptApi")
@Tag(name = "RPC 服务 - 部门")
public interface DeptApi {

    String PREFIX = ApiConstants.PREFIX + "/dept";

    @GetMapping(PREFIX + "/get")
    @Operation(summary = "获得部门信息")
    @Parameter(name = "id", description = "部门编号", example = "1024", required = true)
    CommonResult<DeptRespDTO> getDept(@RequestParam("id") Long id);

    @GetMapping(PREFIX + "/list")
    @Operation(summary = "获得部门信息数组")
    @Parameter(name = "ids", description = "部门编号数组", example = "1,2", required = true)
    CommonResult<List<DeptRespDTO>> getDeptList(@RequestParam("ids") Collection<Long> ids);

    @GetMapping(PREFIX + "/valid")
    @Operation(summary = "校验部门是否合法")
    @Parameter(name = "ids", description = "部门编号数组", example = "1,2", required = true)
    CommonResult<Boolean> validateDeptList(@RequestParam("ids") Collection<Long> ids);

    default Map<Long, DeptRespDTO> getDeptMap(Collection<Long> ids) {
        List<DeptRespDTO> list = getDeptList(ids).getCheckedData();
        return CollectionUtils.convertMap(list, DeptRespDTO::getId);
    }

    @GetMapping(PREFIX + "/list-child")
    @Operation(summary = "获得指定部门的所有子部门")
    @Parameter(name = "id", description = "部门编号", example = "1024", required = true)
    CommonResult<List<DeptRespDTO>> getChildDeptList(@RequestParam("id") Long id);

    @GetMapping(PREFIX + "/list-by-tenants")
    @Operation(summary = "根据租户编号查询部门列表")
    @Parameter(name = "tenantIds", description = "租户编号列表，逗号分隔", example = "171,164")
    CommonResult<List<DeptRespDTO>> getDeptListByTenants(@RequestParam(value = "tenantIds", required = false) String tenantIds);

    @GetMapping(PREFIX + "/get-by-name")
    @Operation(summary = "根据部门名称获取部门")
    @Parameter(name = "name", description = "部门名称", example = "研发中心", required = true)
    CommonResult<DeptRespDTO> getDeptByName(@RequestParam("name") String name);

    @PostMapping(PREFIX + "/create")
    @Operation(summary = "创建部门")
    CommonResult<Long> createDept(@Valid @RequestBody DeptCreateReqDTO reqDTO);

    @PostMapping(PREFIX + "/upsert")
    @Operation(summary = "按指定编号创建或更新部门")
    CommonResult<DeptUpsertRespDTO> upsertDept(@Valid @RequestBody DeptUpsertReqDTO reqDTO);

}
