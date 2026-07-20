package com.kyx.service.hr.dal.mysql.onboarding;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.onboarding.OnboardingAttachmentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OnboardingAttachmentMapper extends BaseMapperX<OnboardingAttachmentDO> {

    default List<OnboardingAttachmentDO> selectListByOnboardingId(Long onboardingId) {
        return selectList(OnboardingAttachmentDO::getOnboardingId, onboardingId);
    }

    default List<OnboardingAttachmentDO> selectListByOnboardingIdAndType(Long onboardingId, Integer attachmentType) {
        return selectList(new LambdaQueryWrapperX<OnboardingAttachmentDO>()
                .eq(OnboardingAttachmentDO::getOnboardingId, onboardingId)
                .eq(OnboardingAttachmentDO::getAttachmentType, attachmentType));
    }

} 