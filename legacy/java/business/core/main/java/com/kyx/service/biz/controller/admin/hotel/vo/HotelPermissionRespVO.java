package com.kyx.service.biz.controller.admin.hotel.vo;

import lombok.Data;

import java.util.List;

@Data
public class HotelPermissionRespVO {
    private Boolean canUseFrontDesk;
    private Boolean canViewDashboard;
    private Boolean canManageWorkOrder;
    private Boolean canDeleteWorkOrder;
    private Boolean canViewAllStores;
    private String scopedStore;
    private Long deptId;
    private String deptName;
    private List<String> stores;
}
