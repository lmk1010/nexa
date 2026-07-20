package com.kyx.service.business.dal.mysql.dict;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.business.controller.admin.dict.vo.type.DictTypePageReqVO;
import com.kyx.service.business.dal.dataobject.dict.DictTypeDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface DictTypeMapper extends BaseMapperX<DictTypeDO> {

    default PageResult<DictTypeDO> selectPage(DictTypePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<DictTypeDO>()
                .likeIfPresent(DictTypeDO::getName, reqVO.getName())
                .likeIfPresent(DictTypeDO::getType, reqVO.getType())
                .eqIfPresent(DictTypeDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(DictTypeDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(DictTypeDO::getId));
    }

    default DictTypeDO selectByType(String type) {
        return selectOne(DictTypeDO::getType, type);
    }

    default DictTypeDO selectByName(String name) {
        return selectOne(DictTypeDO::getName, name);
    }

    @Update("UPDATE system_dict_type SET deleted = 1, deleted_time = #{deletedTime} WHERE id = #{id}")
    void updateToDelete(@Param("id") Long id, @Param("deletedTime") LocalDateTime deletedTime);

}
