package com.kyx.service.finance.service.init;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.finance.controller.admin.init.vo.contact.FinanceContactGroupSaveReqVO;
import com.kyx.service.finance.controller.admin.init.vo.contact.FinanceContactGroupTreeRespVO;
import com.kyx.service.finance.controller.admin.init.vo.contact.FinanceContactPageReqVO;
import com.kyx.service.finance.controller.admin.init.vo.contact.FinanceContactSaveReqVO;
import com.kyx.service.finance.dal.dataobject.init.FinanceContactDO;

import java.util.Collection;
import java.util.List;

/**
 * 往来信息服务接口
 */
public interface FinanceContactService {

    /**
     * 创建往来
     */
    Long createContact(FinanceContactSaveReqVO reqVO);

    /**
     * 更新往来
     */
    Boolean updateContact(FinanceContactSaveReqVO reqVO);

    /**
     * 删除往来
     */
    Boolean deleteContact(Long id);

    /**
     * 更新往来状态
     */
    Boolean updateContactStatus(Long id, Integer status);

    /**
     * 批量更新往来状态
     */
    Boolean batchUpdateContactStatus(Collection<Long> ids, Integer status);

    /**
     * 批量删除往来
     */
    Boolean batchDeleteContact(Collection<Long> ids);

    /**
     * 获取往来详情
     */
    FinanceContactDO getContact(Long id);

    /**
     * 分页查询往来
     */
    PageResult<FinanceContactDO> pageContact(FinanceContactPageReqVO reqVO);

    /**
     * 查询往来分组树
     */
    List<FinanceContactGroupTreeRespVO> listContactGroupTree();

    /**
     * 新增往来分组
     */
    Long createContactGroup(FinanceContactGroupSaveReqVO reqVO);

    /**
     * 更新往来分组
     */
    Boolean updateContactGroup(FinanceContactGroupSaveReqVO reqVO);

    /**
     * 删除往来分组
     */
    Boolean deleteContactGroup(Long id);
}
