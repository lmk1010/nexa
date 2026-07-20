package com.kyx.service.hr.dal.mysql.employee;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeTrainingPageReqVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeTrainingDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface EmployeeTrainingMapper extends BaseMapperX<EmployeeTrainingDO> {

    default List<EmployeeTrainingDO> selectListByProfileId(Long profileId) {
        return selectList(EmployeeTrainingDO::getProfileId, profileId);
    }

    default EmployeeTrainingDO selectArchive(Long profileId, String trainingName, String provider,
                                             LocalDate startDate, LocalDate endDate) {
        return selectOne(new LambdaQueryWrapperX<EmployeeTrainingDO>()
                .eq(EmployeeTrainingDO::getProfileId, profileId)
                .eq(EmployeeTrainingDO::getTrainingName, trainingName)
                .eqIfPresent(EmployeeTrainingDO::getProvider, provider)
                .eqIfPresent(EmployeeTrainingDO::getStartDate, startDate)
                .eqIfPresent(EmployeeTrainingDO::getEndDate, endDate)
                .last("LIMIT 1"));
    }

    default EmployeeTrainingDO selectBySource(String sourceType, Long sourceId) {
        return selectOne(new LambdaQueryWrapperX<EmployeeTrainingDO>()
                .eq(EmployeeTrainingDO::getSourceType, sourceType)
                .eq(EmployeeTrainingDO::getSourceId, sourceId)
                .last("LIMIT 1"));
    }

    default PageResult<EmployeeTrainingDO> selectPage(EmployeeTrainingPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO)
                .orderByDesc(EmployeeTrainingDO::getStartDate)
                .orderByDesc(EmployeeTrainingDO::getId));
    }

    default List<EmployeeTrainingDO> selectListByReq(EmployeeTrainingPageReqVO reqVO, Integer limit) {
        LambdaQueryWrapperX<EmployeeTrainingDO> wrapper = buildQuery(reqVO)
                .orderByDesc(EmployeeTrainingDO::getStartDate)
                .orderByDesc(EmployeeTrainingDO::getId);
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        return selectList(wrapper);
    }

    default LambdaQueryWrapperX<EmployeeTrainingDO> buildQuery(EmployeeTrainingPageReqVO reqVO) {
        LambdaQueryWrapperX<EmployeeTrainingDO> wrapper = new LambdaQueryWrapperX<EmployeeTrainingDO>()
                .eqIfPresent(EmployeeTrainingDO::getProfileId, reqVO.getProfileId())
                .inIfPresent(EmployeeTrainingDO::getProfileId, reqVO.getProfileIds())
                .eqIfPresent(EmployeeTrainingDO::getCourseId, reqVO.getCourseId())
                .eqIfPresent(EmployeeTrainingDO::getPlanId, reqVO.getPlanId())
                .eqIfPresent(EmployeeTrainingDO::getAssignmentId, reqVO.getAssignmentId())
                .eqIfPresent(EmployeeTrainingDO::getExamId, reqVO.getExamId())
                .eqIfPresent(EmployeeTrainingDO::getQuestionnaireId, reqVO.getQuestionnaireId())
                .likeIfPresent(EmployeeTrainingDO::getTrainingName, reqVO.getTrainingName())
                .likeIfPresent(EmployeeTrainingDO::getProvider, reqVO.getProvider())
                .likeIfPresent(EmployeeTrainingDO::getResult, reqVO.getResult())
                .geIfPresent(EmployeeTrainingDO::getStartDate, reqVO.getStartDateStart())
                .leIfPresent(EmployeeTrainingDO::getStartDate, reqVO.getStartDateEnd())
                .geIfPresent(EmployeeTrainingDO::getEndDate, reqVO.getEndDateStart())
                .leIfPresent(EmployeeTrainingDO::getEndDate, reqVO.getEndDateEnd())
                .geIfPresent(EmployeeTrainingDO::getCertificateExpireDate, reqVO.getCertificateExpireDateStart())
                .leIfPresent(EmployeeTrainingDO::getCertificateExpireDate, reqVO.getCertificateExpireDateEnd())
                .geIfPresent(EmployeeTrainingDO::getRetrainDate, reqVO.getRetrainDateStart())
                .leIfPresent(EmployeeTrainingDO::getRetrainDate, reqVO.getRetrainDateEnd());
        if (Boolean.TRUE.equals(reqVO.getRetrainDueOnly())) {
            wrapper.isNotNull(EmployeeTrainingDO::getRetrainDate)
                    .le(EmployeeTrainingDO::getRetrainDate, LocalDate.now().plusDays(30));
        }
        return wrapper;
    }
}
