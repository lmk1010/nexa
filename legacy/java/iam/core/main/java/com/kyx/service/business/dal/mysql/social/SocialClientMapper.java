package com.kyx.service.business.dal.mysql.social;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.business.controller.admin.socail.vo.client.SocialClientPageReqVO;
import com.kyx.service.business.dal.dataobject.social.SocialClientDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SocialClientMapper extends BaseMapperX<SocialClientDO> {

    default SocialClientDO selectBySocialTypeAndUserType(Integer socialType, Integer userType) {
        List<SocialClientDO> list = selectList(new LambdaQueryWrapperX<SocialClientDO>()
                .eq(SocialClientDO::getSocialType, socialType)
                .eq(SocialClientDO::getUserType, userType)
                // 优先使用启用状态，其次选择最近更新的配置，避免历史脏数据导致 selectOne 抛错
                .orderByAsc(SocialClientDO::getStatus)
                .orderByDesc(SocialClientDO::getUpdateTime)
                .orderByDesc(SocialClientDO::getId));
        return list.isEmpty() ? null : list.get(0);
    }

    default PageResult<SocialClientDO> selectPage(SocialClientPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SocialClientDO>()
                .likeIfPresent(SocialClientDO::getName, reqVO.getName())
                .eqIfPresent(SocialClientDO::getSocialType, reqVO.getSocialType())
                .eqIfPresent(SocialClientDO::getUserType, reqVO.getUserType())
                .likeIfPresent(SocialClientDO::getClientId, reqVO.getClientId())
                .eqIfPresent(SocialClientDO::getStatus, reqVO.getStatus())
                .orderByDesc(SocialClientDO::getId));
    }

}
