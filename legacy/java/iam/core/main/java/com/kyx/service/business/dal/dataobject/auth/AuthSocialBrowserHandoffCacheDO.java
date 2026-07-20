package com.kyx.service.business.dal.dataobject.auth;

import com.kyx.service.business.controller.admin.tenant.vo.tenant.UserTenantRespVO;
import lombok.Data;

import java.util.List;

@Data
public class AuthSocialBrowserHandoffCacheDO {

    private String status;

    private String message;

    private String preAuthToken;

    private List<UserTenantRespVO> tenantList;

}
