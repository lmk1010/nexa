package com.kyx.service.business.dal.dataobject.auth;

import lombok.Data;

import java.util.List;

@Data
public class AuthPreLoginCacheDO {

    private Long userId;

    private String username;

    private String deviceType;

    private String deviceId;

    private List<Long> tenantIds;

}
