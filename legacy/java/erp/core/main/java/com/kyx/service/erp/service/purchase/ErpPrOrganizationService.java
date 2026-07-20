package com.kyx.service.erp.service.purchase;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.erp.controller.admin.purchase.vo.prorganization.ErpPrOrganizationPageReqVO;
import com.kyx.service.erp.controller.admin.purchase.vo.prorganization.ErpPrOrganizationSaveReqVO;
import com.kyx.service.erp.dal.dataobject.purchase.ErpPrOrganizationDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.kyx.foundation.common.util.collection.CollectionUtils.convertMap;

/**
 * ERP 采购组织 Service 接口
 *
 * @author MK
 */
public interface ErpPrOrganizationService {

    /**
     * 创建采购组织
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createPrOrganization(@Valid ErpPrOrganizationSaveReqVO createReqVO);

    /**
     * 更新采购组织
     *
     * @param updateReqVO 更新信息
     */
    void updatePrOrganization(@Valid ErpPrOrganizationSaveReqVO updateReqVO);

    /**
     * 删除采购组织
     *
     * @param id 编号
     */
    void deletePrOrganization(Long id);

    /**
     * 批量删除采购组织
     *
     * @param ids 编号列表
     */
    void deletePrOrganizationBatch(List<Long> ids);

    /**
     * 获得采购组织
     *
     * @param id 编号
     * @return 采购组织
     */
    ErpPrOrganizationDO getPrOrganization(Long id);

    /**
     * 校验采购组织
     *
     * @param id 编号
     * @return 采购组织
     */
    ErpPrOrganizationDO validatePrOrganization(Long id);

    /**
     * 获得采购组织列表
     *
     * @param ids 编号列表
     * @return 采购组织列表
     */
    List<ErpPrOrganizationDO> getPrOrganizationList(Collection<Long> ids);

    /**
     * 获得采购组织 Map
     *
     * @param ids 编号列表
     * @return 采购组织 Map
     */
    default Map<Long, ErpPrOrganizationDO> getPrOrganizationMap(Collection<Long> ids) {
        return convertMap(getPrOrganizationList(ids), ErpPrOrganizationDO::getId);
    }

    /**
     * 获得采购组织分页
     *
     * @param pageReqVO 分页查询
     * @return 采购组织分页
     */
    PageResult<ErpPrOrganizationDO> getPrOrganizationPage(ErpPrOrganizationPageReqVO pageReqVO);

    /**
     * 获得指定状态的采购组织列表
     *
     * @param status 状态
     * @return 采购组织列表
     */
    List<ErpPrOrganizationDO> getPrOrganizationListByStatus(Integer status);

    /**
     * 获得采购组织树形列表
     *
     * @return 采购组织树形列表
     */
    List<ErpPrOrganizationDO> getPrOrganizationTree();

    /**
     * 更新采购组织状态
     *
     * @param id 编号
     * @param status 状态
     */
    void updatePrOrganizationStatus(Long id, Integer status);

    /**
     * 校验组织编码是否唯一
     *
     * @param id 编号
     * @param code 组织编码
     */
    void validateCodeUnique(Long id, String code);

    /**
     * 校验上级组织是否合法
     *
     * @param id 编号
     * @param parentId 上级组织编号
     */
    void validateParentOrganization(Long id, Long parentId);

} 