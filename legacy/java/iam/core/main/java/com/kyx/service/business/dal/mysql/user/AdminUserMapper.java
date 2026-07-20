package com.kyx.service.business.dal.mysql.user;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.business.controller.admin.user.vo.user.UserPageReqVO;
import com.kyx.service.business.dal.dataobject.user.AdminUserDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface AdminUserMapper extends BaseMapperX<AdminUserDO> {

    default AdminUserDO selectByUsername(String username) {
        return selectOne(AdminUserDO::getUsername, username);
    }

    default AdminUserDO selectByEmail(String email) {
        return selectOne(AdminUserDO::getEmail, email);
    }

    default AdminUserDO selectByMobile(String mobile) {
        return selectOne(AdminUserDO::getMobile, mobile);
    }

    default PageResult<AdminUserDO> selectPage(UserPageReqVO reqVO, Collection<Long> deptIds, Collection<Long> userIds) {
        LambdaQueryWrapperX<AdminUserDO> query = new LambdaQueryWrapperX<AdminUserDO>();

        // 关键字搜索：同时匹配用户名、昵称、手机号
        if (reqVO.getKeyword() != null && !reqVO.getKeyword().trim().isEmpty()) {
            query.and(w -> w
                .like(AdminUserDO::getUsername, reqVO.getKeyword())
                .or()
                .like(AdminUserDO::getNickname, reqVO.getKeyword())
                .or()
                .like(AdminUserDO::getMobile, reqVO.getKeyword())
            );
        }

        query.eqIfPresent(AdminUserDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(AdminUserDO::getCreateTime, reqVO.getCreateTime())
                .inIfPresent(AdminUserDO::getDeptId, deptIds)
                .inIfPresent(AdminUserDO::getId, userIds)
                .orderByDesc(AdminUserDO::getId);

        return selectPage(reqVO, query);
    }

    default List<AdminUserDO> selectListByNickname(String nickname) {
        return selectList(new LambdaQueryWrapperX<AdminUserDO>().like(AdminUserDO::getNickname, nickname));
    }

    default List<AdminUserDO> selectListByStatus(Integer status) {
        return selectList(AdminUserDO::getStatus, status);
    }

    default List<AdminUserDO> selectListByTenantIds(Collection<Long> tenantIds, Integer status) {
        return selectList(new LambdaQueryWrapperX<AdminUserDO>()
                .inIfPresent(AdminUserDO::getTenantId, tenantIds)
                .eqIfPresent(AdminUserDO::getStatus, status)
                .orderByDesc(AdminUserDO::getId));
    }

    default List<AdminUserDO> selectListByDeptIds(Collection<Long> deptIds) {
        return selectList(AdminUserDO::getDeptId, deptIds);
    }

    default void updateBatchDeptId(Collection<Long> userIds, Long newDeptId) {
        update(new AdminUserDO().setDeptId(newDeptId), 
               new LambdaQueryWrapperX<AdminUserDO>().in(AdminUserDO::getId, userIds));
    }

}
