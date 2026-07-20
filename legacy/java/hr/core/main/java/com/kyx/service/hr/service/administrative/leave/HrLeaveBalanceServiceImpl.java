package com.kyx.service.hr.service.administrative.leave;

import com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.security.core.service.SecurityFrameworkService;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.kyx.service.hr.controller.admin.administrative.leave.vo.HrLeaveBalanceAdjustReqVO;
import com.kyx.service.hr.controller.admin.administrative.leave.vo.HrLeaveBalancePageReqVO;
import com.kyx.service.hr.controller.admin.administrative.leave.vo.HrLeaveBalanceRecordPageReqVO;
import com.kyx.service.hr.controller.admin.administrative.leave.vo.HrLeaveBalanceRecordRespVO;
import com.kyx.service.hr.controller.admin.administrative.leave.vo.HrLeaveBalanceRespVO;
import com.kyx.service.hr.controller.admin.administrative.leave.vo.HrLeaveTypeRespVO;
import com.kyx.service.hr.controller.admin.administrative.leave.vo.HrLeaveTypeSaveReqVO;
import com.kyx.service.hr.dal.dataobject.administrative.HrAdministrativeLeaveDO;
import com.kyx.service.hr.dal.dataobject.administrative.HrLeaveBalanceDO;
import com.kyx.service.hr.dal.dataobject.administrative.HrLeaveTypeDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;
import com.kyx.service.hr.dal.mysql.administrative.HrLeaveBalanceMapper;
import com.kyx.service.hr.dal.mysql.administrative.HrLeaveTypeMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 假期类型与余额 Service 实现
 */
@Service
@Validated
@Slf4j
public class HrLeaveBalanceServiceImpl implements HrLeaveBalanceService {

    private static final Integer STATUS_RUNNING = 1;
    private static final Integer STATUS_APPROVE = 2;
    private static final String PERMISSION_QUERY_ALL = "hr:administrative-leave:query-all";
    private static final String PERMISSION_MANAGE_LEAVE_TYPE = "hr:leave-type:manage";
    private static final String CHANGE_ADJUST = "ADJUST";
    private static final String CHANGE_LEAVE_STATUS = "LEAVE_STATUS";
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @Resource
    private HrLeaveTypeMapper leaveTypeMapper;
    @Resource
    private HrLeaveBalanceMapper leaveBalanceMapper;
    @Resource
    private HrLeaveBalanceRecordMapper leaveBalanceRecordMapper;
    @Resource
    private EmployeeProfileMapper employeeProfileMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private SecurityFrameworkService securityFrameworkService;

    @Override
    public List<HrLeaveTypeRespVO> listLeaveTypes() {
        List<HrLeaveTypeDO> types = canManageLeaveTypes()
                ? leaveTypeMapper.selectAllList()
                : leaveTypeMapper.selectEnabledList();
        return BeanUtils.toBean(types, HrLeaveTypeRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveLeaveType(HrLeaveTypeSaveReqVO reqVO) {
        HrLeaveTypeDO type = BeanUtils.toBean(reqVO, HrLeaveTypeDO.class);
        type.setTypeCode(normalizeTypeCode(type.getTypeCode()));
        if (!StringUtils.hasText(type.getTypeCode())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "假期类型编码不能为空");
        }
        if (!StringUtils.hasText(type.getTypeName())) {
            type.setTypeName(type.getTypeCode());
        }
        if (type.getBalanceEnabled() == null) {
            type.setBalanceEnabled(false);
        }
        if (type.getPaid() == null) {
            type.setPaid(true);
        }
        if (type.getAttachmentRequired() == null) {
            type.setAttachmentRequired(false);
        }
        if (!StringUtils.hasText(type.getMinUnit())) {
            type.setMinUnit("HOUR");
        }
        if (type.getAnnualDefaultAmount() == null) {
            type.setAnnualDefaultAmount(ZERO);
        }
        if (type.getStatus() == null) {
            type.setStatus(0);
        }
        if (type.getId() == null) {
            leaveTypeMapper.insert(type);
        } else {
            leaveTypeMapper.updateById(type);
        }
        return type.getId();
    }

    @Override
    public PageResult<HrLeaveBalanceRespVO> getBalancePage(HrLeaveBalancePageReqVO pageReqVO) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            return new PageResult<>(new ArrayList<>(), 0L);
        }
        if (!canQueryAllLeaves()) {
            pageReqVO.setUserId(loginUserId);
            pageReqVO.setProfileId(null);
        }
        PageResult<HrLeaveBalanceDO> pageResult = leaveBalanceMapper.selectPage(pageReqVO);
        List<HrLeaveBalanceRespVO> respList = BeanUtils.toBean(pageResult.getList(), HrLeaveBalanceRespVO.class);
        fillBalanceNames(respList);
        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    public PageResult<HrLeaveBalanceRecordRespVO> getBalanceRecordPage(HrLeaveBalanceRecordPageReqVO pageReqVO) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            return new PageResult<>(new ArrayList<>(), 0L);
        }
        if (!canQueryAllLeaves()) {
            pageReqVO.setUserId(loginUserId);
            pageReqVO.setProfileId(null);
        }
        if (StringUtils.hasText(pageReqVO.getLeaveTypeCode())) {
            pageReqVO.setLeaveTypeCode(normalizeTypeCode(pageReqVO.getLeaveTypeCode()));
        }
        if (StringUtils.hasText(pageReqVO.getChangeType())) {
            pageReqVO.setChangeType(pageReqVO.getChangeType().trim().toUpperCase());
        }
        PageResult<HrLeaveBalanceRecordDO> pageResult = leaveBalanceRecordMapper.selectPage(pageReqVO);
        List<HrLeaveBalanceRecordRespVO> respList = BeanUtils.toBean(pageResult.getList(), HrLeaveBalanceRecordRespVO.class);
        fillRecordNames(respList);
        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    public List<HrLeaveBalanceRespVO> getMyBalances(Integer year) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            return new ArrayList<>();
        }
        List<HrLeaveBalanceDO> balances = leaveBalanceMapper.selectListByUserYear(
                loginUserId, year == null ? LocalDate.now().getYear() : year);
        List<HrLeaveBalanceRespVO> respList = BeanUtils.toBean(balances, HrLeaveBalanceRespVO.class);
        fillBalanceNames(respList);
        return respList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean adjustBalance(HrLeaveBalanceAdjustReqVO reqVO) {
        EmployeeProfileDO profile = resolveProfile(reqVO.getProfileId(), reqVO.getUserId());
        HrLeaveTypeDO type = loadBalanceEnabledType(reqVO.getLeaveTypeCode());
        HrLeaveBalanceDO balance = getOrCreateBalance(profile, type,
                reqVO.getYear() == null ? LocalDate.now().getYear() : reqVO.getYear());
        BalanceSnapshot before = BalanceSnapshot.from(balance);
        BigDecimal amount = reqVO.getAmount() == null ? ZERO : reqVO.getAmount();
        balance.setTotalAmount(defaultZero(balance.getTotalAmount()).add(amount));
        balance.setRemainAmount(defaultZero(balance.getRemainAmount()).add(amount));
        balance.setRemark(reqVO.getRemark());
        ensureNotNegative(balance);
        leaveBalanceMapper.updateById(balance);
        recordBalanceChange(balance, before, amount, CHANGE_ADJUST, null, null, reqVO.getRemark());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleLeaveStatusChange(HrAdministrativeLeaveDO leave, Integer oldStatus, Integer newStatus) {
        if (leave == null || leave.getUserId() == null || leave.getStartTime() == null || leave.getDuration() == null
                || oldStatus != null && oldStatus.equals(newStatus)) {
            return;
        }
        HrLeaveTypeDO type = leaveTypeMapper.selectByCode(normalizeTypeCode(leave.getLeaveType()));
        if (type == null || !Boolean.TRUE.equals(type.getBalanceEnabled())) {
            return;
        }
        EmployeeProfileDO profile = employeeProfileMapper.selectByUserId(leave.getUserId());
        if (profile == null) {
            return;
        }
        HrLeaveBalanceDO balance = getOrCreateBalance(profile, type, leave.getStartTime().getYear());
        BalanceSnapshot before = BalanceSnapshot.from(balance);
        BigDecimal amount = defaultZero(leave.getDuration());

        if (STATUS_RUNNING.equals(oldStatus)) {
            balance.setFrozenAmount(defaultZero(balance.getFrozenAmount()).subtract(amount));
            balance.setRemainAmount(defaultZero(balance.getRemainAmount()).add(amount));
        } else if (STATUS_APPROVE.equals(oldStatus)) {
            balance.setUsedAmount(defaultZero(balance.getUsedAmount()).subtract(amount));
            balance.setRemainAmount(defaultZero(balance.getRemainAmount()).add(amount));
        }

        if (STATUS_RUNNING.equals(newStatus)) {
            ensureEnough(balance, amount);
            balance.setFrozenAmount(defaultZero(balance.getFrozenAmount()).add(amount));
            balance.setRemainAmount(defaultZero(balance.getRemainAmount()).subtract(amount));
        } else if (STATUS_APPROVE.equals(newStatus)) {
            ensureEnough(balance, amount);
            balance.setUsedAmount(defaultZero(balance.getUsedAmount()).add(amount));
            balance.setRemainAmount(defaultZero(balance.getRemainAmount()).subtract(amount));
        }
        normalizeNonNegative(balance);
        leaveBalanceMapper.updateById(balance);
        recordBalanceChange(balance, before, calculateNetRemainChange(before, balance), CHANGE_LEAVE_STATUS,
                "ADMINISTRATIVE_LEAVE", leave.getId(), "请假状态变更：" + oldStatus + " -> " + newStatus);
    }

    private EmployeeProfileDO resolveProfile(Long profileId, Long userId) {
        EmployeeProfileDO profile = profileId == null ? null : employeeProfileMapper.selectById(profileId);
        if (profile == null && userId != null) {
            profile = employeeProfileMapper.selectByUserId(userId);
        }
        if (profile == null || profile.getUserId() == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "员工档案不存在");
        }
        return profile;
    }

    private HrLeaveTypeDO loadBalanceEnabledType(String leaveTypeCode) {
        HrLeaveTypeDO type = leaveTypeMapper.selectByCode(normalizeTypeCode(leaveTypeCode));
        if (type == null || !Boolean.TRUE.equals(type.getBalanceEnabled())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "该假期类型未启用余额");
        }
        return type;
    }

    private HrLeaveBalanceDO getOrCreateBalance(EmployeeProfileDO profile, HrLeaveTypeDO type, Integer year) {
        HrLeaveBalanceDO balance = leaveBalanceMapper.selectByUserYearType(profile.getUserId(), year, type.getTypeCode());
        if (balance != null) {
            return balance;
        }
        balance = new HrLeaveBalanceDO();
        balance.setProfileId(profile.getId());
        balance.setUserId(profile.getUserId());
        balance.setLeaveTypeId(type.getId());
        balance.setLeaveTypeCode(type.getTypeCode());
        balance.setYear(year);
        balance.setTotalAmount(defaultZero(type.getAnnualDefaultAmount()));
        balance.setUsedAmount(ZERO);
        balance.setFrozenAmount(ZERO);
        balance.setRemainAmount(defaultZero(type.getAnnualDefaultAmount()));
        leaveBalanceMapper.insert(balance);
        return balance;
    }

    private void fillBalanceNames(List<HrLeaveBalanceRespVO> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Map<String, HrLeaveTypeDO> typeMap = new HashMap<>();
        for (HrLeaveTypeDO type : leaveTypeMapper.selectEnabledList()) {
            typeMap.put(type.getTypeCode(), type);
        }
        Set<Long> userIds = new HashSet<>();
        Set<Long> profileIds = new HashSet<>();
        for (HrLeaveBalanceRespVO row : rows) {
            if (row.getUserId() != null) {
                userIds.add(row.getUserId());
            }
            if (row.getProfileId() != null) {
                profileIds.add(row.getProfileId());
            }
        }
        Map<Long, AdminUserRespDTO> userMap = loadUserMapSafe(userIds);
        Map<Long, EmployeeProfileDO> profileMap = loadProfileMapSafe(profileIds);
        for (HrLeaveBalanceRespVO row : rows) {
            HrLeaveTypeDO type = typeMap.get(row.getLeaveTypeCode());
            if (type != null) {
                row.setLeaveTypeName(type.getTypeName());
            }
            AdminUserRespDTO user = userMap.get(row.getUserId());
            if (user != null) {
                row.setUserName(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
            }
            EmployeeProfileDO profile = profileMap.get(row.getProfileId());
            if (profile != null) {
                row.setProfileName(profile.getName());
            }
        }
    }

    private void fillRecordNames(List<HrLeaveBalanceRecordRespVO> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Map<String, HrLeaveTypeDO> typeMap = new HashMap<>();
        for (HrLeaveTypeDO type : leaveTypeMapper.selectEnabledList()) {
            typeMap.put(type.getTypeCode(), type);
        }
        Set<Long> userIds = new HashSet<>();
        Set<Long> profileIds = new HashSet<>();
        for (HrLeaveBalanceRecordRespVO row : rows) {
            if (row.getUserId() != null) {
                userIds.add(row.getUserId());
            }
            if (row.getOperatorId() != null) {
                userIds.add(row.getOperatorId());
            }
            if (row.getProfileId() != null) {
                profileIds.add(row.getProfileId());
            }
        }
        Map<Long, AdminUserRespDTO> userMap = loadUserMapSafe(userIds);
        Map<Long, EmployeeProfileDO> profileMap = loadProfileMapSafe(profileIds);
        for (HrLeaveBalanceRecordRespVO row : rows) {
            HrLeaveTypeDO type = typeMap.get(row.getLeaveTypeCode());
            if (type != null) {
                row.setLeaveTypeName(type.getTypeName());
            }
            AdminUserRespDTO user = userMap.get(row.getUserId());
            if (user != null) {
                row.setUserName(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
            }
            AdminUserRespDTO operator = userMap.get(row.getOperatorId());
            if (operator != null) {
                row.setOperatorName(StringUtils.hasText(operator.getNickname()) ? operator.getNickname() : operator.getUsername());
            }
            EmployeeProfileDO profile = profileMap.get(row.getProfileId());
            if (profile != null) {
                row.setProfileName(profile.getName());
            }
        }
    }

    private void recordBalanceChange(HrLeaveBalanceDO balance, BalanceSnapshot before, BigDecimal changeAmount,
                                     String changeType, String sourceType, Long sourceId, String remark) {
        if (balance == null || before == null || !before.changed(balance)) {
            return;
        }
        HrLeaveBalanceRecordDO record = new HrLeaveBalanceRecordDO();
        record.setBalanceId(balance.getId());
        record.setProfileId(balance.getProfileId());
        record.setUserId(balance.getUserId());
        record.setLeaveTypeId(balance.getLeaveTypeId());
        record.setLeaveTypeCode(balance.getLeaveTypeCode());
        record.setYear(balance.getYear());
        record.setChangeType(changeType);
        record.setChangeAmount(defaultZero(changeAmount));
        record.setBeforeTotalAmount(before.totalAmount);
        record.setAfterTotalAmount(defaultZero(balance.getTotalAmount()));
        record.setBeforeUsedAmount(before.usedAmount);
        record.setAfterUsedAmount(defaultZero(balance.getUsedAmount()));
        record.setBeforeFrozenAmount(before.frozenAmount);
        record.setAfterFrozenAmount(defaultZero(balance.getFrozenAmount()));
        record.setBeforeRemainAmount(before.remainAmount);
        record.setAfterRemainAmount(defaultZero(balance.getRemainAmount()));
        record.setOperatorId(SecurityFrameworkUtils.getLoginUserId());
        record.setSourceType(sourceType);
        record.setSourceId(sourceId);
        record.setRemark(remark);
        leaveBalanceRecordMapper.insert(record);
    }

    private BigDecimal calculateNetRemainChange(BalanceSnapshot before, HrLeaveBalanceDO balance) {
        return defaultZero(balance.getRemainAmount()).subtract(before.remainAmount);
    }

    private Map<Long, AdminUserRespDTO> loadUserMapSafe(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return adminUserApi.getUserMap(userIds);
        } catch (Exception ex) {
            log.warn("Failed to load users for leave balance: {}", ex.getMessage());
            return new HashMap<>();
        }
    }

    private Map<Long, EmployeeProfileDO> loadProfileMapSafe(Set<Long> profileIds) {
        Map<Long, EmployeeProfileDO> profileMap = new HashMap<>();
        if (profileIds == null || profileIds.isEmpty()) {
            return profileMap;
        }
        List<EmployeeProfileDO> profiles = employeeProfileMapper.selectBatchIds(profileIds);
        if (profiles == null) {
            return profileMap;
        }
        for (EmployeeProfileDO profile : profiles) {
            if (profile != null && profile.getId() != null) {
                profileMap.put(profile.getId(), profile);
            }
        }
        return profileMap;
    }

    private boolean canQueryAllLeaves() {
        try {
            return securityFrameworkService.hasPermission(PERMISSION_QUERY_ALL);
        } catch (Exception ex) {
            log.warn("check leave query-all permission failed: {}", ex.getMessage());
            return false;
        }
    }

    private boolean canManageLeaveTypes() {
        try {
            return securityFrameworkService.hasPermission(PERMISSION_MANAGE_LEAVE_TYPE);
        } catch (Exception ex) {
            log.warn("check leave type manage permission failed: {}", ex.getMessage());
            return false;
        }
    }

    private String normalizeTypeCode(String typeCode) {
        return typeCode == null ? null : typeCode.trim().toLowerCase();
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private void ensureEnough(HrLeaveBalanceDO balance, BigDecimal amount) {
        if (defaultZero(balance.getRemainAmount()).compareTo(defaultZero(amount)) < 0) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "假期余额不足");
        }
    }

    private void normalizeNonNegative(HrLeaveBalanceDO balance) {
        if (defaultZero(balance.getFrozenAmount()).compareTo(ZERO) < 0) {
            balance.setFrozenAmount(ZERO);
        }
        if (defaultZero(balance.getUsedAmount()).compareTo(ZERO) < 0) {
            balance.setUsedAmount(ZERO);
        }
        if (defaultZero(balance.getRemainAmount()).compareTo(ZERO) < 0) {
            balance.setRemainAmount(ZERO);
        }
    }

    private void ensureNotNegative(HrLeaveBalanceDO balance) {
        if (defaultZero(balance.getTotalAmount()).compareTo(ZERO) < 0
                || defaultZero(balance.getRemainAmount()).compareTo(ZERO) < 0) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "假期额度不能调整为负数");
        }
    }

    private static class BalanceSnapshot {
        private final BigDecimal totalAmount;
        private final BigDecimal usedAmount;
        private final BigDecimal frozenAmount;
        private final BigDecimal remainAmount;

        private BalanceSnapshot(BigDecimal totalAmount, BigDecimal usedAmount,
                                BigDecimal frozenAmount, BigDecimal remainAmount) {
            this.totalAmount = totalAmount == null ? ZERO : totalAmount;
            this.usedAmount = usedAmount == null ? ZERO : usedAmount;
            this.frozenAmount = frozenAmount == null ? ZERO : frozenAmount;
            this.remainAmount = remainAmount == null ? ZERO : remainAmount;
        }

        private static BalanceSnapshot from(HrLeaveBalanceDO balance) {
            return new BalanceSnapshot(balance.getTotalAmount(), balance.getUsedAmount(),
                    balance.getFrozenAmount(), balance.getRemainAmount());
        }

        private boolean changed(HrLeaveBalanceDO balance) {
            return totalAmount.compareTo(balance.getTotalAmount() == null ? ZERO : balance.getTotalAmount()) != 0
                    || usedAmount.compareTo(balance.getUsedAmount() == null ? ZERO : balance.getUsedAmount()) != 0
                    || frozenAmount.compareTo(balance.getFrozenAmount() == null ? ZERO : balance.getFrozenAmount()) != 0
                    || remainAmount.compareTo(balance.getRemainAmount() == null ? ZERO : balance.getRemainAmount()) != 0;
        }
    }

}
