package com.kyx.service.finance.service.init;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.finance.controller.admin.init.vo.subject.FinanceSubjectTemplatePageReqVO;
import com.kyx.service.finance.controller.admin.init.vo.subject.FinanceSubjectTemplateSaveReqVO;
import com.kyx.service.finance.controller.admin.init.vo.subject.FinanceSubjectTemplateTreeNodeVO;
import com.kyx.service.finance.dal.dataobject.init.FinanceSubjectTemplateDO;

import java.util.List;

/**
 * 科目模板服务接口
 *
 * @author Trae AI
 */
public interface FinanceSubjectTemplateService {

    /**
     * 创建科目模板
     * @param reqVO 科目模板信息
     * @return 模板ID
     */
    Long createSubjectTemplate(FinanceSubjectTemplateSaveReqVO reqVO);

    /**
     * 更新科目模板
     * @param reqVO 科目模板信息
     * @return 是否成功
     */
    Boolean updateSubjectTemplate(FinanceSubjectTemplateSaveReqVO reqVO);

    /**
     * 删除科目模板
     * @param id 模板ID
     * @return 是否成功
     */
    Boolean deleteSubjectTemplate(Long id);

    /**
     * 获取科目模板详情
     * @param id 模板ID
     * @return 科目模板详情
     */
    FinanceSubjectTemplateDO getSubjectTemplate(Long id);

    /**
     * 分页查询科目模板
     * @param reqVO 查询条件
     * @return 分页结果
     */
    PageResult<FinanceSubjectTemplateDO> pageSubjectTemplate(FinanceSubjectTemplatePageReqVO reqVO);

    /**
     * 查询科目模板列表
     * @param reqVO 查询条件
     * @return 科目模板列表
     */
    List<FinanceSubjectTemplateDO> listSubjectTemplate(FinanceSubjectTemplatePageReqVO reqVO);

    List<FinanceSubjectTemplateTreeNodeVO> treeSubjectTemplateTree(FinanceSubjectTemplatePageReqVO reqVO);
}
