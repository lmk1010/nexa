package com.kyx.service.finance.controller.admin.init.vo.subject;

import com.kyx.foundation.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 科目模板分页查询请求
 *
 * @author Trae AI
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FinanceSubjectTemplatePageReqVO extends PageParam {

    /**
     * 会计制度
     */
    private String accountingSystem;

    /**
     * 自定义模板所属租户ID
     */
    private Long customTenantId;

    /**
     * 科目编码
     */
    private String subjectCode;

    /**
     * 科目名称
     */
    private String subjectName;

    /**
     * 科目类型
     */
    private String subjectType;

    /**
     * 状态
     */
    private Integer status;

}
