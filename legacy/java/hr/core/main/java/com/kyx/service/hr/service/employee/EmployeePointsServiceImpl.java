package com.kyx.service.hr.service.employee;

import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePointsAccountRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePointsAddReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePointsDeductReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeePointsRespVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeePointsAccountDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeePointsDO;
import com.kyx.service.hr.dal.mysql.employee.EmployeePointsAccountMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeePointsMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

import static com.kyx.service.hr.enums.ErrorCodeConstants.EMPLOYEE_POINTS_ACCOUNT_NOT_EXISTS;
import static com.kyx.service.hr.enums.ErrorCodeConstants.EMPLOYEE_POINTS_BALANCE_NOT_ENOUGH;
import static com.kyx.service.hr.enums.ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS;

/**
 * 员工积分 Service 实现类
 *
 * @author MK
 */
@Service
@Validated
public class EmployeePointsServiceImpl implements EmployeePointsService {

    @Resource
    private EmployeePointsMapper employeePointsMapper;

    @Resource
    private EmployeePointsAccountMapper employeePointsAccountMapper;

    @Resource
    private EmployeeProfileMapper employeeProfileMapper;

    @Override
    public List<EmployeePointsRespVO> getEmployeePointsList(Long profileId) {
        List<EmployeePointsDO> list = employeePointsMapper.selectListByProfileId(profileId);
        return BeanUtils.toBean(list, EmployeePointsRespVO.class);
    }

    @Override
    public EmployeePointsAccountRespVO getEmployeePointsAccount(Long profileId) {
        EmployeePointsAccountDO account = employeePointsAccountMapper.selectByProfileId(profileId);
        if (account == null) {
            EmployeePointsAccountRespVO empty = new EmployeePointsAccountRespVO();
            empty.setProfileId(profileId);
            empty.setTotalPoints(BigDecimal.ZERO);
            empty.setUsedPoints(BigDecimal.ZERO);
            empty.setExpiredPoints(BigDecimal.ZERO);
            empty.setBalance(BigDecimal.ZERO);
            return empty;
        }
        return BeanUtils.toBean(account, EmployeePointsAccountRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addEmployeePoints(Long operatorId, String operatorName, EmployeePointsAddReqVO reqVO) {
        validateProfileExists(reqVO.getProfileId());

        EmployeePointsAccountDO account = getOrCreateAccount(reqVO.getProfileId());
        BigDecimal currentTotal = safeBigDecimal(account.getTotalPoints());
        BigDecimal currentBalance = safeBigDecimal(account.getBalance());
        BigDecimal addPoints = BigDecimal.valueOf(reqVO.getPoints());

        account.setTotalPoints(currentTotal.add(addPoints));
        account.setBalance(currentBalance.add(addPoints));
        employeePointsAccountMapper.updateById(account);

        EmployeePointsDO points = BeanUtils.toBean(reqVO, EmployeePointsDO.class);
        points.setPointsType(1);
        points.setPoints(addPoints);
        points.setBalance(account.getBalance());
        points.setOperatorId(operatorId);
        points.setOperatorName(operatorName);
        employeePointsMapper.insert(points);
        return points.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long deductEmployeePoints(Long operatorId, String operatorName, EmployeePointsDeductReqVO reqVO) {
        validateProfileExists(reqVO.getProfileId());

        EmployeePointsAccountDO account = employeePointsAccountMapper.selectByProfileId(reqVO.getProfileId());
        if (account == null) {
            throw ServiceExceptionUtil.exception(EMPLOYEE_POINTS_ACCOUNT_NOT_EXISTS);
        }

        BigDecimal currentBalance = safeBigDecimal(account.getBalance());
        BigDecimal deductPoints = BigDecimal.valueOf(reqVO.getPoints());
        if (currentBalance.compareTo(deductPoints) < 0) {
            throw ServiceExceptionUtil.exception(EMPLOYEE_POINTS_BALANCE_NOT_ENOUGH);
        }

        BigDecimal currentUsed = safeBigDecimal(account.getUsedPoints());
        account.setUsedPoints(currentUsed.add(deductPoints));
        account.setBalance(currentBalance.subtract(deductPoints));
        employeePointsAccountMapper.updateById(account);

        EmployeePointsDO points = BeanUtils.toBean(reqVO, EmployeePointsDO.class);
        points.setPointsType(2);
        points.setPoints(deductPoints.negate());
        points.setBalance(account.getBalance());
        points.setOperatorId(operatorId);
        points.setOperatorName(operatorName);
        employeePointsMapper.insert(points);
        return points.getId();
    }

    private void validateProfileExists(Long profileId) {
        if (employeeProfileMapper.selectById(profileId) == null) {
            throw ServiceExceptionUtil.exception(EMPLOYEE_PROFILE_NOT_EXISTS);
        }
    }

    private EmployeePointsAccountDO getOrCreateAccount(Long profileId) {
        EmployeePointsAccountDO account = employeePointsAccountMapper.selectByProfileId(profileId);
        if (account != null) {
            return account;
        }
        account = new EmployeePointsAccountDO();
        account.setProfileId(profileId);
        account.setTotalPoints(BigDecimal.ZERO);
        account.setUsedPoints(BigDecimal.ZERO);
        account.setExpiredPoints(BigDecimal.ZERO);
        account.setBalance(BigDecimal.ZERO);
        employeePointsAccountMapper.insert(account);
        return account;
    }

    private BigDecimal safeBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

}
