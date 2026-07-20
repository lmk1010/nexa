package com.kyx.service.hr.service.administrative.trip;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.administrative.trip.vo.HrTripPageReqVO;
import com.kyx.service.hr.controller.admin.administrative.trip.vo.HrTripSaveReqVO;
import com.kyx.service.hr.dal.dataobject.administrative.HrAdministrativeTripDO;

import javax.validation.Valid;

/**
 * 出差申请 Service 接口
 *
 * @author MK
 */
public interface HrAdministrativeTripService {

    /**
     * 创建出差申请
     *
     * @param userId      用户ID
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createTrip(Long userId, @Valid HrTripSaveReqVO createReqVO);

    /**
     * 更新出差申请
     *
     * @param updateReqVO 更新信息
     */
    void updateTrip(@Valid HrTripSaveReqVO updateReqVO);

    /**
     * 更新出差申请状态
     *
     * @param id     编号
     * @param status 状态
     */
    void updateTripStatus(Long id, Integer status);

    /**
     * 获得出差申请
     *
     * @param id 编号
     * @return 出差申请
     */
    HrAdministrativeTripDO getTrip(Long id);

    /**
     * 获得出差申请分页
     *
     * @param pageReqVO 分页查询
     * @return 出差申请分页
     */
    PageResult<HrAdministrativeTripDO> getTripPage(HrTripPageReqVO pageReqVO);
}
