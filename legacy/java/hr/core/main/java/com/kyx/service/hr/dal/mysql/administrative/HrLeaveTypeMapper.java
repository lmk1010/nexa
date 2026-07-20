package com.kyx.service.hr.dal.mysql.administrative;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.administrative.HrLeaveTypeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 假期类型 Mapper
 */
@Mapper
public interface HrLeaveTypeMapper extends BaseMapperX<HrLeaveTypeDO> {

    default HrLeaveTypeDO selectByCode(String typeCode) {
        return selectOne(new LambdaQueryWrapperX<HrLeaveTypeDO>()
                .eq(HrLeaveTypeDO::getTypeCode, typeCode)
                .last("LIMIT 1"));
    }

    default List<HrLeaveTypeDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapperX<HrLeaveTypeDO>()
                .eq(HrLeaveTypeDO::getStatus, 0)
                .orderByAsc(HrLeaveTypeDO::getId));
    }

    default List<HrLeaveTypeDO> selectAllList() {
        return selectList(new LambdaQueryWrapperX<HrLeaveTypeDO>()
                .orderByAsc(HrLeaveTypeDO::getStatus)
                .orderByAsc(HrLeaveTypeDO::getId));
    }

}
