package com.kyx.service.finance.service.init;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.finance.controller.admin.init.vo.company.FinanceCompanyPageReqVO;
import com.kyx.service.finance.controller.admin.init.vo.company.FinanceCompanySaveReqVO;
import com.kyx.service.finance.controller.admin.init.vo.company.FinanceCompanySubjectRespVO;
import com.kyx.service.finance.dal.dataobject.init.FinanceCompanyDO;
import com.kyx.service.finance.dal.dataobject.init.FinanceCompanySubjectDO;

import java.util.List;

/**
 * 账套服务接口
 *
 * @author Trae AI
 */
public interface FinanceCompanyService {

    /**
     * 创建账套
     *
     * @param reqVO 账套保存请求
     * @return 账套ID
     */
    Long createCompany(FinanceCompanySaveReqVO reqVO);

    /**
     * 更新账套
     *
     * @param reqVO 账套保存请求
     * @return 是否成功
     */
    Boolean updateCompany(FinanceCompanySaveReqVO reqVO);

    /**
     * 删除账套
     *
     * @param ids 账套ID列表
     * @return 是否成功
     */
    Boolean deleteCompany(List<Long> ids);

    /**
     * 获取账套详情
     *
     * @param id 账套ID
     * @return 账套响应
     */
    FinanceCompanyDO getCompany(Long id);

    /**
     * 分页查询账套
     *
     * @param reqVO 分页查询请求
     * @return 分页结果
     */
    PageResult<FinanceCompanyDO> pageCompany(FinanceCompanyPageReqVO reqVO);

    /**
     * 查询账套绑定的科目树
     *
     * @param companyId 账套ID
     * @return 科目树
     */
    List<FinanceCompanySubjectRespVO> listCompanySubjectTree(Long companyId);

}
