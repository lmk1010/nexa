package com.kyx.service.hr.dal.mysql.sequence;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.sequence.vo.SequencePageReqVO;
import com.kyx.service.hr.dal.dataobject.sequence.SequenceDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 序列管理 Mapper
 *
 * @author MK
 */
@Mapper
public interface SequenceMapper extends BaseMapperX<SequenceDO> {

    default PageResult<SequenceDO> selectPage(SequencePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SequenceDO>()
                .likeIfPresent(SequenceDO::getSequenceName, reqVO.getSequenceName())
                .eqIfPresent(SequenceDO::getParentId, reqVO.getParentId())
                .eqIfPresent(SequenceDO::getStatus, reqVO.getStatus())
                .orderByAsc(SequenceDO::getSort, SequenceDO::getId));
    }

    default List<SequenceDO> selectListByParentId(Long parentId) {
        return selectList(new LambdaQueryWrapperX<SequenceDO>()
                .eq(SequenceDO::getParentId, parentId)
                .orderByAsc(SequenceDO::getSort, SequenceDO::getId));
    }

    default List<SequenceDO> selectTreeList() {
        return selectList(new LambdaQueryWrapperX<SequenceDO>()
                .orderByAsc(SequenceDO::getSort, SequenceDO::getId));
    }

    default List<SequenceDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<SequenceDO>()
                .eq(SequenceDO::getStatus, status)
                .orderByAsc(SequenceDO::getSort, SequenceDO::getId));
    }

    default SequenceDO selectBySequenceName(String sequenceName) {
        return selectOne(SequenceDO::getSequenceName, sequenceName);
    }

    default List<SequenceDO> selectBySequenceNameLike(String sequenceName) {
        return selectList(new LambdaQueryWrapperX<SequenceDO>()
                .like(SequenceDO::getSequenceName, sequenceName)
                .orderByAsc(SequenceDO::getSort, SequenceDO::getId));
    }

} 