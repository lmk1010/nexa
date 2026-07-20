package com.kyx.service.hr.dal.mysql.exam;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.exam.ExamAttemptDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HR 考试作答记录 Mapper
 *
 * @author MK
 */
@Mapper
public interface ExamAttemptMapper extends BaseMapperX<ExamAttemptDO> {

    default List<ExamAttemptDO> selectListByExamId(Long examId) {
        return selectList(new LambdaQueryWrapperX<ExamAttemptDO>()
                .eq(ExamAttemptDO::getExamId, examId));
    }

}
