package com.kyx.service.hr.dal.mysql.exam;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.exam.vo.ExamPublishPageReqVO;
import com.kyx.service.hr.dal.dataobject.exam.ExamPublishDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * HR 考试发布配置 Mapper
 *
 * @author MK
 */
@Mapper
public interface ExamPublishMapper extends BaseMapperX<ExamPublishDO> {

    default PageResult<ExamPublishDO> selectPage(ExamPublishPageReqVO reqVO) {
        LambdaQueryWrapperX<ExamPublishDO> queryWrapper = new LambdaQueryWrapperX<ExamPublishDO>()
                .eqIfPresent(ExamPublishDO::getExamId, reqVO.getExamId())
                .eqIfPresent(ExamPublishDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ExamPublishDO::getTenantId, reqVO.getTenantId())
                .eqIfPresent(ExamPublishDO::getCreator, reqVO.getCreator())
                .orderByDesc(ExamPublishDO::getId);
        if (reqVO.getParentPublishId() != null) {
            queryWrapper.eq(ExamPublishDO::getParentPublishId, reqVO.getParentPublishId());
        } else {
            queryWrapper.isNull(ExamPublishDO::getParentPublishId);
        }
        return selectPage(reqVO, queryWrapper);
    }

    default List<ExamPublishDO> selectListByScheduleDue(LocalDateTime now) {
        return selectList(new LambdaQueryWrapperX<ExamPublishDO>()
                .eq(ExamPublishDO::getPublishType, 0)
                .eq(ExamPublishDO::getSendType, 1)
                .eq(ExamPublishDO::getStatus, 0)
                .isNull(ExamPublishDO::getParentPublishId)
                .le(ExamPublishDO::getSendAt, now));
    }

    default List<ExamPublishDO> selectRecurringDue(LocalDateTime now) {
        return selectList(new LambdaQueryWrapperX<ExamPublishDO>()
                .eq(ExamPublishDO::getPublishType, 1)
                .in(ExamPublishDO::getStatus, 0, 1)
                .isNull(ExamPublishDO::getParentPublishId)
                .le(ExamPublishDO::getNextPublishAt, now));
    }

    default List<ExamPublishDO> selectExpiredBatches(LocalDateTime now) {
        return selectList(new LambdaQueryWrapperX<ExamPublishDO>()
                .isNotNull(ExamPublishDO::getParentPublishId)
                .eq(ExamPublishDO::getStatus, 1)
                .le(ExamPublishDO::getEndAt, now));
    }

    default List<ExamPublishDO> selectExpiredOneTimePublishes(LocalDateTime now) {
        return selectList(new LambdaQueryWrapperX<ExamPublishDO>()
                .eq(ExamPublishDO::getPublishType, 0)
                .isNull(ExamPublishDO::getParentPublishId)
                .eq(ExamPublishDO::getStatus, 1)
                .le(ExamPublishDO::getEndAt, now));
    }

    default List<ExamPublishDO> selectBatchesByParentId(Long parentPublishId) {
        return selectList(new LambdaQueryWrapperX<ExamPublishDO>()
                .eq(ExamPublishDO::getParentPublishId, parentPublishId)
                .orderByDesc(ExamPublishDO::getBatchNo));
    }

    default ExamPublishDO selectLatestRootByExam(Long examId, Integer publishType) {
        return selectOne(new LambdaQueryWrapperX<ExamPublishDO>()
                .eq(ExamPublishDO::getExamId, examId)
                .eqIfPresent(ExamPublishDO::getPublishType, publishType)
                .isNull(ExamPublishDO::getParentPublishId)
                .orderByDesc(ExamPublishDO::getId)
                .last("LIMIT 1"));
    }

}
