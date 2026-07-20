package com.kyx.service.hr.service.location;

import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.hr.controller.admin.location.vo.LocationPageReqVO;
import com.kyx.service.hr.controller.admin.location.vo.LocationRespVO;
import com.kyx.service.hr.controller.admin.location.vo.LocationSaveReqVO;
import com.kyx.service.hr.dal.dataobject.location.LocationDO;
import com.kyx.service.hr.dal.mysql.location.LocationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static com.kyx.service.hr.enums.ErrorCodeConstants.*;

/**
 * 公司地点管理 Service 实现类
 *
 * @author MK
 */
@Service
@Validated
@Slf4j
public class LocationServiceImpl implements LocationService {

    @Resource
    private LocationMapper locationMapper;

    @Override
    public Long createLocation(LocationSaveReqVO createReqVO) {
        // 校验地点名称唯一性
        validateLocationNameUnique(null, createReqVO.getLocationName());

        // 插入
        LocationDO location = BeanUtils.toBean(createReqVO, LocationDO.class);
        locationMapper.insert(location);
        return location.getId();
    }

    @Override
    public void updateLocation(LocationSaveReqVO updateReqVO) {
        // 校验存在
        validateLocationExists(updateReqVO.getId());
        
        // 校验地点名称唯一性
        validateLocationNameUnique(updateReqVO.getId(), updateReqVO.getLocationName());

        // 更新
        LocationDO updateObj = BeanUtils.toBean(updateReqVO, LocationDO.class);
        locationMapper.updateById(updateObj);
    }

    @Override
    public void deleteLocation(Long id) {
        // 校验存在
        validateLocationExists(id);
        
        // 删除
        locationMapper.deleteById(id);
    }

    @Override
    public LocationDO getLocation(Long id) {
        return locationMapper.selectById(id);
    }

    @Override
    public PageResult<LocationRespVO> getLocationPage(LocationPageReqVO pageReqVO) {
        PageResult<LocationDO> pageResult = locationMapper.selectPage(pageReqVO);
        return BeanUtils.toBean(pageResult, LocationRespVO.class);
    }

    @Override
    public void validateLocationExists(Long id) {
        if (getLocation(id) == null) {
            throw ServiceExceptionUtil.exception(LOCATION_NOT_EXISTS);
        }
    }

    @Override
    public void validateLocationNameUnique(Long id, String locationName) {
        LocationDO location = locationMapper.selectByLocationName(locationName);
        if (location == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的地点
        if (id == null) {
            throw ServiceExceptionUtil.exception(LOCATION_NAME_DUPLICATE);
        }
        if (!location.getId().equals(id)) {
            throw ServiceExceptionUtil.exception(LOCATION_NAME_DUPLICATE);
        }
    }

    @Override
    public List<LocationDO> getLocationList() {
        return locationMapper.selectList();
    }

} 