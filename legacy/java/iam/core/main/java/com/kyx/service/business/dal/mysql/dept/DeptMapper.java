package com.kyx.service.business.dal.mysql.dept;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.business.controller.admin.dept.vo.dept.DeptListReqVO;
import com.kyx.service.business.dal.dataobject.dept.DeptDO;
import com.kyx.service.business.dal.dataobject.dept.DeptEmployeeCountDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface DeptMapper extends BaseMapperX<DeptDO> {

    /**
     * 统计各部门在职人数（通讯录侧栏 / 部门管理）。
     * 口径对齐成员管理列表：以 HR 档案为主，部门取
     * COALESCE(system_users.dept_id, entry.dept_id)，排除 work_status=4 离职。
     * 这样无系统账号但仍有档案的人员也会计入，避免侧栏与列表人数对不上。
     */
    @Select({
            "<script>",
            "SELECT t.dept_id AS deptId, COUNT(DISTINCT t.profile_id) AS userCount",
            "FROM (",
            "  SELECT p.id AS profile_id,",
            "         COALESCE(u.dept_id, e.dept_id) AS dept_id",
            "  FROM hr_employee_profile p",
            "  LEFT JOIN hr_employee_entry e",
            "    ON e.profile_id = p.id",
            "   AND e.deleted = b'0'",
            "  LEFT JOIN system_users u",
            "    ON u.id = COALESCE(e.user_id, p.user_id)",
            "   AND u.deleted = b'0'",
            "  WHERE p.deleted = b'0'",
            "    AND (e.id IS NULL OR e.work_status IS NULL OR e.work_status &lt;&gt; 4)",
            ") t",
            "WHERE t.dept_id IS NOT NULL",
            "<if test='deptIds != null and deptIds.size > 0'>",
            "AND t.dept_id IN",
            "<foreach collection='deptIds' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</if>",
            "GROUP BY t.dept_id",
            "</script>"
    })
    List<DeptEmployeeCountDO> selectEmployeeCountByDeptIds(@Param("deptIds") Collection<Long> deptIds);

    /**
     * 统计仍挂在指定部门上的 HR 入职记录数（用于删除前校验，覆盖无系统账号档案）。
     */
    @Select({
            "<script>",
            "SELECT COUNT(1)",
            "FROM hr_employee_entry e",
            "WHERE e.deleted = b'0'",
            "AND e.dept_id IN",
            "<foreach collection='deptIds' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    Long selectEntryCountByDeptIds(@Param("deptIds") Collection<Long> deptIds);

    /**
     * 删除部门时，把仍挂在这些部门上的 HR 入职记录迁移到目标部门。
     */
    @org.apache.ibatis.annotations.Update({
            "<script>",
            "UPDATE hr_employee_entry",
            "SET dept_id = #{transferDeptId}",
            "WHERE deleted = b'0'",
            "AND dept_id IN",
            "<foreach collection='deptIds' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    int updateEntryDeptByDeptIds(@Param("deptIds") Collection<Long> deptIds,
                                 @Param("transferDeptId") Long transferDeptId);

    default List<DeptDO> selectList(DeptListReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<DeptDO>()
                .likeIfPresent(DeptDO::getName, reqVO.getName())
                .eqIfPresent(DeptDO::getStatus, reqVO.getStatus()));
    }

    default DeptDO selectByParentIdAndName(Long parentId, String name) {
        return selectOne(DeptDO::getParentId, parentId, DeptDO::getName, name);
    }

    default Long selectCountByParentId(Long parentId) {
        return selectCount(DeptDO::getParentId, parentId);
    }

    default List<DeptDO> selectListByParentId(Collection<Long> parentIds) {
        return selectList(DeptDO::getParentId, parentIds);
    }

    default List<DeptDO> selectListByLeaderUserId(Long id) {
        return selectList(DeptDO::getLeaderUserId, id);
    }

    default DeptDO selectByName(String name) {
        return selectOne(DeptDO::getName, name);
    }

    default List<DeptDO> selectListByName(String name) {
        return selectList(DeptDO::getName, name);
    }

    default List<DeptDO> selectListByTenantIds(Collection<Long> tenantIds, Integer status) {
        return selectList(new LambdaQueryWrapperX<DeptDO>()
                .inIfPresent(DeptDO::getTenantId, tenantIds)
                .eqIfPresent(DeptDO::getStatus, status)
                .orderByAsc(DeptDO::getSort));
    }

}
