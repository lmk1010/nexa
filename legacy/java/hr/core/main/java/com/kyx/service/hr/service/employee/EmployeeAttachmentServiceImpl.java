package com.kyx.service.hr.service.employee;

import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeAttachmentRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeAttachmentSaveReqVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeAttachmentDO;
import com.kyx.service.hr.dal.mysql.employee.EmployeeAttachmentMapper;
import com.kyx.service.hr.enums.ErrorCodeConstants;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

@Service
@Validated
public class EmployeeAttachmentServiceImpl implements EmployeeAttachmentService {

    @Resource
    private EmployeeAttachmentMapper employeeAttachmentMapper;

    @Override
    public List<EmployeeAttachmentRespVO> getAttachmentList(Long profileId) {
        List<EmployeeAttachmentDO> list = employeeAttachmentMapper.selectListByProfileId(profileId);
        return BeanUtils.toBean(list, EmployeeAttachmentRespVO.class);
    }

    @Override
    public Long createAttachment(EmployeeAttachmentSaveReqVO createReqVO) {
        EmployeeAttachmentDO attachment = BeanUtils.toBean(createReqVO, EmployeeAttachmentDO.class);
        employeeAttachmentMapper.insert(attachment);
        return attachment.getId();
    }

    @Override
    public void updateAttachment(EmployeeAttachmentSaveReqVO updateReqVO) {
        if (updateReqVO.getId() == null || employeeAttachmentMapper.selectById(updateReqVO.getId()) == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS);
        }
        EmployeeAttachmentDO attachment = BeanUtils.toBean(updateReqVO, EmployeeAttachmentDO.class);
        employeeAttachmentMapper.updateById(attachment);
    }

    @Override
    public void deleteAttachment(Long id) {
        if (employeeAttachmentMapper.selectById(id) == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EMPLOYEE_PROFILE_NOT_EXISTS);
        }
        employeeAttachmentMapper.deleteById(id);
    }
}
