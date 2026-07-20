package com.kyx.service.hr.controller.admin.employee.vo;

import lombok.Data;

import java.util.Map;

@Data
public class EmployeeDocumentTemplatePreviewRespVO {

    private String templateCode;

    private String requestType;

    private String title;

    private String fileName;

    private String content;

    private Map<String, String> variables;

}
