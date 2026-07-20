package com.kyx.service.hr.dal.mysql.employee;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeMaterialPageReqVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeMaterialDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface EmployeeMaterialMapper extends BaseMapperX<EmployeeMaterialDO> {

    default PageResult<EmployeeMaterialDO> selectPage(EmployeeMaterialPageReqVO reqVO) {
        LambdaQueryWrapperX<EmployeeMaterialDO> wrapper = new LambdaQueryWrapperX<EmployeeMaterialDO>()
                .eqIfPresent(EmployeeMaterialDO::getId, reqVO.getId())
                .eqIfPresent(EmployeeMaterialDO::getProfileId, reqVO.getProfileId())
                .inIfPresent(EmployeeMaterialDO::getProfileId, reqVO.getProfileIds())
                .eqIfPresent(EmployeeMaterialDO::getUserId, reqVO.getUserId())
                .eqIfPresent(EmployeeMaterialDO::getCategory, reqVO.getCategory())
                .eqIfPresent(EmployeeMaterialDO::getMaterialType, reqVO.getMaterialType())
                .likeIfPresent(EmployeeMaterialDO::getMaterialName, reqVO.getMaterialName())
                .geIfPresent(EmployeeMaterialDO::getExpireDate, reqVO.getExpireDateStart())
                .leIfPresent(EmployeeMaterialDO::getExpireDate, reqVO.getExpireDateEnd());
        if (StringUtils.hasText(reqVO.getKeyword())) {
            String keyword = reqVO.getKeyword().trim();
            Long id = parseLong(keyword);
            wrapper.and(item -> {
                if (id != null) {
                    item.eq(EmployeeMaterialDO::getId, id).or();
                }
                item.like(EmployeeMaterialDO::getMaterialName, keyword)
                        .or()
                        .like(EmployeeMaterialDO::getMaterialType, keyword)
                        .or()
                        .like(EmployeeMaterialDO::getFileName, keyword)
                        .or()
                        .like(EmployeeMaterialDO::getRemark, keyword);
            });
        }
        if ("EXPIRED".equals(reqVO.getStatus())) {
            wrapper.and(w -> w.eq(EmployeeMaterialDO::getStatus, "EXPIRED")
                    .or()
                    .eq(EmployeeMaterialDO::getStatus, "ACTIVE")
                    .lt(EmployeeMaterialDO::getExpireDate, LocalDate.now()));
        } else {
            wrapper.eqIfPresent(EmployeeMaterialDO::getStatus, reqVO.getStatus());
        }
        wrapper.orderByAsc(EmployeeMaterialDO::getExpireDate)
                .orderByDesc(EmployeeMaterialDO::getId);
        return selectPage(reqVO, wrapper);
    }

    static Long parseLong(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    default EmployeeMaterialDO selectBySource(String sourceType, Long sourceId) {
        return selectOne(new LambdaQueryWrapperX<EmployeeMaterialDO>()
                .eq(EmployeeMaterialDO::getSourceType, sourceType)
                .eq(EmployeeMaterialDO::getSourceId, sourceId)
                .last("LIMIT 1"));
    }

    default List<EmployeeMaterialDO> selectExpiringActive(LocalDate startDate, LocalDate endDate, Integer limit) {
        LambdaQueryWrapperX<EmployeeMaterialDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.in(EmployeeMaterialDO::getStatus, "ACTIVE", "MISSING")
                .isNotNull(EmployeeMaterialDO::getExpireDate);
        if (startDate != null) {
            wrapper.ge(EmployeeMaterialDO::getExpireDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(EmployeeMaterialDO::getExpireDate, endDate);
        }
        wrapper.orderByAsc(EmployeeMaterialDO::getExpireDate)
                .orderByDesc(EmployeeMaterialDO::getId);
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        return selectList(wrapper);
    }

}
