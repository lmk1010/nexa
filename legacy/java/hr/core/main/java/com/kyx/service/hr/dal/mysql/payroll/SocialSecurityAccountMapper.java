package com.kyx.service.hr.dal.mysql.payroll;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.payroll.vo.SocialSecurityAccountPageReqVO;
import com.kyx.service.hr.dal.dataobject.payroll.SocialSecurityAccountDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface SocialSecurityAccountMapper extends BaseMapperX<SocialSecurityAccountDO> {

    default PageResult<SocialSecurityAccountDO> selectPage(SocialSecurityAccountPageReqVO reqVO) {
        LambdaQueryWrapperX<SocialSecurityAccountDO> wrapper = new LambdaQueryWrapperX<SocialSecurityAccountDO>()
                .eqIfPresent(SocialSecurityAccountDO::getProfileId, reqVO.getProfileId())
                .inIfPresent(SocialSecurityAccountDO::getProfileId, reqVO.getProfileIds())
                .eqIfPresent(SocialSecurityAccountDO::getSocialMonth, reqVO.getSocialMonth())
                .eqIfPresent(SocialSecurityAccountDO::getStatus, reqVO.getStatus())
                .likeIfPresent(SocialSecurityAccountDO::getCity, reqVO.getCity());
        wrapper.orderByDesc(SocialSecurityAccountDO::getSocialMonth)
                .orderByDesc(SocialSecurityAccountDO::getId);
        return selectPage(reqVO, wrapper);
    }

    default List<SocialSecurityAccountDO> selectListByMonthAndProfileIds(String socialMonth, Collection<Long> profileIds) {
        return selectList(new LambdaQueryWrapperX<SocialSecurityAccountDO>()
                .eq(SocialSecurityAccountDO::getSocialMonth, socialMonth)
                .in(SocialSecurityAccountDO::getProfileId, profileIds)
                .orderByDesc(SocialSecurityAccountDO::getId));
    }

}
