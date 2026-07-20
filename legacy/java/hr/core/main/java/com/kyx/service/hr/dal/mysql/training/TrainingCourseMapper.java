package com.kyx.service.hr.dal.mysql.training;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.training.vo.TrainingCoursePageReqVO;
import com.kyx.service.hr.dal.dataobject.training.TrainingCourseDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TrainingCourseMapper extends BaseMapperX<TrainingCourseDO> {

    default PageResult<TrainingCourseDO> selectPage(TrainingCoursePageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO)
                .orderByDesc(TrainingCourseDO::getId));
    }

    default List<TrainingCourseDO> selectListByReq(TrainingCoursePageReqVO reqVO, Integer limit) {
        LambdaQueryWrapperX<TrainingCourseDO> wrapper = buildQuery(reqVO).orderByDesc(TrainingCourseDO::getId);
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        return selectList(wrapper);
    }

    default LambdaQueryWrapperX<TrainingCourseDO> buildQuery(TrainingCoursePageReqVO reqVO) {
        return new LambdaQueryWrapperX<TrainingCourseDO>()
                .likeIfPresent(TrainingCourseDO::getCourseName, reqVO.getCourseName())
                .likeIfPresent(TrainingCourseDO::getCourseType, reqVO.getCourseType())
                .likeIfPresent(TrainingCourseDO::getCategory, reqVO.getCategory())
                .likeIfPresent(TrainingCourseDO::getLecturer, reqVO.getLecturer())
                .likeIfPresent(TrainingCourseDO::getProvider, reqVO.getProvider())
                .eqIfPresent(TrainingCourseDO::getStatus, reqVO.getStatus());
    }

}
