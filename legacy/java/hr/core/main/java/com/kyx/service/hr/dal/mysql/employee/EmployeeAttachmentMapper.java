package com.kyx.service.hr.dal.mysql.employee;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeAttachmentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 员工附件信息 Mapper
 *
 * @author MK
 */
@Mapper
public interface EmployeeAttachmentMapper extends BaseMapperX<EmployeeAttachmentDO> {

    default List<EmployeeAttachmentDO> selectListByProfileId(Long profileId) {
        return selectList(EmployeeAttachmentDO::getProfileId, profileId);
    }

    default List<EmployeeAttachmentDO> selectListByAttachmentType(String attachmentType) {
        return selectList(EmployeeAttachmentDO::getAttachmentType, attachmentType);
    }

} 