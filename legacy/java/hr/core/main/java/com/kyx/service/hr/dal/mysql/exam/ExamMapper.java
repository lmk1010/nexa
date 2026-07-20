package com.kyx.service.hr.dal.mysql.exam;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.exam.vo.ExamPageReqVO;
import com.kyx.service.hr.dal.dataobject.exam.ExamDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * HR 考试 Mapper
 *
 * @author MK
 */
@Mapper
public interface ExamMapper extends BaseMapperX<ExamDO> {

    default PageResult<ExamDO> selectPage(ExamPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ExamDO>()
                .likeIfPresent(ExamDO::getName, reqVO.getName())
                .eqIfPresent(ExamDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ExamDO::getExamType, reqVO.getExamType())
                .eqIfPresent(ExamDO::getTenantId, reqVO.getTenantId())
                .eqIfPresent(ExamDO::getCreator, reqVO.getCreator())
                .orderByDesc(ExamDO::getId));
    }

    default ExamDO selectByCode(String code) {
        return selectOne(ExamDO::getCode, code);
    }

}
