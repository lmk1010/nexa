package com.kyx.service.hr.dal.mysql.employee;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePerformancePageReqVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeePerformanceDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface EmployeePerformanceMapper extends BaseMapperX<EmployeePerformanceDO> {

    default List<EmployeePerformanceDO> selectListByProfileId(Long profileId) {
        return selectList(EmployeePerformanceDO::getProfileId, profileId);
    }

    default PageResult<EmployeePerformanceDO> selectPage(EmployeePerformancePageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO).orderByDesc(EmployeePerformanceDO::getEvaluatedDate)
                .orderByDesc(EmployeePerformanceDO::getId));
    }

    default List<EmployeePerformanceDO> selectListByReq(EmployeePerformancePageReqVO reqVO, Integer limit) {
        LambdaQueryWrapperX<EmployeePerformanceDO> wrapper = buildQuery(reqVO)
                .orderByDesc(EmployeePerformanceDO::getEvaluatedDate)
                .orderByDesc(EmployeePerformanceDO::getId);
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        return selectList(wrapper);
    }

    default LambdaQueryWrapperX<EmployeePerformanceDO> buildQuery(EmployeePerformancePageReqVO reqVO) {
        return new LambdaQueryWrapperX<EmployeePerformanceDO>()
                .eqIfPresent(EmployeePerformanceDO::getProfileId, reqVO.getProfileId())
                .eqIfPresent(EmployeePerformanceDO::getSchemeId, reqVO.getSchemeId())
                .inIfPresent(EmployeePerformanceDO::getProfileId, reqVO.getProfileIds())
                .likeIfPresent(EmployeePerformanceDO::getPeriod, reqVO.getPeriod())
                .likeIfPresent(EmployeePerformanceDO::getGrade, reqVO.getGrade())
                .likeIfPresent(EmployeePerformanceDO::getResult, reqVO.getResult())
                .eqIfPresent(EmployeePerformanceDO::getCycleStatus, reqVO.getCycleStatus())
                .eqIfPresent(EmployeePerformanceDO::getApplicationType, reqVO.getApplicationType())
                .eqIfPresent(EmployeePerformanceDO::getApplicationStatus, reqVO.getApplicationStatus())
                .eqIfPresent(EmployeePerformanceDO::getApprovalStatus, reqVO.getApprovalStatus())
                .geIfPresent(EmployeePerformanceDO::getScore, reqVO.getScoreMin())
                .leIfPresent(EmployeePerformanceDO::getScore, reqVO.getScoreMax())
                .geIfPresent(EmployeePerformanceDO::getEvaluatedDate, reqVO.getEvaluatedDateStart())
                .leIfPresent(EmployeePerformanceDO::getEvaluatedDate, reqVO.getEvaluatedDateEnd())
                .geIfPresent(EmployeePerformanceDO::getInterviewTime, reqVO.getInterviewTimeStart())
                .leIfPresent(EmployeePerformanceDO::getInterviewTime, reqVO.getInterviewTimeEnd())
                .geIfPresent(EmployeePerformanceDO::getNextFollowTime, reqVO.getNextFollowTimeStart())
                .leIfPresent(EmployeePerformanceDO::getNextFollowTime, reqVO.getNextFollowTimeEnd());
    }
}
