package com.kyx.service.hr.service.attendance;

import com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceGroupPageReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceGroupRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceGroupSaveReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceShiftRuleRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceShiftRuleSaveReqVO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceGroupDO;
import com.kyx.service.hr.dal.dataobject.attendance.AttendanceShiftRuleDO;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceGroupMapper;
import com.kyx.service.hr.dal.mysql.attendance.AttendanceShiftRuleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 考勤规则 Service 实现
 */
@Service
@Validated
public class AttendanceRuleServiceImpl implements AttendanceRuleService {

    private static final String SCOPE_ALL = "ALL";

    @Resource
    private AttendanceShiftRuleMapper attendanceShiftRuleMapper;
    @Resource
    private AttendanceGroupMapper attendanceGroupMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveShift(AttendanceShiftRuleSaveReqVO reqVO) {
        validateShiftTime(reqVO);
        AttendanceShiftRuleDO shiftRule = BeanUtils.toBean(reqVO, AttendanceShiftRuleDO.class);
        shiftRule.setStatus(reqVO.getStatus() == null ? 0 : reqVO.getStatus());
        shiftRule.setLateGraceMinutes(defaultMinutes(reqVO.getLateGraceMinutes()));
        shiftRule.setEarlyLeaveGraceMinutes(defaultMinutes(reqVO.getEarlyLeaveGraceMinutes()));
        shiftRule.setWorkHours(resolveWorkHours(reqVO));
        shiftRule.setDefaultFlag(Boolean.TRUE.equals(reqVO.getDefaultFlag()));
        if (Boolean.TRUE.equals(shiftRule.getDefaultFlag())) {
            attendanceShiftRuleMapper.clearDefault(reqVO.getId());
        }
        if (reqVO.getId() == null) {
            attendanceShiftRuleMapper.insert(shiftRule);
        } else {
            attendanceShiftRuleMapper.updateById(shiftRule);
        }
        return shiftRule.getId();
    }

    @Override
    public List<AttendanceShiftRuleRespVO> getShiftList() {
        return BeanUtils.toBean(attendanceShiftRuleMapper.selectList(
                new com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX<AttendanceShiftRuleDO>()
                        .orderByDesc(AttendanceShiftRuleDO::getDefaultFlag)
                        .orderByAsc(AttendanceShiftRuleDO::getId)), AttendanceShiftRuleRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveGroup(AttendanceGroupSaveReqVO reqVO) {
        if (attendanceShiftRuleMapper.selectById(reqVO.getShiftRuleId()) == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "班次规则不存在");
        }
        AttendanceGroupDO group = BeanUtils.toBean(reqVO, AttendanceGroupDO.class);
        group.setScopeType(normalizeScopeType(reqVO.getScopeType()));
        group.setScopeJson(resolveScopeJson(group.getScopeType(), reqVO.getScopeJson()));
        group.setStatus(reqVO.getStatus() == null ? 0 : reqVO.getStatus());
        if (reqVO.getId() == null) {
            attendanceGroupMapper.insert(group);
        } else {
            attendanceGroupMapper.updateById(group);
        }
        return group.getId();
    }

    @Override
    public PageResult<AttendanceGroupRespVO> getGroupPage(AttendanceGroupPageReqVO pageReqVO) {
        PageResult<AttendanceGroupDO> pageResult = attendanceGroupMapper.selectPage(pageReqVO);
        List<AttendanceGroupDO> rows = pageResult.getList();
        List<AttendanceGroupRespVO> respList = BeanUtils.toBean(rows, AttendanceGroupRespVO.class);
        fillShiftName(respList);
        return new PageResult<>(respList == null ? new ArrayList<>() : respList, pageResult.getTotal());
    }

    private void validateShiftTime(AttendanceShiftRuleSaveReqVO reqVO) {
        if (!reqVO.getEndTime().isAfter(reqVO.getStartTime())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "下班时间必须晚于上班时间");
        }
        if ((reqVO.getRestStartTime() == null) != (reqVO.getRestEndTime() == null)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "休息开始和结束时间需要同时填写");
        }
        if (reqVO.getRestStartTime() != null
                && (!reqVO.getRestStartTime().isAfter(reqVO.getStartTime())
                || !reqVO.getRestEndTime().isBefore(reqVO.getEndTime())
                || !reqVO.getRestEndTime().isAfter(reqVO.getRestStartTime()))) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "休息时间必须在班次时间内");
        }
    }

    private BigDecimal resolveWorkHours(AttendanceShiftRuleSaveReqVO reqVO) {
        if (reqVO.getWorkHours() != null && reqVO.getWorkHours().compareTo(BigDecimal.ZERO) > 0) {
            return reqVO.getWorkHours();
        }
        long minutes = Duration.between(reqVO.getStartTime(), reqVO.getEndTime()).toMinutes();
        if (reqVO.getRestStartTime() != null && reqVO.getRestEndTime() != null) {
            minutes -= Duration.between(reqVO.getRestStartTime(), reqVO.getRestEndTime()).toMinutes();
        }
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private Integer defaultMinutes(Integer value) {
        return value == null || value < 0 ? 0 : value;
    }

    private String normalizeScopeType(String scopeType) {
        if (!StringUtils.hasText(scopeType)) {
            return SCOPE_ALL;
        }
        return scopeType.trim().toUpperCase();
    }

    private String resolveScopeJson(String scopeType, String scopeJson) {
        if (SCOPE_ALL.equals(scopeType)) {
            return "{\"type\":\"ALL\"}";
        }
        return StringUtils.hasText(scopeJson) ? scopeJson.trim() : "{}";
    }

    private void fillShiftName(List<AttendanceGroupRespVO> respList) {
        if (respList == null || respList.isEmpty()) {
            return;
        }
        List<AttendanceShiftRuleDO> shifts = attendanceShiftRuleMapper.selectList();
        Map<Long, String> shiftNameMap = new HashMap<>();
        for (AttendanceShiftRuleDO shift : shifts) {
            if (shift.getId() != null) {
                shiftNameMap.put(shift.getId(), shift.getShiftName());
            }
        }
        for (AttendanceGroupRespVO respVO : respList) {
            respVO.setShiftName(shiftNameMap.get(respVO.getShiftRuleId()));
        }
    }

}
