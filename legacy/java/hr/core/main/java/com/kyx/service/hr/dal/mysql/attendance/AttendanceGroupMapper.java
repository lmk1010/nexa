package com.kyx.service.hr.dal.mysql.attendance;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceGroupPageReqVO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceGroupDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 考勤组 Mapper
 */
@Mapper
public interface AttendanceGroupMapper extends BaseMapperX<AttendanceGroupDO> {

    default PageResult<AttendanceGroupDO> selectPage(AttendanceGroupPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AttendanceGroupDO>()
                .likeIfPresent(AttendanceGroupDO::getGroupName, reqVO.getGroupName())
                .eqIfPresent(AttendanceGroupDO::getStatus, reqVO.getStatus())
                .orderByAsc(AttendanceGroupDO::getId));
    }

    default List<AttendanceGroupDO> selectActiveList() {
        return selectList(new LambdaQueryWrapperX<AttendanceGroupDO>()
                .eq(AttendanceGroupDO::getStatus, 0)
                .orderByAsc(AttendanceGroupDO::getId));
    }

}
