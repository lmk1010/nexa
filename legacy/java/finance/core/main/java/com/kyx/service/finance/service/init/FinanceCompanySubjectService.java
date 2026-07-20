package com.kyx.service.finance.service.init;

import com.kyx.service.finance.controller.admin.init.vo.company.FinanceCompanySubjectRespVO;
import com.kyx.service.finance.controller.admin.init.vo.company.FinanceCompanySubjectSaveReqVO;
import com.kyx.service.finance.dal.dataobject.init.FinanceCompanySubjectDO;

import java.util.List;

/**
 * 账套科目服务接口
 * <p>
 * 职责：
 * 1. 账套科目的独立 CRUD（新增/修改/停用/删除）
 * 2. 账套科目树查询
 * 3. 科目编码/名称校验
 *
 * @author xyang
 */
public interface FinanceCompanySubjectService {

    /**
     * 新增账套科目（手动添加三级及以下自定义科目）
     *
     * @param reqVO 科目信息
     * @return 科目ID
     */
    Long createCompanySubject(FinanceCompanySubjectSaveReqVO reqVO);

    /**
     * 更新账套科目（仅允许修改名称/备注/状态/排序）
     *
     * @param reqVO 科目信息
     * @return 是否成功
     */
    Boolean updateCompanySubject(FinanceCompanySubjectSaveReqVO reqVO);

    /**
     * 删除账套科目（仅允许删除三级及以下、未被引用的科目）
     *
     * @param id 科目ID
     * @return 是否成功
     */
    Boolean deleteCompanySubject(Long id);

    /**
     * 启用/停用账套科目
     *
     * @param id     科目ID
     * @param status 状态：0-启用，1-停用
     * @return 是否成功
     */
    Boolean updateCompanySubjectStatus(Long id, Integer status);

    /**
     * 获取账套科目详情
     *
     * @param id 科目ID
     * @return 科目详情
     */
    FinanceCompanySubjectDO getCompanySubject(Long id);

    /**
     * 查询账套科目树（含层级结构）
     *
     * @param companyId 账套ID
     * @return 科目树
     */
    List<FinanceCompanySubjectRespVO> listCompanySubjectTree(Long companyId);

    /**
     * 查询账套科目平铺列表（用于凭证/流水选择科目）
     *
     * @param companyId 账套ID
     * @return 科目列表（仅末级启用科目）
     */
    List<FinanceCompanySubjectDO> listLeafSubjects(Long companyId);

    /**
     * 根据科目编码查询账套科目（启用状态）
     *
     * @param companyId   账套ID
     * @param subjectCode 科目编码
     * @return 科目信息
     */
    FinanceCompanySubjectDO getEnabledByCode(Long companyId, String subjectCode);

    /**
     * 校验科目编码在账套内是否已存在
     *
     * @param companyId   账套ID
     * @param subjectCode 科目编码
     * @param excludeId   排除的科目ID（更新时使用）
     * @return true=已存在
     */
    boolean checkSubjectCodeExists(Long companyId, String subjectCode, Long excludeId);
}
