package com.kyx.service.op.dal.mysql.demo.demo03.inner;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.service.op.dal.dataobject.demo.demo03.Demo03GradeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 学生班级 Mapper
 *
 * @author MK
 */
@Mapper
public interface Demo03GradeInnerMapper extends BaseMapperX<Demo03GradeDO> {

    default Demo03GradeDO selectByStudentId(Long studentId) {
        return selectOne(Demo03GradeDO::getStudentId, studentId);
    }

    default int deleteByStudentId(Long studentId) {
        return delete(Demo03GradeDO::getStudentId, studentId);
    }

    default int deleteByStudentIds(List<Long> studentIds) {
        return deleteBatch(Demo03GradeDO::getStudentId, studentIds);
    }

}
