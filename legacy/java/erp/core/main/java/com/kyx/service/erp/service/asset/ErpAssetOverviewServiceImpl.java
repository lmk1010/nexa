package com.kyx.service.erp.service.asset;

import cn.hutool.core.collection.CollUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.erp.controller.admin.asset.vo.overview.*;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetCategoryDO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetOwnershipDO;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetMapper;
import com.kyx.service.erp.dal.mysql.asset.ErpAssetOwnershipMapper;
import com.kyx.service.business.api.dept.DeptApi;
import com.kyx.service.business.api.dept.dto.DeptRespDTO;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.kyx.foundation.common.util.collection.CollectionUtils.convertMap;
import static com.kyx.foundation.common.util.collection.CollectionUtils.convertSet;

/**
 * ERP 资产纵览 Service 实现类
 *
 * @author KYX
 */
@Service
@Validated
@Slf4j
public class ErpAssetOverviewServiceImpl implements ErpAssetOverviewService {

    @Resource
    private ErpAssetMapper assetMapper;
    @Resource
    private ErpAssetOwnershipMapper assetOwnershipMapper;
    @Resource
    private ErpAssetCategoryService assetCategoryService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    @Override
    public ErpAssetStatisticsRespVO getAssetStatistics(ErpAssetStatisticsReqVO reqVO) {
        ErpAssetStatisticsRespVO resp = new ErpAssetStatisticsRespVO();
        
        // 构建查询条件
        Map<String, Object> params = buildQueryParams(reqVO.getDeptId(), reqVO.getUserId(), reqVO.getCategoryId());
        
        // 获取总数
        resp.setTotalAssets(assetMapper.selectCountByConditions(params));
        
        // 按状态统计
        params.put("status", 0); // 正常
        resp.setNormalAssets(assetMapper.selectCountByConditions(params));
        
        params.put("status", 1); // 借用中
        resp.setBorrowedAssets(assetMapper.selectCountByConditions(params));
        
        params.put("status", 2); // 已报废
        resp.setScrappedAssets(assetMapper.selectCountByConditions(params));
        
        params.put("status", 3); // 维修中
        resp.setRepairingAssets(assetMapper.selectCountByConditions(params));
        
        // 计算快过期资产数量（90天内过期）
        params.remove("status");
        LocalDate now = LocalDate.now();
        LocalDate expiringThreshold = now.plusDays(90);
        params.put("warrantyDateStart", now);
        params.put("warrantyDateEnd", expiringThreshold);
        resp.setExpiringAssets(assetMapper.selectCountByConditions(params));
        
        return resp;
    }

    @Override
    public List<ErpAssetCategoryStatisticsRespVO> getCategoryStatistics(ErpAssetCategoryStatisticsReqVO reqVO) {
        Map<String, Object> params = buildQueryParams(reqVO.getDeptId(), reqVO.getUserId(), null);
        
        // 获取按分类统计的数据
        List<Map<String, Object>> categoryStats = assetMapper.selectCategoryStatistics(params);
        
        // 计算总数用于百分比计算
        Long totalCount = assetMapper.selectCountByConditions(params);
        
        return categoryStats.stream().map(stat -> {
            ErpAssetCategoryStatisticsRespVO resp = new ErpAssetCategoryStatisticsRespVO();
            resp.setCategoryId((Long) stat.get("categoryId"));
            resp.setCategoryName((String) stat.get("categoryName"));
            resp.setCount((Long) stat.get("count"));
            
            // 计算百分比
            if (totalCount > 0) {
                BigDecimal percentage = BigDecimal.valueOf(resp.getCount() * 100.0 / totalCount)
                    .setScale(2, RoundingMode.HALF_UP);
                resp.setPercentage(percentage);
            } else {
                resp.setPercentage(BigDecimal.ZERO);
            }
            
            return resp;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ErpAssetDeptStatisticsRespVO> getDeptStatistics(ErpAssetDeptStatisticsReqVO reqVO) {
        Map<String, Object> params = buildQueryParams(null, null, reqVO.getCategoryId());
        
        // 获取按部门统计的数据
        List<Map<String, Object>> deptStats = assetMapper.selectDeptStatistics(params);
        
        // 计算总数用于百分比计算
        Long totalCount = assetMapper.selectCountByConditions(params);
        
        // 获取部门信息
        Set<Long> deptIds = deptStats.stream()
            .map(stat -> (Long) stat.get("deptId"))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        
        final Map<Long, DeptRespDTO> deptMap;
        if (CollUtil.isNotEmpty(deptIds)) {
            List<DeptRespDTO> depts = deptApi.getDeptList(deptIds).getCheckedData();
            deptMap = convertMap(depts, DeptRespDTO::getId);
        } else {
            deptMap = new HashMap<>();
        }
        
        return deptStats.stream().map(stat -> {
            ErpAssetDeptStatisticsRespVO resp = new ErpAssetDeptStatisticsRespVO();
            Long deptId = (Long) stat.get("deptId");
            resp.setDeptId(deptId);
            resp.setCount((Long) stat.get("count"));
            
            // 设置部门名称
            if (deptId != null && deptMap.containsKey(deptId)) {
                resp.setDeptName(deptMap.get(deptId).getName());
            } else {
                resp.setDeptName("未分配");
            }
            
            // 计算百分比
            if (totalCount > 0) {
                BigDecimal percentage = BigDecimal.valueOf(resp.getCount() * 100.0 / totalCount)
                    .setScale(2, RoundingMode.HALF_UP);
                resp.setPercentage(percentage);
            } else {
                resp.setPercentage(BigDecimal.ZERO);
            }
            
            return resp;
        }).sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
          .collect(Collectors.toList());
    }

    @Override
    public List<ErpAssetUserStatisticsRespVO> getUserStatistics(ErpAssetUserStatisticsReqVO reqVO) {
        Map<String, Object> params = buildQueryParams(reqVO.getDeptId(), null, reqVO.getCategoryId());
        
        // 获取按用户统计的数据
        List<Map<String, Object>> userStats = assetMapper.selectUserStatistics(params);
        
        // 计算总数用于百分比计算
        Long totalCount = assetMapper.selectCountByConditions(params);
        
        // 获取用户信息
        Set<Long> userIds = userStats.stream()
            .map(stat -> (Long) stat.get("userId"))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        
        final Map<Long, AdminUserRespDTO> userMap;
        if (CollUtil.isNotEmpty(userIds)) {
            List<AdminUserRespDTO> users = adminUserApi.getUserList(userIds).getCheckedData();
            userMap = convertMap(users, AdminUserRespDTO::getId);
        } else {
            userMap = new HashMap<>();
        }
        
        return userStats.stream().map(stat -> {
            ErpAssetUserStatisticsRespVO resp = new ErpAssetUserStatisticsRespVO();
            Long userId = (Long) stat.get("userId");
            resp.setUserId(userId);
            resp.setCount((Long) stat.get("count"));
            
            // 设置用户姓名
            if (userId != null && userMap.containsKey(userId)) {
                resp.setUserName(userMap.get(userId).getNickname());
            } else {
                resp.setUserName("未分配");
            }
            
            // 计算百分比
            if (totalCount > 0) {
                BigDecimal percentage = BigDecimal.valueOf(resp.getCount() * 100.0 / totalCount)
                    .setScale(2, RoundingMode.HALF_UP);
                resp.setPercentage(percentage);
            } else {
                resp.setPercentage(BigDecimal.ZERO);
            }
            
            return resp;
        }).sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
          .limit(reqVO.getLimit() != null ? reqVO.getLimit() : 20)
          .collect(Collectors.toList());
    }

    @Override
    public PageResult<ErpAssetExpiringRespVO> getExpiringAssetsPage(ErpAssetExpiringPageReqVO pageReqVO) {
        // 构建查询条件
        Map<String, Object> params = buildQueryParams(pageReqVO.getDeptId(), null, pageReqVO.getCategoryId());
        
        // 设置过期时间范围
        LocalDate now = LocalDate.now();
        LocalDate expiringThreshold = now.plusDays(pageReqVO.getDays());
        params.put("warrantyDateEnd", expiringThreshold);
        
        // 分页查询快过期资产
        PageResult<ErpAssetDO> assetPage = assetMapper.selectExpiringAssetsPage(pageReqVO, params);
        
        // 转换为响应VO
        List<ErpAssetExpiringRespVO> respVOList = assetPage.getList().stream().map(asset -> {
            ErpAssetExpiringRespVO resp = BeanUtils.toBean(asset, ErpAssetExpiringRespVO.class);
            
            // 计算剩余天数
            if (asset.getWarrantyDate() != null) {
                LocalDate currentDate = LocalDate.now();
                long remainingDays = java.time.temporal.ChronoUnit.DAYS.between(currentDate, asset.getWarrantyDate());
                resp.setRemainingDays(remainingDays);
            }
            
            return resp;
        }).collect(Collectors.toList());

        // 查询并填充用户信息（从资产所有权表）
        fillUserInfoForAssets(respVOList);
        
        // 填充关联信息
        fillAssetExpiringRespVO(respVOList);
        
        return new PageResult<>(respVOList, assetPage.getTotal());
    }

    @Override
    public List<ErpAssetStatusStatisticsRespVO> getStatusDistribution(ErpAssetStatusStatisticsReqVO reqVO) {
        Map<String, Object> params = buildQueryParams(reqVO.getDeptId(), reqVO.getUserId(), reqVO.getCategoryId());
        
        // 获取按状态统计的数据
        List<Map<String, Object>> statusStats = assetMapper.selectStatusStatistics(params);
        
        // 计算总数用于百分比计算
        Long totalCount = assetMapper.selectCountByConditions(params);
        
        // 状态名称映射
        Map<Integer, String> statusNameMap = new HashMap<>();
        statusNameMap.put(0, "正常");
        statusNameMap.put(1, "借用中");
        statusNameMap.put(2, "已报废");
        statusNameMap.put(3, "维修中");
        
        return statusStats.stream().map(stat -> {
            ErpAssetStatusStatisticsRespVO resp = new ErpAssetStatusStatisticsRespVO();
            Integer status = (Integer) stat.get("status");
            resp.setStatus(status);
            resp.setStatusName(statusNameMap.getOrDefault(status, "未知"));
            resp.setCount((Long) stat.get("count"));
            
            // 计算百分比
            if (totalCount > 0) {
                BigDecimal percentage = BigDecimal.valueOf(resp.getCount() * 100.0 / totalCount)
                    .setScale(2, RoundingMode.HALF_UP);
                resp.setPercentage(percentage);
            } else {
                resp.setPercentage(BigDecimal.ZERO);
            }
            
            return resp;
        }).collect(Collectors.toList());
    }

    /**
     * 构建查询参数
     */
    private Map<String, Object> buildQueryParams(Long deptId, Long userId, Long categoryId) {
        Map<String, Object> params = new HashMap<>();
        if (deptId != null) {
            params.put("deptId", deptId);
        }
        if (userId != null) {
            params.put("userId", userId);
        }
        if (categoryId != null) {
            params.put("categoryId", categoryId);
        }
        return params;
    }

    /**
     * 填充快过期资产响应VO的关联信息
     */
    private void fillAssetExpiringRespVO(List<ErpAssetExpiringRespVO> respVOList) {
        if (CollUtil.isEmpty(respVOList)) {
            return;
        }
        
        // 填充分类信息
        Set<Long> categoryIds = convertSet(respVOList, ErpAssetExpiringRespVO::getCategoryId);
        if (CollUtil.isNotEmpty(categoryIds)) {
            List<ErpAssetCategoryDO> categories = assetCategoryService.getAssetCategoryList(categoryIds);
            Map<Long, ErpAssetCategoryDO> categoryMap = convertMap(categories, ErpAssetCategoryDO::getId);
            respVOList.forEach(resp -> {
                if (resp.getCategoryId() != null && categoryMap.containsKey(resp.getCategoryId())) {
                    resp.setCategoryName(categoryMap.get(resp.getCategoryId()).getName());
                }
            });
        }
        
        // 填充部门信息
        Set<Long> deptIds = convertSet(respVOList, ErpAssetExpiringRespVO::getDeptId);
        if (CollUtil.isNotEmpty(deptIds)) {
            List<DeptRespDTO> depts = deptApi.getDeptList(deptIds).getCheckedData();
            Map<Long, DeptRespDTO> deptMap = convertMap(depts, DeptRespDTO::getId);
            respVOList.forEach(resp -> {
                if (resp.getDeptId() != null && deptMap.containsKey(resp.getDeptId())) {
                    resp.setDeptName(deptMap.get(resp.getDeptId()).getName());
                }
            });
        }
        
        // 填充用户信息
        Set<Long> userIds = convertSet(respVOList, ErpAssetExpiringRespVO::getUserId);
        if (CollUtil.isNotEmpty(userIds)) {
            List<AdminUserRespDTO> users = adminUserApi.getUserList(userIds).getCheckedData();
            Map<Long, AdminUserRespDTO> userMap = convertMap(users, AdminUserRespDTO::getId);
            respVOList.forEach(resp -> {
                if (resp.getUserId() != null && userMap.containsKey(resp.getUserId())) {
                    resp.setUserName(userMap.get(resp.getUserId()).getNickname());
                }
            });
        }
    }

    /**
     * 填充资产的用户信息（从资产所有权表）
     */
    private void fillUserInfoForAssets(List<ErpAssetExpiringRespVO> respVOList) {
        if (CollUtil.isEmpty(respVOList)) {
            return;
        }

        // 为每个资产查询当前所有权信息并填充用户ID
        respVOList.forEach(resp -> {
            ErpAssetOwnershipDO ownership = assetOwnershipMapper.selectCurrentOwnership(resp.getId());
            if (ownership != null && ownership.getCurrentUserId() != null) {
                resp.setUserId(ownership.getCurrentUserId());
            }
        });
    }
}