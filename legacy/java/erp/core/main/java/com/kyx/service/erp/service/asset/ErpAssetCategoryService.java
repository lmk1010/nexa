package com.kyx.service.erp.service.asset;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.erp.controller.admin.asset.vo.category.ErpAssetCategoryPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.category.ErpAssetCategoryRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.category.ErpAssetCategorySaveReqVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetCategoryDO;
import javax.validation.Valid;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.kyx.foundation.common.util.collection.CollectionUtils.convertMap;

/**
 * ERP 资产分类 Service 接口
 *
 * @author kyx
 */
public interface ErpAssetCategoryService {

    /**
     * 创建资产分类
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createAssetCategory(@Valid ErpAssetCategorySaveReqVO createReqVO);

    /**
     * 更新资产分类
     *
     * @param updateReqVO 更新信息
     */
    void updateAssetCategory(@Valid ErpAssetCategorySaveReqVO updateReqVO);

    /**
     * 删除资产分类
     *
     * @param id 编号
     */
    void deleteAssetCategory(Long id);

    /**
     * 获得资产分类
     *
     * @param id 编号
     * @return 资产分类
     */
    ErpAssetCategoryDO getAssetCategory(Long id);

    /**
     * 校验资产分类
     *
     * @param id 编号
     * @return 资产分类
     */
    ErpAssetCategoryDO validateAssetCategory(Long id);

    /**
     * 获得指定状态的资产分类列表
     *
     * @param status 状态
     * @return 资产分类列表
     */
    List<ErpAssetCategoryDO> getAssetCategoryListByStatus(Integer status);

    /**
     * 获得资产分类列表
     *
     * @param ids 编号数组
     * @return 资产分类列表
     */
    List<ErpAssetCategoryDO> getAssetCategoryList(Collection<Long> ids);

    /**
     * 获得资产分类 Map
     *
     * @param ids 编号数组
     * @return 资产分类 Map
     */
    default Map<Long, ErpAssetCategoryDO> getAssetCategoryMap(Collection<Long> ids) {
        return convertMap(getAssetCategoryList(ids), ErpAssetCategoryDO::getId);
    }

    /**
     * 获得资产分类 VO 分页
     *
     * @param pageReqVO 分页查询
     * @return 资产分类分页
     */
    PageResult<ErpAssetCategoryRespVO> getAssetCategoryVOPage(ErpAssetCategoryPageReqVO pageReqVO);

    /**
     * 获得资产分类 IPage 分页（真分页）
     *
     * @param pageReqVO 分页查询
     * @return 资产分类IPage分页
     */
    IPage<ErpAssetCategoryRespVO> getAssetCategoryVOPageWithIPage(ErpAssetCategoryPageReqVO pageReqVO);

    /**
     * 获得资产分类 VO 列表
     *
     * @return 资产分类 VO 列表
     */
    List<ErpAssetCategoryRespVO> getAssetCategoryVOList();

} 