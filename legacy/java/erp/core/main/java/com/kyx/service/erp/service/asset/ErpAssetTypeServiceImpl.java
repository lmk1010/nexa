package com.kyx.service.erp.service.asset;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.erp.controller.admin.asset.vo.type.ErpAssetTypePageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.type.ErpAssetTypeRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.type.ErpAssetTypeSaveReqVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetTypeDO;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetTypeMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 资产类型 Service 实现类
 *
 * @author kyx
 */
@Service
@Validated
public class ErpAssetTypeServiceImpl implements ErpAssetTypeService {

    @Resource
    private ErpAssetTypeMapper assetTypeMapper;

    @Override
    public Long createAssetType(ErpAssetTypeSaveReqVO createReqVO) {
        // 校验父类型存在
        validateParentAssetType(createReqVO.getParentId());
        // 校验类型名称唯一
        validateAssetTypeNameUnique(null, createReqVO.getParentId(), createReqVO.getName());
        // 校验类型编码唯一
        validateAssetTypeCodeUnique(null, createReqVO.getCode());
        // 插入
        ErpAssetTypeDO assetType = BeanUtils.toBean(createReqVO, ErpAssetTypeDO.class);
        // 新建资产类型默认启用，避免状态混乱
        assetType.setStatus(CommonStatusEnum.ENABLE.getStatus());
        assetTypeMapper.insert(assetType);
        // 返回
        return assetType.getId();
    }

    @Override
    public void updateAssetType(ErpAssetTypeSaveReqVO updateReqVO) {
        // 校验存在
        validateAssetTypeExists(updateReqVO.getId());
        // 校验父类型存在
        validateParentAssetType(updateReqVO.getParentId());
        // 校验类型名称唯一
        validateAssetTypeNameUnique(updateReqVO.getId(), updateReqVO.getParentId(), updateReqVO.getName());
        // 校验类型编码唯一
        validateAssetTypeCodeUnique(updateReqVO.getId(), updateReqVO.getCode());
        // 校验不能设置自己为父类型
        if (ObjectUtil.equal(updateReqVO.getId(), updateReqVO.getParentId())) {
            throw exception(ASSET_TYPE_PARENT_ERROR);
        }
        // 校验不能设置自己的子类型为父类型
        validateParentAssetTypeLevel(updateReqVO.getId(), updateReqVO.getParentId());
        // 更新
        ErpAssetTypeDO updateObj = BeanUtils.toBean(updateReqVO, ErpAssetTypeDO.class);
        assetTypeMapper.updateById(updateObj);
    }

    @Override
    public void deleteAssetType(Long id) {
        // 校验存在
        validateAssetTypeExists(id);
        // 校验是否有子类型
        List<ErpAssetTypeDO> children = assetTypeMapper.selectListByParentId(id);
        if (CollUtil.isNotEmpty(children)) {
            throw exception(ASSET_TYPE_EXITS_CHILDREN);
        }
        // 删除
        assetTypeMapper.deleteById(id);
    }

    private void validateAssetTypeExists(Long id) {
        if (assetTypeMapper.selectById(id) == null) {
            throw exception(ASSET_TYPE_NOT_EXISTS);
        }
    }

    private void validateParentAssetType(Long parentId) {
        if (parentId == null || parentId == 0) {
            return;
        }
        ErpAssetTypeDO type = assetTypeMapper.selectById(parentId);
        if (type == null) {
            throw exception(ASSET_TYPE_PARENT_NOT_EXITS);
        }
    }

    private void validateAssetTypeNameUnique(Long id, Long parentId, String name) {
        ErpAssetTypeDO type = assetTypeMapper.selectByParentIdAndName(parentId, name);
        if (type == null) {
            return;
        }
        // 如果 id 为空，说明是新增，直接抛出异常
        if (id == null) {
            throw exception(ASSET_TYPE_NAME_DUPLICATE);
        }
        // 如果不是同一个类型，则抛出异常
        if (!id.equals(type.getId())) {
            throw exception(ASSET_TYPE_NAME_DUPLICATE);
        }
    }

    private void validateAssetTypeCodeUnique(Long id, String code) {
        ErpAssetTypeDO type = assetTypeMapper.selectByCode(code);
        if (type == null) {
            return;
        }
        // 如果 id 为空，说明是新增，直接抛出异常
        if (id == null) {
            throw exception(ASSET_TYPE_CODE_DUPLICATE);
        }
        // 如果不是同一个类型，则抛出异常
        if (!id.equals(type.getId())) {
            throw exception(ASSET_TYPE_CODE_DUPLICATE);
        }
    }

    private void validateParentAssetTypeLevel(Long id, Long parentId) {
        if (parentId == null || parentId == 0) {
            return;
        }
        // 递归查找父类型链
        for (int i = 0; i < Short.MAX_VALUE; i++) {
            ErpAssetTypeDO parent = assetTypeMapper.selectById(parentId);
            if (parent == null) {
                return;
            }
            // 如果父类型是当前类型的子类型，抛出异常
            if (ObjectUtil.equal(parent.getId(), id)) {
                throw exception(ASSET_TYPE_PARENT_IS_CHILD);
            }
            parentId = parent.getParentId();
            if (parentId == null || parentId == 0) {
                return;
            }
        }
    }

    @Override
    public ErpAssetTypeDO getAssetType(Long id) {
        return assetTypeMapper.selectById(id);
    }

    @Override
    public ErpAssetTypeDO validateAssetType(Long id) {
        ErpAssetTypeDO type = assetTypeMapper.selectById(id);
        if (type == null) {
            throw exception(ASSET_TYPE_NOT_EXISTS);
        }
        if (CommonStatusEnum.isDisable(type.getStatus())) {
            throw exception(ASSET_TYPE_NOT_EXISTS); // TODO: 添加类型未启用的错误码
        }
        return type;
    }

    @Override
    public List<ErpAssetTypeDO> getAssetTypeListByStatus(Integer status) {
        return assetTypeMapper.selectListByStatus(status);
    }

    @Override
    public List<ErpAssetTypeDO> getAssetTypeList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return CollUtil.newArrayList();
        }
        return assetTypeMapper.selectBatchIds(ids);
    }

    @Override
    public PageResult<ErpAssetTypeRespVO> getAssetTypeVOPage(ErpAssetTypePageReqVO pageReqVO) {
        PageResult<ErpAssetTypeDO> pageResult = assetTypeMapper.selectPage(pageReqVO);
        return BeanUtils.toBean(pageResult, ErpAssetTypeRespVO.class);
    }

    @Override
    public IPage<ErpAssetTypeRespVO> getAssetTypeVOPageWithIPage(ErpAssetTypePageReqVO pageReqVO) {
        // 创建IPage对象
        IPage<ErpAssetTypeDO> page = new Page<>(pageReqVO.getPageNo(), pageReqVO.getPageSize());
        
        // 执行分页查询
        IPage<ErpAssetTypeDO> result = assetTypeMapper.selectPageWithIPage(page, pageReqVO);
        
        // 转换结果
        IPage<ErpAssetTypeRespVO> respPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<ErpAssetTypeRespVO> respList = result.getRecords().stream()
                .map(record -> BeanUtils.toBean(record, ErpAssetTypeRespVO.class))
                .collect(Collectors.toList());
        respPage.setRecords(respList);
        
        return respPage;
    }

    @Override
    public List<ErpAssetTypeRespVO> getAssetTypeVOList() {
        List<ErpAssetTypeDO> list = assetTypeMapper.selectList();
        return BeanUtils.toBean(list, ErpAssetTypeRespVO.class);
    }

} 
