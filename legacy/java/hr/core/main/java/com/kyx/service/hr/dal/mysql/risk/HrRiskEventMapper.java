package com.kyx.service.hr.dal.mysql.risk;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.risk.vo.HrRiskEventPageReqVO;
import com.kyx.service.hr.dal.dataobject.risk.HrRiskEventDO;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

@Mapper
public interface HrRiskEventMapper extends BaseMapperX<HrRiskEventDO> {

    @Select({"<script>",
            "SELECT COALESCE(e.dept_id, 0) AS deptId,",
            "       COUNT(1) AS openRiskCount,",
            "       COUNT(CASE WHEN r.severity = 'HIGH' THEN 1 END) AS highRiskCount,",
            "       COUNT(CASE WHEN r.severity = 'MEDIUM' THEN 1 END) AS mediumRiskCount",
            "FROM hr_risk_event r",
            "LEFT JOIN (",
            "    SELECT ee.profile_id, ee.dept_id",
            "    FROM hr_employee_entry ee",
            "    INNER JOIN (",
            "        SELECT profile_id, MAX(id) AS id",
            "        FROM hr_employee_entry",
            "        WHERE deleted = 0",
            "          AND profile_id IS NOT NULL",
            "          AND work_status IN (#{probationStatus}, #{activeStatus})",
            "        GROUP BY profile_id",
            "    ) latest ON latest.id = ee.id",
            "    WHERE ee.deleted = 0",
            ") e ON e.profile_id = r.profile_id",
            "WHERE r.deleted = 0",
            "  AND r.status IN ('OPEN', 'PROCESSING')",
            "GROUP BY COALESCE(e.dept_id, 0)",
            "</script>"})
    List<DeptRiskCount> selectActiveDeptRiskCounts(@Param("probationStatus") Integer probationStatus,
                                                   @Param("activeStatus") Integer activeStatus);

    default PageResult<HrRiskEventDO> selectPage(HrRiskEventPageReqVO reqVO) {
        LambdaQueryWrapperX<HrRiskEventDO> wrapper = buildQuery(reqVO);
        wrapper.orderByAsc(HrRiskEventDO::getStatus)
                .orderByAsc(HrRiskEventDO::getDueTime)
                .orderByDesc(HrRiskEventDO::getId);
        return selectPage(reqVO, wrapper);
    }

    default LambdaQueryWrapperX<HrRiskEventDO> buildQuery(HrRiskEventPageReqVO reqVO) {
        LambdaQueryWrapperX<HrRiskEventDO> wrapper = new LambdaQueryWrapperX<HrRiskEventDO>()
                .eqIfPresent(HrRiskEventDO::getId, reqVO.getId())
                .eqIfPresent(HrRiskEventDO::getSeverity, reqVO.getSeverity())
                .eqIfPresent(HrRiskEventDO::getStatus, reqVO.getStatus())
                .eqIfPresent(HrRiskEventDO::getSourceType, reqVO.getSourceType())
                .eqIfPresent(HrRiskEventDO::getIssueType, reqVO.getIssueType())
                .eqIfPresent(HrRiskEventDO::getProfileId, reqVO.getProfileId())
                .inIfPresent(HrRiskEventDO::getProfileId, reqVO.getProfileIds())
                .eqIfPresent(HrRiskEventDO::getOwnerUserId, reqVO.getOwnerUserId())
                .geIfPresent(HrRiskEventDO::getDueTime, reqVO.getDueTimeStart())
                .leIfPresent(HrRiskEventDO::getDueTime, reqVO.getDueTimeEnd());
        if (StringUtils.hasText(reqVO.getKeyword())) {
            String keyword = reqVO.getKeyword().trim();
            wrapper.and(query -> query.like(HrRiskEventDO::getTitle, keyword)
                    .or()
                    .like(HrRiskEventDO::getDescription, keyword)
                    .or()
                    .like(HrRiskEventDO::getAction, keyword)
                    .or()
                    .like(HrRiskEventDO::getHandleResult, keyword)
                    .or()
                    .like(HrRiskEventDO::getRemark, keyword));
        }
        return wrapper;
    }

    default HrRiskEventDO selectBySource(String sourceType, String sourceKey) {
        return selectOne(new LambdaQueryWrapperX<HrRiskEventDO>()
                .eq(HrRiskEventDO::getSourceType, sourceType)
                .eq(HrRiskEventDO::getSourceKey, sourceKey)
                .last("LIMIT 1"));
    }

    default List<HrRiskEventDO> selectActiveList(Integer limit) {
        LambdaQueryWrapperX<HrRiskEventDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.in(HrRiskEventDO::getStatus, Arrays.asList("OPEN", "PROCESSING"))
                .orderByAsc(HrRiskEventDO::getDueTime)
                .orderByDesc(HrRiskEventDO::getId);
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        return selectList(wrapper);
    }

    default List<HrRiskEventDO> selectActiveGeneratedList(Integer limit) {
        LambdaQueryWrapperX<HrRiskEventDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(HrRiskEventDO::getGeneratedFlag, true)
                .in(HrRiskEventDO::getStatus, Arrays.asList("OPEN", "PROCESSING"))
                .orderByAsc(HrRiskEventDO::getDueTime)
                .orderByDesc(HrRiskEventDO::getId);
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        return selectList(wrapper);
    }

    default List<HrRiskEventDO> selectTodoSourceList(Integer limit) {
        LambdaQueryWrapperX<HrRiskEventDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.in(HrRiskEventDO::getStatus, Arrays.asList("OPEN", "PROCESSING"))
                .in(HrRiskEventDO::getSeverity, Arrays.asList("HIGH", "MEDIUM"))
                .orderByAsc(HrRiskEventDO::getDueTime)
                .orderByDesc(HrRiskEventDO::getId);
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        return selectList(wrapper);
    }

    @Data
    class DeptRiskCount {

        private Long deptId;

        private Long openRiskCount;

        private Long highRiskCount;

        private Long mediumRiskCount;
    }

}
