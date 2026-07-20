package com.kyx.service.hr.dal.mysql.onboarding;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.onboarding.vo.OnboardingPageReqVO;
import com.kyx.service.hr.dal.dataobject.onboarding.OnboardingDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface OnboardingMapper extends BaseMapperX<OnboardingDO> {

    default PageResult<OnboardingDO> selectPage(OnboardingPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OnboardingDO>()
                .likeIfPresent(OnboardingDO::getApplicationNo, reqVO.getApplicationNo())
                .likeIfPresent(OnboardingDO::getApplicantName, reqVO.getApplicantName())
                .likeIfPresent(OnboardingDO::getApplicantMobile, reqVO.getApplicantMobile())
                .likeIfPresent(OnboardingDO::getApplicantIdCard, reqVO.getApplicantIdCard())
                .eqIfPresent(OnboardingDO::getGender, reqVO.getGender())
                .eqIfPresent(OnboardingDO::getStatus, reqVO.getStatus())
                .eqIfPresent(OnboardingDO::getApprovalType, reqVO.getApprovalType())
                .eqIfPresent(OnboardingDO::getDeptId, reqVO.getDeptId())
                .betweenIfPresent(OnboardingDO::getExpectedEntryDate, reqVO.getExpectedEntryDateStart(), reqVO.getExpectedEntryDateEnd())
                .betweenIfPresent(OnboardingDO::getCreateTime, reqVO.getCreateTimeStart(), reqVO.getCreateTimeEnd())
                .orderByDesc(OnboardingDO::getId));
    }

    default List<OnboardingDO> selectList(Collection<Long> ids) {
        return selectList(OnboardingDO::getId, ids);
    }

    default OnboardingDO selectByApplicationNo(String applicationNo) {
        return selectOne(OnboardingDO::getApplicationNo, applicationNo);
    }

    default OnboardingDO selectByIdCard(String idCard) {
        return selectOne(OnboardingDO::getApplicantIdCard, idCard);
    }

    default List<OnboardingDO> selectListByStatus(Integer status) {
        return selectList(OnboardingDO::getStatus, status);
    }

    default List<OnboardingDO> selectListByApprovalType(Integer approvalType) {
        return selectList(OnboardingDO::getApprovalType, approvalType);
    }

    default List<OnboardingDO> selectListByDeptId(Long deptId) {
        return selectList(OnboardingDO::getDeptId, deptId);
    }

    /**
     * 查询当天最大申请编号
     *
     * @param dateStr 日期字符串，格式：yyyyMMdd
     * @return 最大申请编号
     */
    default String selectMaxApplicationNoByDate(String dateStr) {
        List<OnboardingDO> results = selectList(new LambdaQueryWrapperX<OnboardingDO>()
                .likeRight(OnboardingDO::getApplicationNo, "ON" + dateStr)
                .orderByDesc(OnboardingDO::getApplicationNo)
                .last("LIMIT 1"));
        return results.isEmpty() ? null : results.get(0).getApplicationNo();
    }

} 