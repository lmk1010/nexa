package com.kyx.service.hr.dal.mysql.employee;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.foundation.mybatis.core.query.QueryWrapperX;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeEntryPageReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeEntryRespVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeEntryDO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 员工入职记录 Mapper
 *
 * @author MK
 */
@Mapper
public interface EmployeeEntryMapper extends BaseMapperX<EmployeeEntryDO> {

    @Select("SELECT COALESCE(dept_id, 0) AS deptId, COUNT(1) AS headcount " +
            "FROM hr_employee_entry " +
            "WHERE deleted = 0 " +
            "AND work_status IN (#{probationStatus}, #{activeStatus}) " +
            "GROUP BY COALESCE(dept_id, 0)")
    List<DeptHeadcount> selectDeptHeadcounts(@Param("probationStatus") Integer probationStatus,
                                             @Param("activeStatus") Integer activeStatus);

    default EmployeeEntryDO selectByEntryNo(String entryNo) {
        return selectFirstOne(EmployeeEntryDO::getEntryNo, entryNo);
    }

    default EmployeeEntryDO selectByEmployeeNo(String employeeNo) {
        return selectFirstOne(EmployeeEntryDO::getEmployeeNo, employeeNo);
    }

    default EmployeeEntryDO selectByUserId(Long userId) {
        return selectFirstOne(EmployeeEntryDO::getUserId, userId);
    }

    default List<EmployeeEntryDO> selectListByUserId(Long userId) {
        return selectList(EmployeeEntryDO::getUserId, userId);
    }

    default List<EmployeeEntryDO> selectListByProfileId(Long profileId) {
        return selectList(EmployeeEntryDO::getProfileId, profileId);
    }

    default List<EmployeeEntryDO> selectListByWorkStatus(Integer workStatus) {
        return selectList(EmployeeEntryDO::getWorkStatus, workStatus);
    }

    default List<EmployeeEntryDO> selectListByOnboardingStatus(Integer onboardingStatus) {
        return selectList(EmployeeEntryDO::getOnboardingStatus, onboardingStatus);
    }

    default List<EmployeeEntryDO> selectListByDeptId(Long deptId) {
        return selectList(EmployeeEntryDO::getDeptId, deptId);
    }

    default List<EmployeeEntryDO> selectListByEntryDateRange(LocalDate startDate, LocalDate endDate) {
        return selectList(new LambdaQueryWrapperX<EmployeeEntryDO>()
                .betweenIfPresent(EmployeeEntryDO::getEntryDate, startDate, endDate));
    }

    default List<EmployeeEntryDO> selectListByEntryType(Integer entryType) {
        return selectList(EmployeeEntryDO::getEntryType, entryType);
    }

    /**
     * 查询当天最大入职编号
     *
     * @param dateStr 日期字符串，格式：yyyyMMdd
     * @return 最大入职编号
     */
    default String selectMaxEntryNoByDate(String dateStr) {
        List<EmployeeEntryDO> results = selectList(new LambdaQueryWrapperX<EmployeeEntryDO>()
                .likeRight(EmployeeEntryDO::getEntryNo, "ENTRY" + dateStr)
                .orderByDesc(EmployeeEntryDO::getEntryNo)
                .last("LIMIT 1"));
        return results.isEmpty() ? null : results.get(0).getEntryNo();
    }

    /**
     * 查询当天最大员工编号
     *
     * @param dateStr 日期字符串，格式：yyyyMMdd
     * @return 最大员工编号
     */
    default String selectMaxEmployeeNoByDate(String dateStr) {
        List<EmployeeEntryDO> results = selectList(new LambdaQueryWrapperX<EmployeeEntryDO>()
                .likeRight(EmployeeEntryDO::getEmployeeNo, "EMP" + dateStr)
                .orderByDesc(EmployeeEntryDO::getEmployeeNo)
                .last("LIMIT 1"));
        return results.isEmpty() ? null : results.get(0).getEmployeeNo();
    }

    /**
     * 分页查询员工入职记录（关联员工档案信息）
     *
     * @param page 分页参数
     * @param pageReqVO 查询条件
     * @return 分页结果
     */
    IPage<EmployeeEntryRespVO> selectPageWithProfile(IPage<EmployeeEntryRespVO> page, @Param("ew") EmployeeEntryPageReqVO pageReqVO);

    /**
     * 根据ID查询员工入职记录（关联员工档案信息）
     *
     * @param id 入职记录ID
     * @return 员工入职记录
     */
    EmployeeEntryRespVO selectByIdWithProfile(@Param("id") Long id);

    /**
     * 根据入职编号查询员工入职记录（关联员工档案信息）
     *
     * @param entryNo 入职编号
     * @return 员工入职记录详情
     */
    EmployeeEntryRespVO selectByEntryNoWithProfile(@Param("entryNo") String entryNo);

    /**
     * 根据员工编号查询员工入职记录（关联员工档案信息）
     *
     * @param employeeNo 员工编号
     * @return 员工入职记录详情
     */
    EmployeeEntryRespVO selectByEmployeeNoWithProfile(@Param("employeeNo") String employeeNo);

    /**
     * 根据ID查询员工入职记录详情（包含教育信息、家庭信息等）
     *
     * @param id 入职记录ID
     * @return 员工入职记录详情
     */
    EmployeeEntryRespVO selectDetailById(@Param("id") Long id);

    @Data
    class DeptHeadcount {

        private Long deptId;

        private Long headcount;
    }
}
