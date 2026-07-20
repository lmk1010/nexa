package com.kyx.service.erp.service.asset;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.erp.controller.admin.asset.vo.myassets.ErpMyOwnedAssetsPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.myassets.ErpMyOwnedAssetsRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.myassets.ErpMyCheckoutPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.myassets.ErpMyCheckoutRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.myassets.ErpMyReturnPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.myassets.ErpMyReturnRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.myassets.ErpMyTransferPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.myassets.ErpMyTransferRespVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetOwnershipDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetCheckoutDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetReturnDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetTransferDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetCategoryDO;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetOwnershipMapper;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetCheckoutMapper;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetReturnMapper;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetTransferMapper;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetMapper;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetCategoryMapper;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.kyx.foundation.common.util.collection.CollectionUtils.convertList;
import static com.kyx.foundation.common.util.collection.CollectionUtils.convertMap;
import static com.kyx.foundation.common.util.collection.CollectionUtils.convertSet;

/**
 * ERP 我的资产 Service 实现类
 *
 * @author kyx
 */
@Service
@Validated
public class ErpMyAssetsServiceImpl implements ErpMyAssetsService {

    @Resource
    private ErpAssetOwnershipMapper ownershipMapper;
    @Resource
    private ErpAssetCheckoutMapper checkoutMapper;
    @Resource
    private ErpAssetReturnMapper returnMapper;
    @Resource
    private ErpAssetTransferMapper transferMapper;
    @Resource
    private ErpAssetMapper assetMapper;
    @Resource
    private ErpAssetCategoryMapper categoryMapper;
    @Resource
    private AdminUserApi adminUserApi;

    @Override
    public PageResult<ErpMyOwnedAssetsRespVO> getMyOwnedAssetsPage(Long userId, ErpMyOwnedAssetsPageReqVO pageReqVO) {
        // 1. 查询我的所有权关系（简化查询：由于归还时直接删除记录，所以存在记录就表示正在使用中）
        PageResult<ErpAssetOwnershipDO> pageResult = ownershipMapper.selectPage(pageReqVO, 
            new LambdaQueryWrapperX<ErpAssetOwnershipDO>()
                .eq(ErpAssetOwnershipDO::getCurrentUserId, userId)
                .orderByDesc(ErpAssetOwnershipDO::getStartTime));

        List<ErpAssetOwnershipDO> ownershipList = pageResult.getList();
        if (ownershipList.isEmpty()) {
            return new PageResult<>(convertList(ownershipList, ownership -> 
                BeanUtils.toBean(ownership, ErpMyOwnedAssetsRespVO.class)), pageResult.getTotal());
        }

        // 2. 获取资产编号列表
        Set<Long> assetIds = convertSet(ownershipList, ErpAssetOwnershipDO::getAssetId);

        // 3. 查询资产信息
        LambdaQueryWrapperX<ErpAssetDO> assetQuery = new LambdaQueryWrapperX<ErpAssetDO>()
            .in(ErpAssetDO::getId, assetIds);
        
        // 添加搜索条件
        if (pageReqVO.getAssetNo() != null) {
            assetQuery.like(ErpAssetDO::getAssetNo, pageReqVO.getAssetNo());
        }
        if (pageReqVO.getName() != null) {
            assetQuery.like(ErpAssetDO::getName, pageReqVO.getName());
        }
        if (pageReqVO.getCategoryId() != null) {
            assetQuery.eq(ErpAssetDO::getCategoryId, pageReqVO.getCategoryId());
        }
        if (pageReqVO.getStatus() != null) {
            assetQuery.eq(ErpAssetDO::getStatus, pageReqVO.getStatus());
        }

        List<ErpAssetDO> assetList = assetMapper.selectList(assetQuery);
        Map<Long, ErpAssetDO> assetMap = convertMap(assetList, ErpAssetDO::getId);

        // 4. 查询分类信息
        Set<Long> categoryIds = convertSet(assetList, ErpAssetDO::getCategoryId);
        categoryIds.removeIf(id -> id == null);
        Map<Long, ErpAssetCategoryDO> categoryMap = convertMap(
            categoryMapper.selectBatchIds(categoryIds), ErpAssetCategoryDO::getId);

        // 5. 构建返回结果
        List<ErpMyOwnedAssetsRespVO> resultList = convertList(ownershipList, ownership -> {
            ErpMyOwnedAssetsRespVO vo = BeanUtils.toBean(ownership, ErpMyOwnedAssetsRespVO.class);
            vo.setOwnershipId(ownership.getId());
            vo.setOwnershipStatus(ownership.getStatus());
            vo.setStartTime(ownership.getStartTime());
            vo.setEndTime(ownership.getEndTime());
            vo.setRemark(ownership.getRemark());

            // 设置资产信息
            ErpAssetDO asset = assetMap.get(ownership.getAssetId());
            if (asset != null) {
                vo.setAssetId(asset.getId());
                vo.setAssetNo(asset.getAssetNo());
                vo.setName(asset.getName());
                vo.setType(asset.getType());
                vo.setCategoryId(asset.getCategoryId());
                vo.setSpecification(asset.getSpecification());
                vo.setBrand(asset.getBrand());
                vo.setModel(asset.getModel());
                vo.setLocation(asset.getLocation());
                vo.setPurchasePrice(asset.getPurchasePrice());
                vo.setCurrentValue(asset.getCurrentValue());
                vo.setStatus(asset.getStatus());
                vo.setConditionStatus(asset.getConditionStatus());
                vo.setCreateTime(asset.getCreateTime());

                // 设置分类名称
                if (asset.getCategoryId() != null) {
                    ErpAssetCategoryDO category = categoryMap.get(asset.getCategoryId());
                    if (category != null) {
                        vo.setCategoryName(category.getName());
                    }
                }
            }

            return vo;
        });

        return new PageResult<>(resultList, pageResult.getTotal());
    }

    @Override
    public PageResult<ErpMyCheckoutRespVO> getMyCheckoutPage(Long userId, ErpMyCheckoutPageReqVO pageReqVO) {
        // 1. 查询我的领用记录
        PageResult<ErpAssetCheckoutDO> pageResult = checkoutMapper.selectPage(pageReqVO, 
            new LambdaQueryWrapperX<ErpAssetCheckoutDO>()
                .eq(ErpAssetCheckoutDO::getCheckoutUserId, userId)
                .eqIfPresent(ErpAssetCheckoutDO::getStatus, pageReqVO.getStatus())
                .eqIfPresent(ErpAssetCheckoutDO::getApprovalStatus, pageReqVO.getApprovalStatus())
                .betweenIfPresent(ErpAssetCheckoutDO::getCheckoutDate, pageReqVO.getCheckoutDate())
                .orderByDesc(ErpAssetCheckoutDO::getCheckoutDate));

        List<ErpAssetCheckoutDO> checkoutList = pageResult.getList();
        if (checkoutList.isEmpty()) {
            return new PageResult<>(convertList(checkoutList, checkout -> 
                BeanUtils.toBean(checkout, ErpMyCheckoutRespVO.class)), pageResult.getTotal());
        }

        // 2. 获取资产编号列表
        Set<Long> assetIds = convertSet(checkoutList, ErpAssetCheckoutDO::getAssetId);

        // 3. 查询资产信息
        LambdaQueryWrapperX<ErpAssetDO> assetQuery = new LambdaQueryWrapperX<ErpAssetDO>()
            .in(ErpAssetDO::getId, assetIds);
        
        // 添加资产名称搜索条件
        if (pageReqVO.getAssetName() != null) {
            assetQuery.like(ErpAssetDO::getName, pageReqVO.getAssetName());
        }

        List<ErpAssetDO> assetList = assetMapper.selectList(assetQuery);
        Map<Long, ErpAssetDO> assetMap = convertMap(assetList, ErpAssetDO::getId);

        // 4. 查询审批人信息
        Set<Long> approverUserIds = convertSet(checkoutList, ErpAssetCheckoutDO::getApproverUserId);
        approverUserIds.removeIf(id -> id == null);
        Map<Long, AdminUserRespDTO> userMap = convertMap(
            adminUserApi.getUserList(approverUserIds).getCheckedData(), AdminUserRespDTO::getId);

        // 5. 构建返回结果
        List<ErpMyCheckoutRespVO> resultList = convertList(checkoutList, checkout -> {
            ErpMyCheckoutRespVO vo = BeanUtils.toBean(checkout, ErpMyCheckoutRespVO.class);

            // 设置资产信息
            ErpAssetDO asset = assetMap.get(checkout.getAssetId());
            if (asset != null) {
                vo.setAssetNo(asset.getAssetNo());
                vo.setAssetName(asset.getName());
                vo.setAssetType(asset.getType());
            }

            // 设置审批人信息
            if (checkout.getApproverUserId() != null) {
                AdminUserRespDTO approver = userMap.get(checkout.getApproverUserId());
                if (approver != null) {
                    vo.setApproverUserName(approver.getNickname());
                }
            }

            return vo;
        });

        return new PageResult<>(resultList, pageResult.getTotal());
    }

    @Override
    public PageResult<ErpMyReturnRespVO> getMyReturnPage(Long userId, ErpMyReturnPageReqVO pageReqVO) {
        // 1. 查询我的归还记录
        PageResult<ErpAssetReturnDO> pageResult = returnMapper.selectPage(pageReqVO, 
            new LambdaQueryWrapperX<ErpAssetReturnDO>()
                .eq(ErpAssetReturnDO::getReturnUserId, userId)
                .eqIfPresent(ErpAssetReturnDO::getStatus, pageReqVO.getStatus())
                .eqIfPresent(ErpAssetReturnDO::getReturnCondition, pageReqVO.getReturnCondition())
                .betweenIfPresent(ErpAssetReturnDO::getReturnDate, pageReqVO.getReturnDate())
                .orderByDesc(ErpAssetReturnDO::getReturnDate));

        List<ErpAssetReturnDO> returnList = pageResult.getList();
        if (returnList.isEmpty()) {
            return new PageResult<>(convertList(returnList, returnRecord -> 
                BeanUtils.toBean(returnRecord, ErpMyReturnRespVO.class)), pageResult.getTotal());
        }

        // 2. 获取资产编号列表
        Set<Long> assetIds = convertSet(returnList, ErpAssetReturnDO::getAssetId);

        // 3. 查询资产信息
        LambdaQueryWrapperX<ErpAssetDO> assetQuery = new LambdaQueryWrapperX<ErpAssetDO>()
            .in(ErpAssetDO::getId, assetIds);
        
        // 添加资产名称搜索条件
        if (pageReqVO.getAssetName() != null) {
            assetQuery.like(ErpAssetDO::getName, pageReqVO.getAssetName());
        }

        List<ErpAssetDO> assetList = assetMapper.selectList(assetQuery);
        Map<Long, ErpAssetDO> assetMap = convertMap(assetList, ErpAssetDO::getId);

        // 4. 查询领用记录信息
        Set<Long> checkoutIds = convertSet(returnList, ErpAssetReturnDO::getCheckoutId);
        Map<Long, ErpAssetCheckoutDO> checkoutMap = convertMap(
            checkoutMapper.selectBatchIds(checkoutIds), ErpAssetCheckoutDO::getId);

        // 5. 查询接收人信息
        Set<Long> receiverUserIds = returnList.stream()
            .map(ErpAssetReturnDO::getReceiverUserId)
            .filter(id -> id != null)
            .collect(Collectors.toSet());
        Map<Long, com.kyx.service.business.api.user.dto.AdminUserRespDTO> userMap = adminUserApi.getUserMap(receiverUserIds);

        // 6. 构建返回结果
        List<ErpMyReturnRespVO> resultList = convertList(returnList, returnRecord -> {
            ErpMyReturnRespVO vo = BeanUtils.toBean(returnRecord, ErpMyReturnRespVO.class);

            // 设置资产信息
            ErpAssetDO asset = assetMap.get(returnRecord.getAssetId());
            if (asset != null) {
                vo.setAssetNo(asset.getAssetNo());
                vo.setAssetName(asset.getName());
                vo.setAssetType(asset.getType());
            }

            // 设置领用记录信息
            ErpAssetCheckoutDO checkout = checkoutMap.get(returnRecord.getCheckoutId());
            if (checkout != null) {
                vo.setCheckoutDate(checkout.getCheckoutDate());
                vo.setExpectedReturnDate(checkout.getExpectedReturnDate());
                vo.setCheckoutReason(checkout.getCheckoutReason());
            }

            // 设置接收人信息
            if (returnRecord.getReceiverUserId() != null) {
                com.kyx.service.business.api.user.dto.AdminUserRespDTO receiverUser = userMap.get(returnRecord.getReceiverUserId());
                if (receiverUser != null) {
                    vo.setReceiverUserName(receiverUser.getNickname());
                }
            }

            return vo;
        });

        return new PageResult<>(resultList, pageResult.getTotal());
    }

    @Override
    public PageResult<ErpMyTransferRespVO> getMyTransferPage(Long userId, ErpMyTransferPageReqVO pageReqVO) {
        // 1. 查询我发起的转移记录
        PageResult<ErpAssetTransferDO> pageResult = transferMapper.selectPage(pageReqVO, 
            new LambdaQueryWrapperX<ErpAssetTransferDO>()
                .eq(ErpAssetTransferDO::getFromUserId, userId)
                .eqIfPresent(ErpAssetTransferDO::getStatus, pageReqVO.getStatus())
                .eqIfPresent(ErpAssetTransferDO::getApprovalStatus, pageReqVO.getApprovalStatus())
                .betweenIfPresent(ErpAssetTransferDO::getTransferDate, pageReqVO.getTransferDate())
                .orderByDesc(ErpAssetTransferDO::getTransferDate));

        List<ErpAssetTransferDO> transferList = pageResult.getList();
        if (transferList.isEmpty()) {
            return new PageResult<>(convertList(transferList, transfer -> 
                BeanUtils.toBean(transfer, ErpMyTransferRespVO.class)), pageResult.getTotal());
        }

        // 2. 获取资产编号列表
        Set<Long> assetIds = convertSet(transferList, ErpAssetTransferDO::getAssetId);

        // 3. 查询资产信息
        LambdaQueryWrapperX<ErpAssetDO> assetQuery = new LambdaQueryWrapperX<ErpAssetDO>()
            .in(ErpAssetDO::getId, assetIds);
        
        // 添加资产名称搜索条件
        if (pageReqVO.getAssetName() != null) {
            assetQuery.like(ErpAssetDO::getName, pageReqVO.getAssetName());
        }

        List<ErpAssetDO> assetList = assetMapper.selectList(assetQuery);
        Map<Long, ErpAssetDO> assetMap = convertMap(assetList, ErpAssetDO::getId);

        // 4. 查询接收人和审批人信息
        Set<Long> userIds = convertSet(transferList, ErpAssetTransferDO::getToUserId);
        Set<Long> approverUserIds = convertSet(transferList, ErpAssetTransferDO::getApproverUserId);
        approverUserIds.removeIf(id -> id == null);
        userIds.addAll(approverUserIds);
        
        Map<Long, AdminUserRespDTO> userMap = convertMap(
            adminUserApi.getUserList(userIds).getCheckedData(), AdminUserRespDTO::getId);

        // 5. 添加接收人姓名搜索条件
        if (pageReqVO.getToUserName() != null && !pageReqVO.getToUserName().trim().isEmpty()) {
            transferList = transferList.stream()
                .filter(transfer -> {
                    AdminUserRespDTO toUser = userMap.get(transfer.getToUserId());
                    return toUser != null && (
                        (toUser.getNickname() != null && toUser.getNickname().contains(pageReqVO.getToUserName())) ||
                        (toUser.getUsername() != null && toUser.getUsername().contains(pageReqVO.getToUserName()))
                    );
                })
                .collect(Collectors.toList());
        }

        // 6. 构建返回结果
        return new PageResult<>(convertList(transferList, transfer -> {
            ErpMyTransferRespVO respVO = BeanUtils.toBean(transfer, ErpMyTransferRespVO.class);
            
            // 设置资产信息
            ErpAssetDO asset = assetMap.get(transfer.getAssetId());
            if (asset != null) {
                respVO.setAssetNo(asset.getAssetNo());
                respVO.setAssetName(asset.getName());
                respVO.setAssetType(asset.getType());
            }
            
            // 设置接收人信息
            AdminUserRespDTO toUser = userMap.get(transfer.getToUserId());
            if (toUser != null) {
                respVO.setToUserName(toUser.getNickname());
                respVO.setToDeptId(toUser.getDeptId());
                // 这里可以添加部门名称，如果需要的话
            }
            
            // 设置审批人信息
            if (transfer.getApproverUserId() != null) {
                AdminUserRespDTO approver = userMap.get(transfer.getApproverUserId());
                if (approver != null) {
                    respVO.setApproverUserName(approver.getNickname());
                }
            }
            
            return respVO;
        }), pageResult.getTotal());
    }

} 