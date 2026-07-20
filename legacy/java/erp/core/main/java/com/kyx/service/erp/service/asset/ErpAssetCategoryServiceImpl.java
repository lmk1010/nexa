package com.kyx.service.erp.service.asset;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.erp.controller.admin.asset.vo.category.ErpAssetCategoryPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.category.ErpAssetCategoryRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.category.ErpAssetCategorySaveReqVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetCategoryDO;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetCategoryMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 资产分类 Service 实现类
 *
 * @author kyx
 */
@Service
@Validated
public class ErpAssetCategoryServiceImpl implements ErpAssetCategoryService {

    @Resource
    private ErpAssetCategoryMapper assetCategoryMapper;

    @Resource
    @Lazy
    private ErpAssetService assetService;

    @Override
    public Long createAssetCategory(ErpAssetCategorySaveReqVO createReqVO) {
        // 校验父分类存在
        validateParentAssetCategory(createReqVO.getParentId());
        // 校验分类名称唯一
        validateAssetCategoryNameUnique(null, createReqVO.getParentId(), createReqVO.getName());
        // 校验分类编码唯一
        validateAssetCategoryCodeUnique(null, createReqVO.getCode());
        // 插入
        ErpAssetCategoryDO assetCategory = BeanUtils.toBean(createReqVO, ErpAssetCategoryDO.class);
        assetCategoryMapper.insert(assetCategory);
        // 返回
        return assetCategory.getId();
    }

    @Override
    public void updateAssetCategory(ErpAssetCategorySaveReqVO updateReqVO) {
        // 校验存在
        validateAssetCategoryExists(updateReqVO.getId());
        // 校验父分类存在
        validateParentAssetCategory(updateReqVO.getParentId());
        // 校验分类名称唯一
        validateAssetCategoryNameUnique(updateReqVO.getId(), updateReqVO.getParentId(), updateReqVO.getName());
        // 校验分类编码唯一
        validateAssetCategoryCodeUnique(updateReqVO.getId(), updateReqVO.getCode());
        // 校验不能设置自己为父分类
        if (ObjectUtil.equal(updateReqVO.getId(), updateReqVO.getParentId())) {
            throw exception(ASSET_CATEGORY_PARENT_ERROR);
        }
        // 校验不能设置自己的子分类为父分类
        validateParentAssetCategoryLevel(updateReqVO.getId(), updateReqVO.getParentId());
        // 更新
        ErpAssetCategoryDO updateObj = BeanUtils.toBean(updateReqVO, ErpAssetCategoryDO.class);
        assetCategoryMapper.updateById(updateObj);
    }

    @Override
    public void deleteAssetCategory(Long id) {
        // 校验存在
        validateAssetCategoryExists(id);
        // 校验是否有子分类
        List<ErpAssetCategoryDO> children = assetCategoryMapper.selectListByParentId(id);
        if (CollUtil.isNotEmpty(children)) {
            throw exception(ASSET_CATEGORY_EXITS_CHILDREN);
        }
        // 校验是否有资产使用该分类
        Long assetCount = assetService.getAssetCountByCategoryId(id);
        if (assetCount > 0) {
            throw exception(ASSET_CATEGORY_EXITS_ASSET);
        }
        // 删除
        assetCategoryMapper.deleteById(id);
    }

    private void validateAssetCategoryExists(Long id) {
        if (assetCategoryMapper.selectById(id) == null) {
            throw exception(ASSET_CATEGORY_NOT_EXISTS);
        }
    }

    private void validateParentAssetCategory(Long parentId) {
        if (parentId == null || parentId == 0) {
            return;
        }
        ErpAssetCategoryDO category = assetCategoryMapper.selectById(parentId);
        if (category == null) {
            throw exception(ASSET_CATEGORY_PARENT_NOT_EXITS);
        }
    }

    private void validateAssetCategoryNameUnique(Long id, Long parentId, String name) {
        ErpAssetCategoryDO category = assetCategoryMapper.selectByParentIdAndName(parentId, name);
        if (category == null) {
            return;
        }
        // 如果 id 为空，说明是新增，直接抛出异常
        if (id == null) {
            throw exception(ASSET_CATEGORY_NAME_DUPLICATE);
        }
        // 如果不是同一个分类，则抛出异常
        if (!id.equals(category.getId())) {
            throw exception(ASSET_CATEGORY_NAME_DUPLICATE);
        }
    }

    private void validateAssetCategoryCodeUnique(Long id, String code) {
        ErpAssetCategoryDO category = assetCategoryMapper.selectByCode(code);
        if (category == null) {
            return;
        }
        // 如果 id 为空，说明是新增，直接抛出异常
        if (id == null) {
            throw exception(ASSET_CATEGORY_CODE_DUPLICATE);
        }
        // 如果不是同一个分类，则抛出异常
        if (!id.equals(category.getId())) {
            throw exception(ASSET_CATEGORY_CODE_DUPLICATE);
        }
    }

    private void validateParentAssetCategoryLevel(Long id, Long parentId) {
        if (parentId == null || parentId == 0) {
            return;
        }
        // 递归查找父分类链
        for (int i = 0; i < Short.MAX_VALUE; i++) {
            ErpAssetCategoryDO parent = assetCategoryMapper.selectById(parentId);
            if (parent == null) {
                return;
            }
            // 如果父分类是当前分类的子分类，抛出异常
            if (ObjectUtil.equal(parent.getId(), id)) {
                throw exception(ASSET_CATEGORY_PARENT_IS_CHILD);
            }
            parentId = parent.getParentId();
            if (parentId == null || parentId == 0) {
                return;
            }
        }
    }

    @Override
    public ErpAssetCategoryDO getAssetCategory(Long id) {
        return assetCategoryMapper.selectById(id);
    }

    @Override
    public ErpAssetCategoryDO validateAssetCategory(Long id) {
        ErpAssetCategoryDO category = assetCategoryMapper.selectById(id);
        if (category == null) {
            throw exception(ASSET_CATEGORY_NOT_EXISTS);
        }
        if (CommonStatusEnum.isDisable(category.getStatus())) {
            throw exception(ASSET_CATEGORY_NOT_EXISTS); // TODO: 添加分类未启用的错误码
        }
        return category;
    }

    @Override
    public List<ErpAssetCategoryDO> getAssetCategoryListByStatus(Integer status) {
        return assetCategoryMapper.selectListByStatus(status);
    }

    @Override
    public List<ErpAssetCategoryDO> getAssetCategoryList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return CollUtil.newArrayList();
        }
        return assetCategoryMapper.selectBatchIds(ids);
    }

    @Override
    public PageResult<ErpAssetCategoryRespVO> getAssetCategoryVOPage(ErpAssetCategoryPageReqVO pageReqVO) {
        PageResult<ErpAssetCategoryDO> pageResult = assetCategoryMapper.selectPage(pageReqVO);
        return BeanUtils.toBean(pageResult, ErpAssetCategoryRespVO.class);
    }

    @Override
    public IPage<ErpAssetCategoryRespVO> getAssetCategoryVOPageWithIPage(ErpAssetCategoryPageReqVO pageReqVO) {
        // 创建IPage对象
        IPage<ErpAssetCategoryDO> page = new Page<>(pageReqVO.getPageNo(), pageReqVO.getPageSize());
        
        // 执行分页查询
        IPage<ErpAssetCategoryDO> result = assetCategoryMapper.selectPageWithIPage(page, pageReqVO);
        
        // 转换结果
        IPage<ErpAssetCategoryRespVO> respPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<ErpAssetCategoryRespVO> respList = result.getRecords().stream()
                .map(record -> BeanUtils.toBean(record, ErpAssetCategoryRespVO.class))
                .collect(Collectors.toList());
        respPage.setRecords(respList);
        
        return respPage;
    }

    @Override
    public List<ErpAssetCategoryRespVO> getAssetCategoryVOList() {
        List<ErpAssetCategoryDO> list = assetCategoryMapper.selectList();
        return BeanUtils.toBean(list, ErpAssetCategoryRespVO.class);
    }

} 