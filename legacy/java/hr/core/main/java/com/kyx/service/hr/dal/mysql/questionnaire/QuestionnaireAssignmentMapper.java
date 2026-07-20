package com.kyx.service.hr.dal.mysql.questionnaire;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireAssignmentPageReqVO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnaireAssignmentDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * HR 问卷分配 Mapper
 *
 * @author MK
 */
@Mapper
public interface QuestionnaireAssignmentMapper extends BaseMapperX<QuestionnaireAssignmentDO> {

    default PageResult<QuestionnaireAssignmentDO> selectPage(QuestionnaireAssignmentPageReqVO reqVO) {
        LambdaQueryWrapperX<QuestionnaireAssignmentDO> queryWrapper = new LambdaQueryWrapperX<QuestionnaireAssignmentDO>()
                .eqIfPresent(QuestionnaireAssignmentDO::getQuestionnaireId, reqVO.getQuestionnaireId())
                .eqIfPresent(QuestionnaireAssignmentDO::getPublishId, reqVO.getPublishId())
                .eqIfPresent(QuestionnaireAssignmentDO::getBatchNo, reqVO.getBatchNo())
                .eqIfPresent(QuestionnaireAssignmentDO::getEvaluatorId, reqVO.getEvaluatorId())
                .likeIfPresent(QuestionnaireAssignmentDO::getEvaluatorName, reqVO.getEvaluatorName())
                .eqIfPresent(QuestionnaireAssignmentDO::getTargetId, reqVO.getTargetId())
                .likeIfPresent(QuestionnaireAssignmentDO::getTargetName, reqVO.getTargetName())
                .eqIfPresent(QuestionnaireAssignmentDO::getStatus, reqVO.getStatus())
                .orderByDesc(QuestionnaireAssignmentDO::getId);
        if (Boolean.TRUE.equals(reqVO.getVisibleOnly())) {
            LocalDateTime now = LocalDateTime.now();
            queryWrapper.ne(QuestionnaireAssignmentDO::getStatus, 2);
            queryWrapper.and(wrapper -> wrapper
                    .isNull(QuestionnaireAssignmentDO::getBatchStartAt)
                    .or()
                    .le(QuestionnaireAssignmentDO::getBatchStartAt, now));
            queryWrapper.and(wrapper -> wrapper
                    .isNull(QuestionnaireAssignmentDO::getBatchEndAt)
                    .or()
                    .ge(QuestionnaireAssignmentDO::getBatchEndAt, now));
        }
        return selectPage(reqVO, queryWrapper);
    }

    default List<QuestionnaireAssignmentDO> selectListByQuestionnaireId(Long questionnaireId) {
        return selectList(new LambdaQueryWrapperX<QuestionnaireAssignmentDO>()
                .eq(QuestionnaireAssignmentDO::getQuestionnaireId, questionnaireId));
    }

    default List<QuestionnaireAssignmentDO> selectListByPublishId(Long publishId) {
        return selectList(new LambdaQueryWrapperX<QuestionnaireAssignmentDO>()
                .eq(QuestionnaireAssignmentDO::getPublishId, publishId));
    }

    default List<QuestionnaireAssignmentDO> selectListByPublishIdAndBatchNo(Long publishId, Integer batchNo) {
        return selectList(new LambdaQueryWrapperX<QuestionnaireAssignmentDO>()
                .eq(QuestionnaireAssignmentDO::getPublishId, publishId)
                .eqIfPresent(QuestionnaireAssignmentDO::getBatchNo, batchNo));
    }

    default Integer selectLatestBatchNoByPublishId(Long publishId) {
        QuestionnaireAssignmentDO one = selectOne(new LambdaQueryWrapperX<QuestionnaireAssignmentDO>()
                .eq(QuestionnaireAssignmentDO::getPublishId, publishId)
                .orderByDesc(QuestionnaireAssignmentDO::getBatchNo)
                .last("LIMIT 1"));
        return one != null ? one.getBatchNo() : null;
    }

    default List<QuestionnaireAssignmentDO> selectListByEvaluatorId(Long evaluatorId, Integer status) {
        return selectList(new LambdaQueryWrapperX<QuestionnaireAssignmentDO>()
                .eq(QuestionnaireAssignmentDO::getEvaluatorId, evaluatorId)
                .eqIfPresent(QuestionnaireAssignmentDO::getStatus, status)
                .orderByDesc(QuestionnaireAssignmentDO::getId));
    }

    default Set<Long> selectPublishIdsByEvaluatorId(Long evaluatorId, Integer status) {
        return selectListByEvaluatorId(evaluatorId, status).stream()
                .map(QuestionnaireAssignmentDO::getPublishId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    default boolean existsByPublishIdAndPair(Long publishId, Long evaluatorId, Long targetId, Integer batchNo) {
        LambdaQueryWrapperX<QuestionnaireAssignmentDO> queryWrapper = new LambdaQueryWrapperX<QuestionnaireAssignmentDO>()
                .eq(QuestionnaireAssignmentDO::getPublishId, publishId)
                .eqIfPresent(QuestionnaireAssignmentDO::getBatchNo, batchNo)
                .eq(QuestionnaireAssignmentDO::getEvaluatorId, evaluatorId);
        if (targetId == null) {
            queryWrapper.isNull(QuestionnaireAssignmentDO::getTargetId);
        } else {
            queryWrapper.eq(QuestionnaireAssignmentDO::getTargetId, targetId);
        }
        return selectCount(queryWrapper) > 0;
    }

    default boolean existsByBusinessKey(Long questionnaireId, Long publishId, Integer batchNo,
                                        Long evaluatorId, Long targetId, Long excludeId) {
        LambdaQueryWrapperX<QuestionnaireAssignmentDO> queryWrapper = new LambdaQueryWrapperX<QuestionnaireAssignmentDO>()
                .eq(QuestionnaireAssignmentDO::getQuestionnaireId, questionnaireId)
                .eq(QuestionnaireAssignmentDO::getEvaluatorId, evaluatorId)
                .neIfPresent(QuestionnaireAssignmentDO::getId, excludeId);
        if (targetId == null) {
            queryWrapper.isNull(QuestionnaireAssignmentDO::getTargetId);
        } else {
            queryWrapper.eq(QuestionnaireAssignmentDO::getTargetId, targetId);
        }
        if (publishId == null) {
            queryWrapper.isNull(QuestionnaireAssignmentDO::getPublishId);
        } else {
            queryWrapper.eq(QuestionnaireAssignmentDO::getPublishId, publishId);
        }
        if (batchNo == null) {
            queryWrapper.isNull(QuestionnaireAssignmentDO::getBatchNo);
        } else {
            queryWrapper.eq(QuestionnaireAssignmentDO::getBatchNo, batchNo);
        }
        return selectCount(queryWrapper) > 0;
    }

    default int deleteUnsubmittedByPublishId(Long publishId) {
        return delete(new LambdaQueryWrapperX<QuestionnaireAssignmentDO>()
                .eq(QuestionnaireAssignmentDO::getPublishId, publishId)
                .eq(QuestionnaireAssignmentDO::getStatus, 0));
    }

    default List<QuestionnaireAssignmentDO> selectPendingListByPublishId(Long publishId) {
        return selectList(new LambdaQueryWrapperX<QuestionnaireAssignmentDO>()
                .eq(QuestionnaireAssignmentDO::getPublishId, publishId)
                .eq(QuestionnaireAssignmentDO::getStatus, 0));
    }

    default int deleteByPublishId(Long publishId) {
        return delete(new LambdaQueryWrapperX<QuestionnaireAssignmentDO>()
                .eq(QuestionnaireAssignmentDO::getPublishId, publishId));
    }

    default int deleteByPublishIdAndBatchNo(Long publishId, Integer batchNo) {
        return delete(new LambdaQueryWrapperX<QuestionnaireAssignmentDO>()
                .eq(QuestionnaireAssignmentDO::getPublishId, publishId)
                .eqIfPresent(QuestionnaireAssignmentDO::getBatchNo, batchNo));
    }

    default int updateBatchEndAtByPublishId(Long publishId, Integer batchNo, LocalDateTime batchEndAt) {
        if (publishId == null || batchEndAt == null) {
            return 0;
        }
        QuestionnaireAssignmentDO update = new QuestionnaireAssignmentDO();
        update.setBatchEndAt(batchEndAt);
        return update(update, new LambdaQueryWrapperX<QuestionnaireAssignmentDO>()
                .eq(QuestionnaireAssignmentDO::getPublishId, publishId)
                .eqIfPresent(QuestionnaireAssignmentDO::getBatchNo, batchNo));
    }

    default int deleteByQuestionnaireId(Long questionnaireId) {
        return delete(new LambdaQueryWrapperX<QuestionnaireAssignmentDO>()
                .eq(QuestionnaireAssignmentDO::getQuestionnaireId, questionnaireId));
    }

}
