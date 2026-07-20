package com.kyx.service.hr.service.administrative.leave;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.administrative.leave.vo.HrLeavePageReqVO;
import com.kyx.service.hr.controller.admin.administrative.leave.vo.HrLeaveSaveReqVO;
import com.kyx.service.hr.dal.dataobject.administrative.HrAdministrativeLeaveDO;

import javax.validation.Valid;

/**
 * 请假申请 Service 接口
 *
 * @author MK
 */
public interface HrAdministrativeLeaveService {

    /**
     * 创建请假申请
     *
     * @param userId      用户ID
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createLeave(Long userId, @Valid HrLeaveSaveReqVO createReqVO);

    /**
     * 更新请假申请
     *
     * @param updateReqVO 更新信息
     */
    void updateLeave(@Valid HrLeaveSaveReqVO updateReqVO);

    /**
     * 更新请假申请状态
     *
     * @param id     编号
     * @param status 状态
     */
    void updateLeaveStatus(Long id, Integer status);

    /**
     * 获得请假申请
     *
     * @param id 编号
     * @return 请假申请
     */
    HrAdministrativeLeaveDO getLeave(Long id);

    /**
     * 获得请假申请分页
     *
     * @param pageReqVO 分页查询
     * @return 请假申请分页
     */
    PageResult<HrAdministrativeLeaveDO> getLeavePage(HrLeavePageReqVO pageReqVO);
}
