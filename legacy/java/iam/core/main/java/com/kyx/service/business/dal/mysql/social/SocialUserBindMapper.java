package com.kyx.service.business.dal.mysql.social;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.business.dal.dataobject.social.SocialUserBindDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface SocialUserBindMapper extends BaseMapperX<SocialUserBindDO> {

    default void deleteByUserTypeAndUserIdAndSocialType(Integer userType, Long userId, Integer socialType) {
        delete(new LambdaQueryWrapperX<SocialUserBindDO>()
                .eq(SocialUserBindDO::getUserType, userType)
                .eq(SocialUserBindDO::getUserId, userId)
                .eq(SocialUserBindDO::getSocialType, socialType));
    }

    default void deleteByUserTypeAndSocialUserId(Integer userType, Long socialUserId) {
        delete(new LambdaQueryWrapperX<SocialUserBindDO>()
                .eq(SocialUserBindDO::getUserType, userType)
                .eq(SocialUserBindDO::getSocialUserId, socialUserId));
    }

    default SocialUserBindDO selectByUserTypeAndSocialUserId(Integer userType, Long socialUserId) {
        return selectOne(SocialUserBindDO::getUserType, userType,
                SocialUserBindDO::getSocialUserId, socialUserId);
    }

    default List<SocialUserBindDO> selectListByUserIdAndUserType(Long userId, Integer userType) {
        return selectList(new LambdaQueryWrapperX<SocialUserBindDO>()
                .eq(SocialUserBindDO::getUserId, userId)
                .eq(SocialUserBindDO::getUserType, userType));
    }

    default SocialUserBindDO selectByUserIdAndUserTypeAndSocialType(Long userId, Integer userType, Integer socialType) {
        return selectOne(new LambdaQueryWrapperX<SocialUserBindDO>()
                .eq(SocialUserBindDO::getUserId, userId)
                .eq(SocialUserBindDO::getUserType, userType)
                .eq(SocialUserBindDO::getSocialType, socialType));
    }

    default List<SocialUserBindDO> selectListByUserIdsAndUserTypeAndSocialType(Collection<Long> userIds,
                                                                                Integer userType,
                                                                                Integer socialType) {
        return selectList(new LambdaQueryWrapperX<SocialUserBindDO>()
                .inIfPresent(SocialUserBindDO::getUserId, userIds)
                .eq(SocialUserBindDO::getUserType, userType)
                .eq(SocialUserBindDO::getSocialType, socialType));
    }

    default List<SocialUserBindDO> selectListBySocialUserIdsAndUserType(Collection<Long> socialUserIds,
                                                                         Integer userType) {
        return selectList(new LambdaQueryWrapperX<SocialUserBindDO>()
                .inIfPresent(SocialUserBindDO::getSocialUserId, socialUserIds)
                .eq(SocialUserBindDO::getUserType, userType));
    }

}
