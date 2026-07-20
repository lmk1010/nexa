package com.kyx.service.erp.service.printtemplate;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.erp.controller.admin.printtemplate.vo.ErpPrintTemplatePageReqVO;
import com.kyx.service.erp.controller.admin.printtemplate.vo.ErpPrintTemplateRespVO;
import com.kyx.service.erp.controller.admin.printtemplate.vo.ErpPrintTemplateSaveReqVO;
import com.kyx.service.erp.dal.dataobject.printtemplate.ErpPrintTemplateDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * ERP 打印模版 Service 接口
 *
 * @author kyx
 */
public interface ErpPrintTemplateService {

    /**
     * 创建打印模版
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createPrintTemplate(@Valid ErpPrintTemplateSaveReqVO createReqVO);

    /**
     * 更新打印模版
     *
     * @param updateReqVO 更新信息
     */
    void updatePrintTemplate(@Valid ErpPrintTemplateSaveReqVO updateReqVO);

    /**
     * 删除打印模版
     *
     * @param id 编号
     */
    void deletePrintTemplate(Long id);

    /**
     * 获得打印模版
     *
     * @param id 编号
     * @return 打印模版
     */
    ErpPrintTemplateDO getPrintTemplate(Long id);

    /**
     * 获得打印模版列表
     *
     * @param ids 编号数组
     * @return 打印模版列表
     */
    List<ErpPrintTemplateDO> getPrintTemplateList(Collection<Long> ids);

    /**
     * 获得打印模版分页
     *
     * @param pageReqVO 分页查询
     * @return 打印模版分页
     */
    PageResult<ErpPrintTemplateRespVO> getPrintTemplatePage(ErpPrintTemplatePageReqVO pageReqVO);

    /**
     * 获得打印模版VO分页
     *
     * @param pageReqVO 分页查询
     * @return 打印模版分页
     */
    PageResult<ErpPrintTemplateRespVO> getPrintTemplateVOPage(ErpPrintTemplatePageReqVO pageReqVO);

    /**
     * 获得启用的打印模版列表
     *
     * @param type 模版类型
     * @return 打印模版列表
     */
    List<ErpPrintTemplateRespVO> getEnabledPrintTemplateList(String type);

    /**
     * 复制打印模版
     *
     * @param id 编号
     * @param name 新模版名称
     * @return 新模版编号
     */
    Long copyPrintTemplate(Long id, String name);

    /**
     * 更新模版状态
     *
     * @param id 编号
     * @param status 状态
     */
    void updatePrintTemplateStatus(Long id, Integer status);

} 