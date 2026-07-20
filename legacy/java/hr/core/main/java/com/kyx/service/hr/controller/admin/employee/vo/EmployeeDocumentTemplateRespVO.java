package com.kyx.service.hr.controller.admin.employee.vo;

import lombok.Data;

import java.util.List;

@Data
public class EmployeeDocumentTemplateRespVO {

    private String code;

    private String requestType;

    private String name;

    private String description;

    private List<String> placeholders;

}
