package com.kyx.service.erp.service.asset;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.erp.controller.admin.asset.vo.assetreturn.ErpAssetReturnPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.assetreturn.ErpAssetReturnRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.assetreturn.ErpAssetReturnSaveReqVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetReturnDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetCheckoutDO;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetReturnMapper;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetMapper;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetCheckoutMapper;
import com.kyx.service.business.api.dept.DeptApi;
import com.kyx.service.business.api.dept.dto.DeptRespDTO;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.foundation.common.util.collection.CollectionUtils.convertList;
import static com.kyx.foundation.common.util.collection.CollectionUtils.convertMap;
import static com.kyx.foundation.common.util.collection.CollectionUtils.convertSet;
import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static com.kyx.service.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 资产归还记录 Service 实现类
 *
 * @author kyx
 */
@Slf4j
@Service
@Validated
public class ErpAssetReturnServiceImpl implements ErpAssetReturnService {

    @Resource
    private ErpAssetReturnMapper returnMapper;
    @Resource
    private ErpAssetMapper assetMapper;
    @Resource
    private ErpAssetCheckoutMapper checkoutMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReturn(@Valid ErpAssetReturnSaveReqVO createReqVO) {
        // 1. 校验领用记录存在且状态正确
        ErpAssetCheckoutDO checkout = validateCheckoutExists(createReqVO.getCheckoutId());
        if (!checkout.getStatus().equals(1)) { // 必须是领用中状态
            throw exception(ASSET_RETURN_FAIL_STATUS_ERROR);
        }
        
        // 2. 校验资产是否存在
        validateAssetExists(createReqVO.getAssetId());
        
        // 3. 校验用户和部门是否存在
        validateUserExists(createReqVO.getReturnUserId());
        validateDeptExists(createReqVO.getReturnDeptId());
        
        // 4. 检查是否已有归还记录
        ErpAssetReturnDO existingReturn = returnMapper.selectByCheckoutId(createReqVO.getCheckoutId());
        if (existingReturn != null) {
            throw exception(ASSET_RETURN_ALREADY_EXISTS);
        }
        
        // 5. 创建归还记录
        ErpAssetReturnDO returnRecord = BeanUtils.toBean(createReqVO, ErpAssetReturnDO.class);
        returnRecord.setStatus(1); // 已归还（等待接收确认）
        returnMapper.insert(returnRecord);
        
        return returnRecord.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReturn(@Valid ErpAssetReturnSaveReqVO updateReqVO) {
        // 校验存在
        validateReturnExists(updateReqVO.getId());
        
        // 更新归还记录
        ErpAssetReturnDO updateObj = BeanUtils.toBean(updateReqVO, ErpAssetReturnDO.class);
        returnMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReturn(Long id) {
        // 校验存在
        ErpAssetReturnDO returnRecord = validateReturnExists(id);
        
        // 只有已归还状态的记录才能删除
        if (!returnRecord.getStatus().equals(1)) {
            throw exception(ASSET_RETURN_DELETE_FAIL_STATUS_ERROR);
        }
        
        // 删除归还记录
        returnMapper.deleteById(id);
    }

    private ErpAssetReturnDO validateReturnExists(Long id) {
        ErpAssetReturnDO returnRecord = returnMapper.selectById(id);
        if (returnRecord == null) {
            throw exception(ASSET_RETURN_NOT_EXISTS);
        }
        return returnRecord;
    }

    @Override
    public ErpAssetReturnDO getReturn(Long id) {
        return returnMapper.selectById(id);
    }

    @Override
    public PageResult<ErpAssetReturnRespVO> getReturnPage(ErpAssetReturnPageReqVO pageReqVO) {
        PageResult<ErpAssetReturnDO> pageResult = returnMapper.selectPage(pageReqVO);
        return buildReturnVOPageResult(pageResult);
    }

    @Override
    public List<ErpAssetReturnRespVO> getReturnListByAssetId(Long assetId) {
        List<ErpAssetReturnDO> list = returnMapper.selectListByAssetId(assetId);
        return buildReturnVOList(list);
    }

    @Override
    public List<ErpAssetReturnRespVO> getReturnListByUserId(Long userId) {
        List<ErpAssetReturnDO> list = returnMapper.selectListByUserId(userId);
        return buildReturnVOList(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receiveReturn(Long returnId, String receiverRemark) {
        // 1. 校验归还记录存在
        ErpAssetReturnDO returnRecord = validateReturnExists(returnId);
        
        // 2. 校验状态（只有已归还的才能接收确认）
        if (!returnRecord.getStatus().equals(1)) {
            throw exception(ASSET_RETURN_RECEIVE_FAIL_STATUS_ERROR);
        }
        
        // 3. 更新归还记录状态
        returnRecord.setReceiverUserId(getLoginUserId());
        returnRecord.setReceiverTime(LocalDateTime.now());
        returnRecord.setReceiverRemark(receiverRemark);
        returnRecord.setStatus(2); // 已接收确认
        returnMapper.updateById(returnRecord);
        
        log.info("资产归还接收确认: returnId={}, assetId={}, receiverId={}", 
                returnId, returnRecord.getAssetId(), getLoginUserId());
    }

    private PageResult<ErpAssetReturnRespVO> buildReturnVOPageResult(PageResult<ErpAssetReturnDO> pageResult) {
        List<ErpAssetReturnRespVO> list = buildReturnVOList(pageResult.getList());
        return new PageResult<>(list, pageResult.getTotal());
    }

    private List<ErpAssetReturnRespVO> buildReturnVOList(List<ErpAssetReturnDO> list) {
        if (list.isEmpty()) {
            return convertList(list, returnRecord -> BeanUtils.toBean(returnRecord, ErpAssetReturnRespVO.class));
        }
        
        // 获取资产信息
        Map<Long, ErpAssetDO> assetMap = convertMap(
                assetMapper.selectBatchIds(convertList(list, ErpAssetReturnDO::getAssetId)), 
                ErpAssetDO::getId);
        
        // 获取领用记录信息
        Map<Long, ErpAssetCheckoutDO> checkoutMap = convertMap(
                checkoutMapper.selectBatchIds(convertList(list, ErpAssetReturnDO::getCheckoutId)), 
                ErpAssetCheckoutDO::getId);
        
        // 获取用户信息
        Set<Long> userIds = list.stream()
                .flatMap(returnRecord -> Stream.of(returnRecord.getReturnUserId(), returnRecord.getReceiverUserId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        
        // 获取部门信息  
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(
                convertList(list, ErpAssetReturnDO::getReturnDeptId));
        
        // 构建VO
        return convertList(list, returnRecord -> {
            ErpAssetReturnRespVO vo = BeanUtils.toBean(returnRecord, ErpAssetReturnRespVO.class);
            
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
            
            // 设置归还人信息
            AdminUserRespDTO returnUser = userMap.get(returnRecord.getReturnUserId());
            if (returnUser != null) {
                vo.setReturnUserName(returnUser.getNickname());
            }
            
            // 设置接收人信息
            if (returnRecord.getReceiverUserId() != null) {
                AdminUserRespDTO receiverUser = userMap.get(returnRecord.getReceiverUserId());
                if (receiverUser != null) {
                    vo.setReceiverUserName(receiverUser.getNickname());
                }
            }
            
            // 设置部门信息
            DeptRespDTO dept = deptMap.get(returnRecord.getReturnDeptId());
            if (dept != null) {
                vo.setReturnDeptName(dept.getName());
            }
            
            return vo;
        });
    }

    private ErpAssetDO validateAssetExists(Long assetId) {
        ErpAssetDO asset = assetMapper.selectById(assetId);
        if (asset == null) {
            throw exception(ASSET_NOT_EXISTS);
        }
        return asset;
    }

    private ErpAssetCheckoutDO validateCheckoutExists(Long checkoutId) {
        ErpAssetCheckoutDO checkout = checkoutMapper.selectById(checkoutId);
        if (checkout == null) {
            throw exception(ASSET_CHECKOUT_NOT_EXISTS);
        }
        return checkout;
    }

    private void validateUserExists(Long userId) {
        AdminUserRespDTO user = adminUserApi.getUser(userId).getCheckedData();
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }
    }

    private void validateDeptExists(Long deptId) {
        DeptRespDTO dept = deptApi.getDept(deptId).getCheckedData();
        if (dept == null) {
            throw exception(DEPT_NOT_EXISTS);
        }
    }

} 