package com.kyx.service.hr.dal.mysql.employee;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeRecruitmentPageReqVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeRecruitmentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface EmployeeRecruitmentMapper extends BaseMapperX<EmployeeRecruitmentDO> {

    default List<EmployeeRecruitmentDO> selectListByProfileId(Long profileId) {
        return selectList(EmployeeRecruitmentDO::getProfileId, profileId);
    }

    default PageResult<EmployeeRecruitmentDO> selectPage(EmployeeRecruitmentPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO).orderByDesc(EmployeeRecruitmentDO::getId));
    }

    default List<EmployeeRecruitmentDO> selectListByReq(EmployeeRecruitmentPageReqVO reqVO, Integer limit) {
        LambdaQueryWrapperX<EmployeeRecruitmentDO> wrapper = buildQuery(reqVO)
                .orderByDesc(EmployeeRecruitmentDO::getId);
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        return selectList(wrapper);
    }

    default LambdaQueryWrapperX<EmployeeRecruitmentDO> buildQuery(EmployeeRecruitmentPageReqVO reqVO) {
        LambdaQueryWrapperX<EmployeeRecruitmentDO> wrapper = new LambdaQueryWrapperX<EmployeeRecruitmentDO>()
                .eqIfPresent(EmployeeRecruitmentDO::getId, reqVO.getId())
                .eqIfPresent(EmployeeRecruitmentDO::getProfileId, reqVO.getProfileId())
                .inIfPresent(EmployeeRecruitmentDO::getProfileId, reqVO.getProfileIds())
                .likeIfPresent(EmployeeRecruitmentDO::getChannel, reqVO.getChannel())
                .likeIfPresent(EmployeeRecruitmentDO::getSource, reqVO.getSource())
                .likeIfPresent(EmployeeRecruitmentDO::getCampaignName, reqVO.getCampaignName())
                .likeIfPresent(EmployeeRecruitmentDO::getReferrerName, reqVO.getReferrerName())
                .eqIfPresent(EmployeeRecruitmentDO::getTouchStatus, reqVO.getTouchStatus())
                .likeIfPresent(EmployeeRecruitmentDO::getPosition, reqVO.getPosition())
                .likeIfPresent(EmployeeRecruitmentDO::getRecruiter, reqVO.getRecruiter())
                .likeIfPresent(EmployeeRecruitmentDO::getDemandCode, reqVO.getDemandCode())
                .likeIfPresent(EmployeeRecruitmentDO::getDemandDeptName, reqVO.getDemandDeptName())
                .eqIfPresent(EmployeeRecruitmentDO::getDemandStatus, reqVO.getDemandStatus())
                .eqIfPresent(EmployeeRecruitmentDO::getCandidateStage, reqVO.getCandidateStage())
                .eqIfPresent(EmployeeRecruitmentDO::getPriority, reqVO.getPriority())
                .eqIfPresent(EmployeeRecruitmentDO::getInterviewResult, reqVO.getInterviewResult())
                .eqIfPresent(EmployeeRecruitmentDO::getInterviewDecision, reqVO.getInterviewDecision())
                .eqIfPresent(EmployeeRecruitmentDO::getTalentStatus, reqVO.getTalentStatus())
                .likeIfPresent(EmployeeRecruitmentDO::getTalentTags, reqVO.getTalentTag());
        if ("PENDING".equals(reqVO.getResumeParseStatus())) {
            wrapper.and(item -> item.isNull(EmployeeRecruitmentDO::getResumeParseStatus)
                    .or()
                    .eq(EmployeeRecruitmentDO::getResumeParseStatus, "")
                    .or()
                    .eq(EmployeeRecruitmentDO::getResumeParseStatus, "PENDING"));
        } else {
            wrapper.eqIfPresent(EmployeeRecruitmentDO::getResumeParseStatus, reqVO.getResumeParseStatus());
        }
        return wrapper
                .eqIfPresent(EmployeeRecruitmentDO::getOfferStatus, reqVO.getOfferStatus())
                .eqIfPresent(EmployeeRecruitmentDO::getStatus, reqVO.getStatus())
                .geIfPresent(EmployeeRecruitmentDO::getInterviewTime, reqVO.getInterviewTimeStart())
                .leIfPresent(EmployeeRecruitmentDO::getInterviewTime, reqVO.getInterviewTimeEnd())
                .geIfPresent(EmployeeRecruitmentDO::getNextFollowTime, reqVO.getNextFollowTimeStart())
                .leIfPresent(EmployeeRecruitmentDO::getNextFollowTime, reqVO.getNextFollowTimeEnd())
                .geIfPresent(EmployeeRecruitmentDO::getLastContactTime, reqVO.getLastContactTimeStart())
                .leIfPresent(EmployeeRecruitmentDO::getLastContactTime, reqVO.getLastContactTimeEnd())
                .geIfPresent(EmployeeRecruitmentDO::getTouchTime, reqVO.getTouchTimeStart())
                .leIfPresent(EmployeeRecruitmentDO::getTouchTime, reqVO.getTouchTimeEnd())
                .geIfPresent(EmployeeRecruitmentDO::getOfferDate, reqVO.getOfferDateStart())
                .leIfPresent(EmployeeRecruitmentDO::getOfferDate, reqVO.getOfferDateEnd())
                .geIfPresent(EmployeeRecruitmentDO::getEntryDate, reqVO.getEntryDateStart())
                .leIfPresent(EmployeeRecruitmentDO::getEntryDate, reqVO.getEntryDateEnd());
    }
}
