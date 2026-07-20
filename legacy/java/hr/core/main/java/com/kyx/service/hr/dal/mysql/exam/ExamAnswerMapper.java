package com.kyx.service.hr.dal.mysql.exam;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.exam.ExamAnswerDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Collection;

/**
 * HR 考试答案 Mapper
 *
 * @author MK
 */
@Mapper
public interface ExamAnswerMapper extends BaseMapperX<ExamAnswerDO> {

    default List<ExamAnswerDO> selectListByAttemptId(Long attemptId) {
        return selectList(new LambdaQueryWrapperX<ExamAnswerDO>()
                .eq(ExamAnswerDO::getAttemptId, attemptId));
    }

    default List<ExamAnswerDO> selectListByAttemptIds(Collection<Long> attemptIds) {
        return selectList(new LambdaQueryWrapperX<ExamAnswerDO>()
                .in(ExamAnswerDO::getAttemptId, attemptIds));
    }

    default List<ExamAnswerDO> selectListByAttemptIdAndStatus(Long attemptId, Integer gradeStatus) {
        return selectList(new LambdaQueryWrapperX<ExamAnswerDO>()
                .eq(ExamAnswerDO::getAttemptId, attemptId)
                .eq(ExamAnswerDO::getGradeStatus, gradeStatus));
    }

    default void deleteByAttemptId(Long attemptId) {
        delete(new LambdaQueryWrapperX<ExamAnswerDO>()
                .eq(ExamAnswerDO::getAttemptId, attemptId));
    }

}
