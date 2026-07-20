package com.kyx.service.hr.dal.mysql.employee;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeDocumentRequestPageReqVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeDocumentRequestDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Employee document request Mapper.
 */
@Mapper
public interface EmployeeDocumentRequestMapper extends BaseMapperX<EmployeeDocumentRequestDO> {

    default PageResult<EmployeeDocumentRequestDO> selectPage(EmployeeDocumentRequestPageReqVO reqVO) {
        LambdaQueryWrapperX<EmployeeDocumentRequestDO> wrapper = new LambdaQueryWrapperX<EmployeeDocumentRequestDO>()
                .eqIfPresent(EmployeeDocumentRequestDO::getId, reqVO.getId())
                .eqIfPresent(EmployeeDocumentRequestDO::getProfileId, reqVO.getProfileId())
                .eqIfPresent(EmployeeDocumentRequestDO::getUserId, reqVO.getUserId())
                .eqIfPresent(EmployeeDocumentRequestDO::getRequestType, reqVO.getRequestType())
                .eqIfPresent(EmployeeDocumentRequestDO::getStatus, reqVO.getStatus())
                .geIfPresent(EmployeeDocumentRequestDO::getExpectedDate, reqVO.getExpectedDateStart())
                .leIfPresent(EmployeeDocumentRequestDO::getExpectedDate, reqVO.getExpectedDateEnd());
        if (StringUtils.hasText(reqVO.getKeyword())) {
            String keyword = reqVO.getKeyword().trim();
            Long id = parseLong(keyword);
            wrapper.and(item -> {
                if (id != null) {
                    item.eq(EmployeeDocumentRequestDO::getId, id).or();
                }
                item.like(EmployeeDocumentRequestDO::getTitle, keyword)
                        .or()
                        .like(EmployeeDocumentRequestDO::getPurpose, keyword);
            });
        }
        wrapper.orderByDesc(EmployeeDocumentRequestDO::getId);
        return selectPage(reqVO, wrapper);
    }

    static Long parseLong(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    default List<EmployeeDocumentRequestDO> selectOpenList(Integer limit) {
        LambdaQueryWrapperX<EmployeeDocumentRequestDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.in(EmployeeDocumentRequestDO::getStatus, "PENDING", "PROCESSING")
                .orderByAsc(EmployeeDocumentRequestDO::getExpectedDate)
                .orderByDesc(EmployeeDocumentRequestDO::getId);
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        return selectList(wrapper);
    }

}
