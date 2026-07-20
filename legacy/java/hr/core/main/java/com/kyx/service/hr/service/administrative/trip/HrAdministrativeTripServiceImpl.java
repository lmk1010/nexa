package com.kyx.service.hr.service.administrative.trip;

import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.bpm.api.task.BpmProcessInstanceApi;
import com.kyx.service.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.kyx.service.hr.controller.admin.administrative.trip.vo.HrTripPageReqVO;
import com.kyx.service.hr.controller.admin.administrative.trip.vo.HrTripSaveReqVO;
import com.kyx.service.hr.dal.dataobject.administrative.HrAdministrativeTripDO;
import com.kyx.service.hr.dal.mysql.administrative.HrAdministrativeTripMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static com.kyx.service.hr.enums.ErrorCodeConstants.HR_TRIP_NOT_EXISTS;

/**
 * 出差申请 Service 实现
 *
 * @author MK
 */
@Service
@Validated
@Slf4j
public class HrAdministrativeTripServiceImpl implements HrAdministrativeTripService {

    /**
     * HR 出差流程定义 KEY
     */
    public static final String PROCESS_KEY = "hr_administrative_trip";

    private static final Integer STATUS_RUNNING = 1;
    private static final BigDecimal MINUTES_PER_DAY = BigDecimal.valueOf(60 * 24);

    @Resource
    private HrAdministrativeTripMapper tripMapper;
    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTrip(Long userId, HrTripSaveReqVO createReqVO) {
        HrAdministrativeTripDO trip = BeanUtils.toBean(createReqVO, HrAdministrativeTripDO.class)
                .setUserId(userId)
                .setStatus(STATUS_RUNNING);
        fillDefaultDuration(trip);
        tripMapper.insert(trip);

        Map<String, Object> variables = new HashMap<>();
        variables.put("tripType", trip.getTripType());
        variables.put("destinationCity", trip.getDestinationCity());
        variables.put("transportType", trip.getTransportType());
        variables.put("purpose", trip.getPurpose());
        variables.put("duration", trip.getDuration());
        variables.put("startTime", trip.getStartTime());
        variables.put("endTime", trip.getEndTime());
        variables.put("costEstimate", trip.getCostEstimate());

        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO()
                        .setProcessDefinitionKey(PROCESS_KEY)
                        .setBusinessKey(String.valueOf(trip.getId()))
                        .setVariables(variables)
                        .setStartUserSelectAssignees(createReqVO.getStartUserSelectAssignees()))
                .getCheckedData();

        tripMapper.updateById(new HrAdministrativeTripDO()
                .setId(trip.getId())
                .setProcessInstanceId(processInstanceId));
        return trip.getId();
    }

    @Override
    public void updateTrip(HrTripSaveReqVO updateReqVO) {
        if (updateReqVO.getId() == null) {
            throw ServiceExceptionUtil.exception(HR_TRIP_NOT_EXISTS);
        }
        validateTripExists(updateReqVO.getId());
        HrAdministrativeTripDO updateObj = BeanUtils.toBean(updateReqVO, HrAdministrativeTripDO.class);
        fillDefaultDuration(updateObj);
        tripMapper.updateById(updateObj);
    }

    @Override
    public void updateTripStatus(Long id, Integer status) {
        validateTripExists(id);
        tripMapper.updateById(new HrAdministrativeTripDO().setId(id).setStatus(status));
    }

    @Override
    public HrAdministrativeTripDO getTrip(Long id) {
        return tripMapper.selectById(id);
    }

    @Override
    public PageResult<HrAdministrativeTripDO> getTripPage(HrTripPageReqVO pageReqVO) {
        return tripMapper.selectPage(pageReqVO);
    }

    private void validateTripExists(Long id) {
        if (tripMapper.selectById(id) == null) {
            throw ServiceExceptionUtil.exception(HR_TRIP_NOT_EXISTS);
        }
    }

    private void fillDefaultDuration(HrAdministrativeTripDO trip) {
        if (trip.getDuration() != null || trip.getStartTime() == null || trip.getEndTime() == null) {
            return;
        }
        long minutes = Duration.between(trip.getStartTime(), trip.getEndTime()).toMinutes();
        if (minutes <= 0) {
            return;
        }
        BigDecimal days = BigDecimal.valueOf(minutes)
                .divide(MINUTES_PER_DAY, 2, RoundingMode.HALF_UP);
        trip.setDuration(days);
    }
}
