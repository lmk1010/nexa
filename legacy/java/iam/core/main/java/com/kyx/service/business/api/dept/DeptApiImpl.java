package com.kyx.service.business.api.dept;

import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.business.api.dept.dto.DeptCreateReqDTO;
import com.kyx.service.business.api.dept.dto.DeptRespDTO;
import com.kyx.service.business.api.dept.dto.DeptUpsertReqDTO;
import com.kyx.service.business.api.dept.dto.DeptUpsertRespDTO;
import com.kyx.service.business.controller.admin.dept.vo.dept.DeptSaveReqVO;
import com.kyx.service.business.dal.dataobject.dept.DeptDO;
import com.kyx.service.business.service.dept.DeptService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@RestController
@Validated
public class DeptApiImpl implements DeptApi {

    @Resource
    private DeptService deptService;

    @Override
    public CommonResult<DeptRespDTO> getDept(Long id) {
        DeptDO dept = deptService.getDept(id);
        return success(toResp(dept));
    }

    @Override
    public CommonResult<List<DeptRespDTO>> getDeptList(Collection<Long> ids) {
        List<DeptDO> depts = deptService.getDeptList(ids);
        return success(toRespList(depts));
    }

    @Override
    public CommonResult<Boolean> validateDeptList(Collection<Long> ids) {
        deptService.validateDeptList(ids);
        return success(true);
    }

    @Override
    public CommonResult<List<DeptRespDTO>> getChildDeptList(Long id) {
        List<DeptDO> depts = deptService.getChildDeptList(id);
        return success(toRespList(depts));
    }

    @Override
    public CommonResult<List<DeptRespDTO>> getDeptListByTenants(String tenantIds) {
        List<DeptDO> depts = deptService.getDeptListByTenants(StrUtil.trimToNull(tenantIds));
        return success(toRespList(depts));
    }

    @Override
    public CommonResult<DeptRespDTO> getDeptByName(String name) {
        DeptDO dept = deptService.getDeptByName(StrUtil.trimToNull(name));
        return success(toResp(dept));
    }

    @Override
    public CommonResult<Long> createDept(DeptCreateReqDTO reqDTO) {
        String deptName = StrUtil.trimToNull(reqDTO == null ? null : reqDTO.getName());
        if (deptName == null) {
            return success(null);
        }
        DeptDO existed = deptService.getDeptByName(deptName);
        if (existed != null && existed.getId() != null) {
            return success(existed.getId());
        }

        DeptSaveReqVO saveReqVO = new DeptSaveReqVO();
        saveReqVO.setName(deptName);
        saveReqVO.setParentId(reqDTO.getParentId());
        saveReqVO.setSort(reqDTO.getSort() == null ? 0 : reqDTO.getSort());
        saveReqVO.setStatus(reqDTO.getStatus() == null ? CommonStatusEnum.ENABLE.getStatus() : reqDTO.getStatus());
        Long deptId = deptService.createDept(saveReqVO);
        return success(deptId);
    }

    @Override
    public CommonResult<DeptUpsertRespDTO> upsertDept(DeptUpsertReqDTO reqDTO) {
        DeptService.UpsertDeptResult result = deptService.upsertDept(
                reqDTO.getId(), reqDTO.getName(), reqDTO.getParentId(), reqDTO.getSort(), reqDTO.getStatus(),
                reqDTO.getLeaderUserId());
        DeptUpsertRespDTO respDTO = new DeptUpsertRespDTO();
        respDTO.setId(result.getId());
        respDTO.setCreated(result.isCreated());
        respDTO.setUpdated(result.isUpdated());
        return success(respDTO);
    }

    private DeptRespDTO toResp(DeptDO dept) {
        DeptRespDTO respDTO = BeanUtils.toBean(dept, DeptRespDTO.class);
        if (respDTO != null && dept != null) {
            respDTO.setTenantId(dept.getTenantId());
        }
        return respDTO;
    }

    private List<DeptRespDTO> toRespList(List<DeptDO> depts) {
        if (depts == null || depts.isEmpty()) {
            return new ArrayList<>();
        }
        List<DeptRespDTO> result = new ArrayList<>(depts.size());
        for (DeptDO dept : depts) {
            result.add(toResp(dept));
        }
        return result;
    }

}
