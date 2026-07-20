package com.kyx.service.hr.dal.mysql.employee;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeProfileChangePageReqVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileChangeRequestDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Employee profile change request Mapper.
 */
@Mapper
public interface EmployeeProfileChangeRequestMapper extends BaseMapperX<EmployeeProfileChangeRequestDO> {

    default PageResult<EmployeeProfileChangeRequestDO> selectPage(EmployeeProfileChangePageReqVO reqVO) {
        LambdaQueryWrapperX<EmployeeProfileChangeRequestDO> wrapper = new LambdaQueryWrapperX<EmployeeProfileChangeRequestDO>()
                .eqIfPresent(EmployeeProfileChangeRequestDO::getProfileId, reqVO.getProfileId())
                .eqIfPresent(EmployeeProfileChangeRequestDO::getUserId, reqVO.getUserId())
                .eqIfPresent(EmployeeProfileChangeRequestDO::getChangeType, reqVO.getChangeType())
                .eqIfPresent(EmployeeProfileChangeRequestDO::getStatus, reqVO.getStatus());
        if (StringUtils.hasText(reqVO.getKeyword())) {
            String keyword = reqVO.getKeyword().trim();
            Long id = parseLong(keyword);
            wrapper.and(item -> {
                if (id != null) {
                    item.eq(EmployeeProfileChangeRequestDO::getId, id).or();
                }
                item.like(EmployeeProfileChangeRequestDO::getChangeSummary, keyword)
                        .or()
                        .like(EmployeeProfileChangeRequestDO::getReason, keyword);
            });
        }
        wrapper.orderByDesc(EmployeeProfileChangeRequestDO::getId);
        return selectPage(reqVO, wrapper);
    }

    static Long parseLong(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    default List<EmployeeProfileChangeRequestDO> selectPendingList(Integer limit) {
        LambdaQueryWrapperX<EmployeeProfileChangeRequestDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(EmployeeProfileChangeRequestDO::getStatus, "PENDING")
                .isNull(EmployeeProfileChangeRequestDO::getProcessInstanceId)
                .orderByDesc(EmployeeProfileChangeRequestDO::getId);
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        return selectList(wrapper);
    }

}
