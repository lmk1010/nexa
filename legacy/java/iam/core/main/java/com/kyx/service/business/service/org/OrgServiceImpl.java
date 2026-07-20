package com.kyx.service.business.service.org;

import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.tenant.core.aop.TenantIgnore;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.service.business.controller.admin.org.vo.OrgTreeNodeRespVO;
import com.kyx.service.business.dal.dataobject.dept.DeptDO;
import com.kyx.service.business.dal.dataobject.tenant.TenantDO;
import com.kyx.service.business.dal.mysql.dept.DeptMapper;
import com.kyx.service.business.service.tenant.TenantService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrgServiceImpl implements OrgService {

    @Resource
    private TenantService tenantService;

    @Resource
    private DeptMapper deptMapper;

    @Override
    @TenantIgnore
    public List<OrgTreeNodeRespVO> getOrgTree() {
        Long currentTenantId = TenantContextHolder.getTenantId();

        // 1. 获取可见租户ID列表
        List<Long> tenantIds = tenantService.getAllowedTenantIds(currentTenantId);

        // 2. 加载租户数据，转换为树节点
        List<TenantDO> tenants = tenantService.getTenantListByIds(tenantIds);
        List<OrgTreeNodeRespVO> result = new ArrayList<>();
        for (TenantDO tenant : tenants) {
            OrgTreeNodeRespVO node = new OrgTreeNodeRespVO();
            node.setId("t-" + tenant.getId());
            node.setName(tenant.getName());
            // parentId: 有父租户则指向父租户节点，否则为空（根节点）
            if (tenant.getParentId() != null && tenant.getParentId() > 0) {
                node.setParentId("t-" + tenant.getParentId());
            }
            node.setNodeType("tenant");
            node.setTenantId(tenant.getId());
            result.add(node);
        }

        // 3. 跨租户加载部门（仅启用状态）
        Map<Long, String> tenantNameMap = tenants.stream()
                .collect(Collectors.toMap(TenantDO::getId, TenantDO::getName));

        List<DeptDO> depts = deptMapper.selectListByTenantIds(
                tenantIds, CommonStatusEnum.ENABLE.getStatus());

        // 第一遍：找出所有与租户同名的顶级部门，记录其 deptId → tenantId
        Map<Long, Long> skippedDeptToTenant = new HashMap<>();
        for (DeptDO dept : depts) {
            boolean isRootDept = dept.getParentId() == null || dept.getParentId().equals(DeptDO.PARENT_ID_ROOT);
            if (isRootDept) {
                String tName = tenantNameMap.get(dept.getTenantId());
                if (tName != null && tName.equals(dept.getName())) {
                    skippedDeptToTenant.put(dept.getId(), dept.getTenantId());
                }
            }
        }

        // 第二遍：构建部门节点，跳过同名顶级部门，其子部门改挂到租户节点
        for (DeptDO dept : depts) {
            if (skippedDeptToTenant.containsKey(dept.getId())) {
                continue;
            }

            boolean isRootDept = dept.getParentId() == null || dept.getParentId().equals(DeptDO.PARENT_ID_ROOT);
            OrgTreeNodeRespVO node = new OrgTreeNodeRespVO();
            node.setId("d-" + dept.getId());
            node.setName(dept.getName());
            if (isRootDept) {
                node.setParentId("t-" + dept.getTenantId());
            } else if (skippedDeptToTenant.containsKey(dept.getParentId())) {
                // 父部门被跳过了，直接挂到租户节点
                node.setParentId("t-" + skippedDeptToTenant.get(dept.getParentId()));
            } else {
                node.setParentId("d-" + dept.getParentId());
            }
            node.setNodeType("dept");
            node.setTenantId(dept.getTenantId());
            node.setDeptId(dept.getId());
            result.add(node);
        }

        return result;
    }

}
