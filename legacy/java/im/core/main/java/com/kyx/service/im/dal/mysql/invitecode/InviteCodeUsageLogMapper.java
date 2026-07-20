package com.kyx.service.im.dal.mysql.invitecode;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.im.controller.admin.invitecode.vo.InviteCodeUsageLogPageReqVO;
import com.kyx.service.im.dal.dataobject.invitecode.InviteCodeUsageLogDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 邀请码使用记录 Mapper
 *
 * @author MK
 */
@Mapper
public interface InviteCodeUsageLogMapper extends BaseMapperX<InviteCodeUsageLogDO> {

    /**
     * 分页查询邀请码使用记录列表
     * 
     * @param page 分页参数
     * @param reqVO 分页查询参数
     * @return 分页结果
     */
    IPage<InviteCodeUsageLogDO> selectPageByXml(IPage<InviteCodeUsageLogDO> page, @Param("ew") InviteCodeUsageLogPageReqVO reqVO);

    /**
     * 根据邀请码ID查询使用记录列表
     *
     * @param inviteCodeId 邀请码ID
     * @return 使用记录列表
     */
    default List<InviteCodeUsageLogDO> selectListByInviteCodeId(Long inviteCodeId) {
        return selectList(new LambdaQueryWrapperX<InviteCodeUsageLogDO>()
                .eq(InviteCodeUsageLogDO::getInviteCodeId, inviteCodeId)
                .orderByDesc(InviteCodeUsageLogDO::getUsageTime));
    }

    /**
     * 根据邀请码ID查询使用记录列表（包含租户信息）
     *
     * @param inviteCodeId 邀请码ID
     * @return 使用记录列表
     */
    List<InviteCodeUsageLogDO> selectListByInviteCodeIdWithTenant(@Param("inviteCodeId") Long inviteCodeId);

    /**
     * 根据邀请码查询使用记录列表
     *
     * @param inviteCode 邀请码
     * @return 使用记录列表
     */
    default List<InviteCodeUsageLogDO> selectListByInviteCode(String inviteCode) {
        return selectList(new LambdaQueryWrapperX<InviteCodeUsageLogDO>()
                .eq(InviteCodeUsageLogDO::getInviteCode, inviteCode)
                .orderByDesc(InviteCodeUsageLogDO::getUsageTime));
    }

    /**
     * 根据邀请码查询使用记录列表（包含租户信息）
     *
     * @param inviteCode 邀请码
     * @return 使用记录列表
     */
    List<InviteCodeUsageLogDO> selectListByInviteCodeWithTenant(@Param("inviteCode") String inviteCode);

    /**
     * 根据租户ID查询使用记录列表
     *
     * @param tenantId 租户ID
     * @return 使用记录列表
     */
    default List<InviteCodeUsageLogDO> selectListByTenantId(Long tenantId) {
        return selectList(new LambdaQueryWrapperX<InviteCodeUsageLogDO>()
                .eq(InviteCodeUsageLogDO::getTenantId, tenantId)
                .orderByDesc(InviteCodeUsageLogDO::getUsageTime));
    }

    /**
     * 根据租户ID查询使用记录列表（包含租户信息）
     *
     * @param tenantId 租户ID
     * @return 使用记录列表
     */
    List<InviteCodeUsageLogDO> selectListByTenantIdWithTenant(@Param("tenantId") Long tenantId);

    /**
     * 统计邀请码使用次数
     *
     * @param inviteCodeId 邀请码ID
     * @return 使用次数
     */
    default Long countByInviteCodeId(Long inviteCodeId) {
        return selectCount(new LambdaQueryWrapperX<InviteCodeUsageLogDO>()
                .eq(InviteCodeUsageLogDO::getInviteCodeId, inviteCodeId)
                .eq(InviteCodeUsageLogDO::getUsageResult, 1));
    }

    /**
     * 统计邀请码成功使用次数
     *
     * @param inviteCodeId 邀请码ID
     * @return 成功使用次数
     */
    default Long countSuccessByInviteCodeId(Long inviteCodeId) {
        return selectCount(new LambdaQueryWrapperX<InviteCodeUsageLogDO>()
                .eq(InviteCodeUsageLogDO::getInviteCodeId, inviteCodeId)
                .eq(InviteCodeUsageLogDO::getUsageResult, 1));
    }

    /**
     * 统计邀请码失败使用次数
     *
     * @param inviteCodeId 邀请码ID
     * @return 失败使用次数
     */
    default Long countFailureByInviteCodeId(Long inviteCodeId) {
        return selectCount(new LambdaQueryWrapperX<InviteCodeUsageLogDO>()
                .eq(InviteCodeUsageLogDO::getInviteCodeId, inviteCodeId)
                .eq(InviteCodeUsageLogDO::getUsageResult, 0));
    }
} 