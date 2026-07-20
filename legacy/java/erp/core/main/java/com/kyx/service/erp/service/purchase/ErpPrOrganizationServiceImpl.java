package com.kyx.service.erp.service.purchase;

import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.erp.controller.admin.purchase.vo.prorganization.ErpPrOrganizationPageReqVO;
import com.kyx.service.erp.controller.admin.purchase.vo.prorganization.ErpPrOrganizationSaveReqVO;
import com.kyx.service.erp.dal.dataobject.purchase.ErpPrOrganizationDO;
import com.kyx.service.erp.dal.mysql.purchase.ErpPrOrganizationMapper;
import static com.kyx.service.erp.enums.ErrorCodeConstants.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ERP 采购组织 Service 实现类
 *
 * @author MK
 */
@Service
@Validated
@Slf4j
public class ErpPrOrganizationServiceImpl implements ErpPrOrganizationService {

    @Resource
    private ErpPrOrganizationMapper prOrganizationMapper;

    @Override
    public Long createPrOrganization(@Valid ErpPrOrganizationSaveReqVO createReqVO) {
        // 校验组织编码唯一性
        validateCodeUnique(null, createReqVO.getCode());
        
        // 校验上级组织
        if (createReqVO.getParentId() != null && createReqVO.getParentId() > 0) {
            validateParentOrganization(null, createReqVO.getParentId());
        }

        // 插入
        ErpPrOrganizationDO prOrganization = BeanUtils.toBean(createReqVO, ErpPrOrganizationDO.class);
        prOrganizationMapper.insert(prOrganization);
        // 返回
        return prOrganization.getId();
    }

    @Override
    public void updatePrOrganization(@Valid ErpPrOrganizationSaveReqVO updateReqVO) {
        // 校验存在
        validatePrOrganization(updateReqVO.getId());
        
        // 校验组织编码唯一性
        validateCodeUnique(updateReqVO.getId(), updateReqVO.getCode());
        
        // 校验上级组织
        if (updateReqVO.getParentId() != null && updateReqVO.getParentId() > 0) {
            validateParentOrganization(updateReqVO.getId(), updateReqVO.getParentId());
        }

        // 更新
        ErpPrOrganizationDO updateObj = BeanUtils.toBean(updateReqVO, ErpPrOrganizationDO.class);
        prOrganizationMapper.updateById(updateObj);
    }

    @Override
    public void deletePrOrganization(Long id) {
        // 校验存在
        validatePrOrganization(id);
        
        // 校验是否有子组织
        List<ErpPrOrganizationDO> children = prOrganizationMapper.selectListByParentId(id);
        if (!children.isEmpty()) {
            throw ServiceExceptionUtil.exception(PR_ORGANIZATION_EXITS_CHILDREN);
        }

        // 删除
        prOrganizationMapper.deleteById(id);
    }

    @Override
    public void deletePrOrganizationBatch(List<Long> ids) {
        // 校验存在
        for (Long id : ids) {
            validatePrOrganization(id);
            
            // 校验是否有子组织
            List<ErpPrOrganizationDO> children = prOrganizationMapper.selectListByParentId(id);
            if (!children.isEmpty()) {
                ErpPrOrganizationDO organization = prOrganizationMapper.selectById(id);
                throw ServiceExceptionUtil.exception(PR_ORGANIZATION_EXITS_CHILDREN, 
                    organization != null ? organization.getName() : "未知组织");
            }
        }

        // 删除
        prOrganizationMapper.deleteBatchIds(ids);
    }

    @Override
    public ErpPrOrganizationDO getPrOrganization(Long id) {
        return prOrganizationMapper.selectById(id);
    }

    @Override
    public ErpPrOrganizationDO validatePrOrganization(Long id) {
        ErpPrOrganizationDO prOrganization = prOrganizationMapper.selectById(id);
        if (prOrganization == null) {
            throw ServiceExceptionUtil.exception(PR_ORGANIZATION_NOT_EXISTS);
        }
        return prOrganization;
    }

    @Override
    public List<ErpPrOrganizationDO> getPrOrganizationList(Collection<Long> ids) {
        return prOrganizationMapper.selectBatchIds(ids);
    }

    @Override
    public PageResult<ErpPrOrganizationDO> getPrOrganizationPage(ErpPrOrganizationPageReqVO pageReqVO) {
        return prOrganizationMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpPrOrganizationDO> getPrOrganizationListByStatus(Integer status) {
        return prOrganizationMapper.selectListByStatus(status);
    }

    @Override
    public List<ErpPrOrganizationDO> getPrOrganizationTree() {
        // 获取所有组织
        List<ErpPrOrganizationDO> allOrganizations = prOrganizationMapper.selectList();
        
        // 构建树形结构
        return buildTree(allOrganizations);
    }

    @Override
    public void updatePrOrganizationStatus(Long id, Integer status) {
        // 校验存在
        validatePrOrganization(id);
        
        // 更新状态
        ErpPrOrganizationDO updateObj = new ErpPrOrganizationDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        prOrganizationMapper.updateById(updateObj);
    }

    @Override
    public void validateCodeUnique(Long id, String code) {
        ErpPrOrganizationDO organization = prOrganizationMapper.selectByCode(code);
        if (organization == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的组织
        if (id == null) {
            throw ServiceExceptionUtil.exception(PR_ORGANIZATION_CODE_DUPLICATE);
        }
        if (!organization.getId().equals(id)) {
            throw ServiceExceptionUtil.exception(PR_ORGANIZATION_CODE_DUPLICATE);
        }
    }

    @Override
    public void validateParentOrganization(Long id, Long parentId) {
        if (parentId == null || parentId <= 0) {
            return;
        }
        
        // 校验父组织存在
        ErpPrOrganizationDO parentOrganization = prOrganizationMapper.selectById(parentId);
        if (parentOrganization == null) {
            throw ServiceExceptionUtil.exception(PR_ORGANIZATION_PARENT_NOT_EXISTS);
        }
        
        // 校验父组织状态
        if (!CommonStatusEnum.ENABLE.getStatus().equals(parentOrganization.getStatus())) {
            throw ServiceExceptionUtil.exception(PR_ORGANIZATION_PARENT_DISABLED);
        }
        
        // 不能设置自己为父组织
        if (id != null && id.equals(parentId)) {
            throw ServiceExceptionUtil.exception(PR_ORGANIZATION_PARENT_ERROR);
        }
        
        // 不能设置自己的子组织为父组织
        if (id != null) {
            List<Long> childrenIds = getChildrenIds(id);
            if (childrenIds.contains(parentId)) {
                throw ServiceExceptionUtil.exception(PR_ORGANIZATION_PARENT_ERROR);
            }
        }
    }

    /**
     * 获取组织的所有子组织 ID
     *
     * @param id 组织 ID
     * @return 子组织 ID 列表
     */
    private List<Long> getChildrenIds(Long id) {
        List<Long> result = new ArrayList<>();
        List<ErpPrOrganizationDO> children = prOrganizationMapper.selectListByParentId(id);
        for (ErpPrOrganizationDO child : children) {
            result.add(child.getId());
            result.addAll(getChildrenIds(child.getId()));
        }
        return result;
    }

    /**
     * 构建树形结构
     *
     * @param allOrganizations 所有组织列表
     * @return 树形组织列表
     */
    private List<ErpPrOrganizationDO> buildTree(List<ErpPrOrganizationDO> allOrganizations) {
        // 按父ID分组
        Map<Long, List<ErpPrOrganizationDO>> parentGroupMap = allOrganizations.stream()
                .collect(Collectors.groupingBy(org -> org.getParentId() == null ? 0L : org.getParentId()));

        // 递归构建树
        return buildTreeRecursive(parentGroupMap, 0L);
    }

    /**
     * 递归构建树形结构
     *
     * @param parentGroupMap 按父ID分组的Map
     * @param parentId 父ID
     * @return 子树列表
     */
    private List<ErpPrOrganizationDO> buildTreeRecursive(Map<Long, List<ErpPrOrganizationDO>> parentGroupMap, Long parentId) {
        List<ErpPrOrganizationDO> children = parentGroupMap.get(parentId);
        if (children == null || children.isEmpty()) {
            return new ArrayList<>();
        }

        return children.stream()
                .sorted(Comparator.comparing(ErpPrOrganizationDO::getSort)
                        .thenComparing(ErpPrOrganizationDO::getId))
                .peek(child -> {
                    // 递归获取子节点
                    List<ErpPrOrganizationDO> subChildren = buildTreeRecursive(parentGroupMap, child.getId());
                    // 这里需要在DO中添加children字段，或者使用VO
                    // child.setChildren(subChildren);
                })
                .collect(Collectors.toList());
    }

} 