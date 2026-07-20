package com.kyx.service.im.service.invitecode;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.im.controller.admin.invitecode.vo.InviteCodeUsageLogPageReqVO;
import com.kyx.service.im.controller.admin.invitecode.vo.InviteCodeUsageLogRespVO;
import com.kyx.service.im.dal.dataobject.invitecode.InviteCodeUsageLogDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 邀请码使用记录 Service 接口
 *
 * @author kyx
 */
public interface InviteCodeUsageLogService {

    /**
     * 创建邀请码使用记录
     *
     * @param inviteCodeId 邀请码ID（可为null，当邀请码不存在时）
     * @param inviteCode 邀请码
     * @param userId 使用者用户ID
     * @param userName 使用者用户名
     * @param userIp 使用者IP
     * @param userAgent 浏览器UA
     * @param deviceType 设备类型
     * @param deviceId 设备标识
     * @param usageResult 使用结果：1-成功，0-失败
     * @param errorMessage 错误信息
     * @return 记录ID
     */
    Long createUsageLog(Long inviteCodeId, String inviteCode, Long userId, String userName, 
                       String userIp, String userAgent, String deviceType, String deviceId,
                       Integer usageResult, String errorMessage);

    /**
     * 获得邀请码使用记录
     *
     * @param id 编号
     * @return 邀请码使用记录
     */
    InviteCodeUsageLogDO getUsageLog(Long id);

    /**
     * 获得邀请码使用记录分页
     *
     * @param pageReqVO 分页查询
     * @return 邀请码使用记录分页
     */
    PageResult<InviteCodeUsageLogDO> getUsageLogPage(@Valid InviteCodeUsageLogPageReqVO pageReqVO);

    /**
     * 根据邀请码ID获取使用记录列表
     *
     * @param inviteCodeId 邀请码ID
     * @return 使用记录列表
     */
    List<InviteCodeUsageLogDO> getUsageLogsByInviteCodeId(Long inviteCodeId);

    /**
     * 根据邀请码ID获取使用记录列表（包含租户信息）
     *
     * @param inviteCodeId 邀请码ID
     * @return 使用记录列表
     */
    List<InviteCodeUsageLogDO> getUsageLogsByInviteCodeIdWithTenant(Long inviteCodeId);

    /**
     * 根据邀请码获取使用记录列表
     *
     * @param inviteCode 邀请码
     * @return 使用记录列表
     */
    List<InviteCodeUsageLogDO> getUsageLogsByInviteCode(String inviteCode);

    /**
     * 根据邀请码获取使用记录列表（包含租户信息）
     *
     * @param inviteCode 邀请码
     * @return 使用记录列表
     */
    List<InviteCodeUsageLogDO> getUsageLogsByInviteCodeWithTenant(String inviteCode);

    /**
     * 根据租户ID获取使用记录列表
     *
     * @param tenantId 租户ID
     * @return 使用记录列表
     */
    List<InviteCodeUsageLogDO> getUsageLogsByTenantId(Long tenantId);

    /**
     * 根据租户ID获取使用记录列表（包含租户信息）
     *
     * @param tenantId 租户ID
     * @return 使用记录列表
     */
    List<InviteCodeUsageLogDO> getUsageLogsByTenantIdWithTenant(Long tenantId);

    /**
     * 获取邀请码成功使用次数
     *
     * @param inviteCodeId 邀请码ID
     * @return 成功使用次数
     */
    Long getSuccessUsageCount(Long inviteCodeId);

    /**
     * 获取邀请码失败使用次数
     *
     * @param inviteCodeId 邀请码ID
     * @return 失败使用次数
     */
    Long getFailureUsageCount(Long inviteCodeId);
} 