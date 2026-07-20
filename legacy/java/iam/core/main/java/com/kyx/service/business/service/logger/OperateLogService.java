package com.kyx.service.business.service.logger;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.biz.system.logger.dto.OperateLogCreateReqDTO;
import com.kyx.service.business.api.logger.dto.OperateLogPageReqDTO;
import com.kyx.service.business.controller.admin.logger.vo.operatelog.OperateLogPageReqVO;
import com.kyx.service.business.dal.dataobject.logger.OperateLogDO;

/**
 * 操作日志 Service 接口
 *
 * @author MK
 */
public interface OperateLogService {

    /**
     * 记录操作日志
     *
     * @param createReqDTO 创建请求
     */
    void createOperateLog(OperateLogCreateReqDTO createReqDTO);

    /**
     * 获得操作日志分页列表
     *
     * @param pageReqVO 分页条件
     * @return 操作日志分页列表
     */
    PageResult<OperateLogDO> getOperateLogPage(OperateLogPageReqVO pageReqVO);

    /**
     * 获得操作日志分页列表
     *
     * @param pageReqVO 分页条件
     * @return 操作日志分页列表
     */
    PageResult<OperateLogDO> getOperateLogPage(OperateLogPageReqDTO pageReqVO);

}
