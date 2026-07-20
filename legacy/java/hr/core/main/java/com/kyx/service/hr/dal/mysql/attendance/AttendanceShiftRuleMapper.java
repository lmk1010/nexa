package com.kyx.service.hr.dal.mysql.attendance;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceShiftRuleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 考勤班次规则 Mapper
 */
@Mapper
public interface AttendanceShiftRuleMapper extends BaseMapperX<AttendanceShiftRuleDO> {

    default List<AttendanceShiftRuleDO> selectActiveList() {
        return selectList(new LambdaQueryWrapperX<AttendanceShiftRuleDO>()
                .eq(AttendanceShiftRuleDO::getStatus, 0)
                .orderByDesc(AttendanceShiftRuleDO::getDefaultFlag)
                .orderByAsc(AttendanceShiftRuleDO::getId));
    }

    default AttendanceShiftRuleDO selectDefault() {
        return selectOne(new LambdaQueryWrapperX<AttendanceShiftRuleDO>()
                .eq(AttendanceShiftRuleDO::getStatus, 0)
                .eq(AttendanceShiftRuleDO::getDefaultFlag, true)
                .last("LIMIT 1"));
    }

    default void clearDefault(Long excludeId) {
        update(null, new LambdaUpdateWrapper<AttendanceShiftRuleDO>()
                .ne(excludeId != null, AttendanceShiftRuleDO::getId, excludeId)
                .set(AttendanceShiftRuleDO::getDefaultFlag, false));
    }

}
