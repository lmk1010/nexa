package com.kyx.service.business.service.org;

import com.kyx.service.business.controller.admin.org.vo.OrgTreeNodeRespVO;

import java.util.List;

/**
 * 组织架构 Service 接口
 */
public interface OrgService {

    /**
     * 获取组织架构树（租户+部门合并的扁平列表）
     *
     * @return 组织架构树节点列表
     */
    List<OrgTreeNodeRespVO> getOrgTree();

}
