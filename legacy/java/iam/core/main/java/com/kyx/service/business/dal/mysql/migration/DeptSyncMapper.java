package com.kyx.service.business.dal.mysql.migration;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.business.dal.dataobject.migration.DeptSyncDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 部门同步迁移 Mapper
 * 
 * @author MK
 */
@Mapper
public interface DeptSyncMapper extends BaseMapperX<DeptSyncDO> {

    /**
     * 根据外部部门ID查询
     */
    default DeptSyncDO selectByExternalDeptId(String externalDeptId) {
        return selectOne(DeptSyncDO::getExternalDeptId, externalDeptId);
    }

    /**
     * 根据部门名称查询
     */
    default DeptSyncDO selectByDeptName(String deptName) {
        return selectOne(DeptSyncDO::getDeptName, deptName);
    }

    /**
     * 根据同步状态查询列表
     */
    default List<DeptSyncDO> selectListBySyncStatus(Integer syncStatus) {
        return selectList(DeptSyncDO::getSyncStatus, syncStatus);
    }

    /**
     * 查询待同步的部门列表
     */
    default List<DeptSyncDO> selectPendingSyncList() {
        return selectList(new LambdaQueryWrapperX<DeptSyncDO>()
                .eq(DeptSyncDO::getSyncStatus, DeptSyncDO.SyncStatus.PENDING.getCode())
                .orderByAsc(DeptSyncDO::getOrderNum)
                .orderByAsc(DeptSyncDO::getCreateTime));
    }

    /**
     * 根据父部门ID查询子部门列表
     */
    default List<DeptSyncDO> selectListByParentId(Long parentId) {
        return selectList(DeptSyncDO::getParentId, parentId);
    }

    /**
     * 根据外部父部门ID查询子部门列表
     */
    default List<DeptSyncDO> selectListByExternalParentId(String externalParentId) {
        return selectList(DeptSyncDO::getExternalParentId, externalParentId);
    }

    /**
     * 查询所有根部门（parentId为0或null）
     */
    default List<DeptSyncDO> selectRootDeptList() {
        return selectList(new LambdaQueryWrapperX<DeptSyncDO>()
                .and(wrapper -> wrapper.eq(DeptSyncDO::getParentId, 0L)
                        .or()
                        .isNull(DeptSyncDO::getParentId))
                .orderByAsc(DeptSyncDO::getOrderNum));
    }
}