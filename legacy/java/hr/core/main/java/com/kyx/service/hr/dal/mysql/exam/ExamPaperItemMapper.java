package com.kyx.service.hr.dal.mysql.exam;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.exam.ExamPaperItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * HR 考试试卷题目 Mapper
 *
 * @author MK
 */
@Mapper
public interface ExamPaperItemMapper extends BaseMapperX<ExamPaperItemDO> {

    default List<ExamPaperItemDO> selectListByPaperIds(Collection<Long> paperIds) {
        return selectList(new LambdaQueryWrapperX<ExamPaperItemDO>()
                .in(ExamPaperItemDO::getPaperId, paperIds)
                .orderByAsc(ExamPaperItemDO::getSortNo));
    }

    @Select({"<script>",
            "SELECT * FROM hr_exam_paper_item WHERE paper_id IN",
            "<foreach collection='paperIds' item='paperId' open='(' separator=',' close=')'>#{paperId}</foreach>",
            "ORDER BY sort_no",
            "</script>"})
    List<ExamPaperItemDO> selectListByPaperIdsIncludeDeleted(@Param("paperIds") Collection<Long> paperIds);

    @Select("SELECT * FROM hr_exam_paper_item WHERE id = #{id}")
    ExamPaperItemDO selectByIdIncludeDeleted(@Param("id") Long id);

    default void deleteByPaperIds(Collection<Long> paperIds) {
        delete(new LambdaQueryWrapperX<ExamPaperItemDO>().in(ExamPaperItemDO::getPaperId, paperIds));
    }

}
