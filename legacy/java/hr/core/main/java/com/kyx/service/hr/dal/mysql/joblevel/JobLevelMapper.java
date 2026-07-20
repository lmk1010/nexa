package com.kyx.service.hr.dal.mysql.joblevel;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.joblevel.vo.JobLevelPageReqVO;
import com.kyx.service.hr.dal.dataobject.joblevel.JobLevelDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 职级管理 Mapper
 *
 * @author MK
 */
@Mapper
public interface JobLevelMapper extends BaseMapperX<JobLevelDO> {

    default PageResult<JobLevelDO> selectPage(JobLevelPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<JobLevelDO>()
                .likeIfPresent(JobLevelDO::getLevelName, reqVO.getLevelName())
                .likeIfPresent(JobLevelDO::getLevelCode, reqVO.getLevelCode())
                .eqIfPresent(JobLevelDO::getSequenceId, reqVO.getSequenceId())
                .eqIfPresent(JobLevelDO::getStatus, reqVO.getStatus())
                .eqIfPresent(JobLevelDO::getTenantId, reqVO.getTenantId())
                .orderByAsc(JobLevelDO::getSort, JobLevelDO::getId));
    }

    default List<JobLevelDO> selectListBySequenceId(Long sequenceId) {
        return selectList(new LambdaQueryWrapperX<JobLevelDO>()
                .eq(JobLevelDO::getSequenceId, sequenceId)
                .orderByAsc(JobLevelDO::getSort, JobLevelDO::getId));
    }

    default List<JobLevelDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<JobLevelDO>()
                .eq(JobLevelDO::getStatus, status)
                .orderByAsc(JobLevelDO::getSort, JobLevelDO::getId));
    }

    default JobLevelDO selectByLevelCode(String levelCode) {
        return selectOne(JobLevelDO::getLevelCode, levelCode);
    }

    default JobLevelDO selectFirstByLevelName(String levelName) {
        return selectOne(new LambdaQueryWrapperX<JobLevelDO>()
                .eq(JobLevelDO::getLevelName, levelName)
                .orderByAsc(JobLevelDO::getId)
                .last("LIMIT 1"));
    }

    default List<JobLevelDO> selectByLevelCodeLike(String levelCode) {
        return selectList(new LambdaQueryWrapperX<JobLevelDO>()
                .like(JobLevelDO::getLevelCode, levelCode)
                .orderByAsc(JobLevelDO::getSort, JobLevelDO::getId));
    }

    default List<JobLevelDO> selectByLevelNameLike(String levelName) {
        return selectList(new LambdaQueryWrapperX<JobLevelDO>()
                .like(JobLevelDO::getLevelName, levelName)
                .orderByAsc(JobLevelDO::getSort, JobLevelDO::getId));
    }

    default List<JobLevelDO> selectAll() {
        return selectList(new LambdaQueryWrapperX<JobLevelDO>()
                .orderByAsc(JobLevelDO::getSort, JobLevelDO::getId));
    }

}
