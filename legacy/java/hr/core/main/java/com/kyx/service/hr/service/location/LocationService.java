package com.kyx.service.hr.service.location;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.location.vo.LocationPageReqVO;
import com.kyx.service.hr.controller.admin.location.vo.LocationRespVO;
import com.kyx.service.hr.controller.admin.location.vo.LocationSaveReqVO;
import com.kyx.service.hr.dal.dataobject.location.LocationDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 公司地点管理 Service 接口
 *
 * @author MK
 */
public interface LocationService {

    /**
     * 创建地点
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createLocation(@Valid LocationSaveReqVO createReqVO);

    /**
     * 更新地点
     *
     * @param updateReqVO 更新信息
     */
    void updateLocation(@Valid LocationSaveReqVO updateReqVO);

    /**
     * 删除地点
     *
     * @param id 编号
     */
    void deleteLocation(Long id);

    /**
     * 获得地点
     *
     * @param id 编号
     * @return 地点
     */
    LocationDO getLocation(Long id);

    /**
     * 获得地点分页
     *
     * @param pageReqVO 分页查询
     * @return 地点分页
     */
    PageResult<LocationRespVO> getLocationPage(LocationPageReqVO pageReqVO);

    /**
     * 校验地点是否存在
     *
     * @param id 编号
     */
    void validateLocationExists(Long id);

    /**
     * 校验地点名称是否唯一
     *
     * @param id 编号
     * @param locationName 地点名称
     */
    void validateLocationNameUnique(Long id, String locationName);

    /**
     * 获得所有地点列表
     *
     * @return 地点列表
     */
    List<LocationDO> getLocationList();

} 