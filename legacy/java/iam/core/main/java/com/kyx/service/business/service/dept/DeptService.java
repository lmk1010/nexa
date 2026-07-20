package com.kyx.service.business.service.dept;

import com.kyx.foundation.common.util.collection.CollectionUtils;
import com.kyx.service.business.controller.admin.dept.vo.dept.DeptListReqVO;
import com.kyx.service.business.controller.admin.dept.vo.dept.DeptSaveReqVO;
import com.kyx.service.business.dal.dataobject.dept.DeptDO;
import lombok.Data;

import java.util.*;

/**
 * 部门 Service 接口
 *
 * @author MK
 */
public interface DeptService {

    /**
     * 创建部门
     *
     * @param createReqVO 部门信息
     * @return 部门编号
     */
    Long createDept(DeptSaveReqVO createReqVO);

    /**
     * 按指定部门编号创建或更新部门，用于外部组织架构同步。
     *
     * @param id 部门编号
     * @param name 部门名称
     * @param parentId 父部门编号
     * @param sort 排序
     * @param status 状态
     * @return 同步结果
     */
    default UpsertDeptResult upsertDept(Long id, String name, Long parentId, Integer sort, Integer status) {
        return upsertDept(id, name, parentId, sort, status, null);
    }

    /**
     * 按指定部门编号创建或更新部门，用于外部组织架构同步。
     *
     * @param id 部门编号
     * @param name 部门名称
     * @param parentId 父部门编号
     * @param sort 排序
     * @param status 状态
     * @param leaderUserId 部门主管用户编号；null 表示不更新主管
     * @return 同步结果
     */
    UpsertDeptResult upsertDept(Long id, String name, Long parentId, Integer sort, Integer status, Long leaderUserId);

    /**
     * 更新部门
     *
     * @param updateReqVO 部门信息
     */
    void updateDept(DeptSaveReqVO updateReqVO);

    /**
     * 删除部门
     *
     * @param id 部门编号
     */
    void deleteDept(Long id);

    /**
     * 删除部门（带员工转移）
     *
     * @param id 部门编号
     * @param transferDeptId 员工转移到的部门编号，如果为null则拒绝删除
     */
    void deleteDeptWithUserTransfer(Long id, Long transferDeptId);

    /**
     * 获得部门信息
     *
     * @param id 部门编号
     * @return 部门信息
     */
    DeptDO getDept(Long id);

    /**
     * 获得部门信息数组
     *
     * @param ids 部门编号数组
     * @return 部门信息数组
     */
    List<DeptDO> getDeptList(Collection<Long> ids);

    /**
     * 筛选部门列表
     *
     * @param reqVO 筛选条件请求 VO
     * @return 部门列表
     */
    List<DeptDO> getDeptList(DeptListReqVO reqVO);

    /**
     * 根据租户ID列表获取部门列表（支持跨租户查询）
     *
     * @param tenantIds 租户ID列表，逗号分隔。为空则查询当前租户
     * @return 部门列表
     */
    List<DeptDO> getDeptListByTenants(String tenantIds);

    /**
     * 获得指定编号的部门 Map
     *
     * @param ids 部门编号数组
     * @return 部门 Map
     */
    default Map<Long, DeptDO> getDeptMap(Collection<Long> ids) {
        List<DeptDO> list = getDeptList(ids);
        return CollectionUtils.convertMap(list, DeptDO::getId);
    }

    /**
     * 获得指定部门的所有子部门
     *
     * @param id 部门编号
     * @return 子部门列表
     */
    default List<DeptDO> getChildDeptList(Long id) {
        return getChildDeptList(Collections.singleton(id));
    }

    /**
     * 获得指定部门的所有子部门
     *
     * @param ids 部门编号数组
     * @return 子部门列表
     */
    List<DeptDO> getChildDeptList(Collection<Long> ids);

    /**
     * 获得指定领导者的部门列表
     *
     * @param id 领导者编号
     * @return 部门列表
     */
    List<DeptDO> getDeptListByLeaderUserId(Long id);

    /**
     * 根据部门名称获取部门信息
     *
     * @param name 部门名称
     * @return 部门信息，如果不存在则返回null
     */
    DeptDO getDeptByName(String name);

    /**
     * 根据部门名称获取部门列表
     *
     * @param name 部门名称
     * @return 部门列表
     */
    List<DeptDO> getDeptListByName(String name);

    /**
     * 根据外部部门ID获取内部部门ID
     *
     * @param externalDeptId 外部部门ID
     * @return 内部部门ID，如果不存在则返回null
     */
    Long getInternalDeptIdByExternalId(String externalDeptId);

    /**
     * 获得所有子部门，从缓存中
     *
     * @param id 父部门编号
     * @return 子部门列表
     */
    Set<Long> getChildDeptIdListFromCache(Long id);

    /**
     * 校验部门们是否有效。如下情况，视为无效：
     * 1. 部门编号不存在
     * 2. 部门被禁用
     *
     * @param ids 角色编号数组
     */
    void validateDeptList(Collection<Long> ids);

    /**
     * 统计部门员工数量
     *
     * @param deptIds 部门编号集合
     * @return 部门员工数量映射
     */
    Map<Long, Integer> getUserCountByDeptIds(Collection<Long> deptIds);

    /**
     * 获取部门（含子部门）员工数量（基于 HR 花名册）
     *
     * @param deptIds 部门编号集合
     * @return 部门ID -> 员工数量
     */
    Map<Long, Integer> getEmployeeCountByDeptIds(Collection<Long> deptIds);

    @Data
    class UpsertDeptResult {
        private Long id;
        private boolean created;
        private boolean updated;
    }

}
