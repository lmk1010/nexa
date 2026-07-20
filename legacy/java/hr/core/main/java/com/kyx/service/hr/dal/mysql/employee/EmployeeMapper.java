package com.kyx.service.hr.dal.mysql.employee;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeOverviewRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePageReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeStatisticsRespVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeDO;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 员工花名册 Mapper
 *
 * @author MK
 */
@Mapper
public interface EmployeeMapper extends BaseMapperX<EmployeeDO> {

    /**
     * 分页查询员工花名册
     *
     * @param page 分页参数
     * @param pageReqVO 分页查询条件
     * @param tenantId 租户ID
     * @return 员工花名册分页结果
     */
    IPage<EmployeeRespVO> selectPage(IPage<EmployeeRespVO> page, @Param("ew") EmployeePageReqVO pageReqVO, @Param("tenantId") Long tenantId);

    /**
     * 根据ID查询员工详情
     *
     * @param id 员工ID
     * @param tenantId 租户ID
     * @return 员工详情
     */
    EmployeeRespVO selectEmployeeById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 获取员工统计数据
     *
     * @param tenantId 租户ID
     * @return 员工统计数据
     */
    EmployeeStatisticsRespVO selectEmployeeStatistics(@Param("tenantId") Long tenantId);

    /**
     * 获取某个参考日期对应的员工统计数据
     *
     * @param tenantId 租户ID
     * @param referenceDate 参考日期（月末）
     * @return 员工统计数据
     */
    EmployeeStatisticsRespVO selectEmployeeStatisticsByDate(@Param("tenantId") Long tenantId,
                                                            @Param("referenceDate") LocalDate referenceDate);

    /**
     * 获取某个参考日期对应的年龄司龄数据
     *
     * @param tenantId 租户ID
     * @param referenceDate 参考日期（月末）
     * @return 年龄司龄
     */
    EmployeeOverviewRespVO.Age selectEmployeeOverviewAgeByDate(@Param("tenantId") Long tenantId,
                                                               @Param("referenceDate") LocalDate referenceDate);

    /**
     * 获取员工总览人员规模
     *
     * @param tenantId 租户ID
     * @return 人员规模
     */
    EmployeeOverviewRespVO.Scale selectEmployeeOverviewScale(@Param("tenantId") Long tenantId);

    /**
     * 获取员工总览年龄司龄
     *
     * @param tenantId 租户ID
     * @return 年龄司龄
     */
    EmployeeOverviewRespVO.Age selectEmployeeOverviewAge(@Param("tenantId") Long tenantId);

    /**
     * 获取员工总览学历统计
     *
     * @param tenantId 租户ID
     * @return 学历统计
     */
    EmployeeOverviewRespVO.Education selectEmployeeOverviewEducation(@Param("tenantId") Long tenantId);

    /**
     * 获取员工总览岗位分布
     *
     * @param tenantId 租户ID
     * @return 岗位分布
     */
    List<EmployeeOverviewRespVO.PostStat> selectEmployeeOverviewPostStats(@Param("tenantId") Long tenantId);
} 
