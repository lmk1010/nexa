package com.kyx.service.hr.dal.mysql.exam;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.exam.ExamPaperDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * HR 考试试卷 Mapper
 *
 * @author MK
 */
@Mapper
public interface ExamPaperMapper extends BaseMapperX<ExamPaperDO> {

    default List<ExamPaperDO> selectListByExamId(Long examId) {
        return selectList(new LambdaQueryWrapperX<ExamPaperDO>()
                .eq(ExamPaperDO::getExamId, examId)
                .orderByDesc(ExamPaperDO::getId));
    }

    default void deleteByExamId(Long examId) {
        delete(new LambdaQueryWrapperX<ExamPaperDO>().eq(ExamPaperDO::getExamId, examId));
    }

    @Select("SELECT * FROM hr_exam_paper WHERE id = #{id}")
    ExamPaperDO selectByIdIncludeDeleted(@Param("id") Long id);

}
