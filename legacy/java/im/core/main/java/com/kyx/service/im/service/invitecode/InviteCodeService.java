package com.kyx.service.im.service.invitecode;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.im.controller.admin.invitecode.vo.InviteCodeCreateReqVO;
import com.kyx.service.im.controller.admin.invitecode.vo.InviteCodePageReqVO;
import com.kyx.service.im.controller.admin.invitecode.vo.InviteCodeRespVO;
import com.kyx.service.im.controller.admin.invitecode.vo.InviteCodeUpdateReqVO;
import com.kyx.service.im.controller.admin.invitecode.vo.InviteCodeValidateRespVO;
import com.kyx.service.im.dal.dataobject.invitecode.InviteCodeDO;
import com.kyx.service.im.dal.mysql.invitecode.TenantInviteCodeStatsVO;

import javax.validation.Valid;
import java.util.List;

/**
 * 邀请码 Service 接口
 *
 * @author MK
 */
public interface InviteCodeService {

    /**
     * 创建邀请码
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createInviteCode(@Valid InviteCodeCreateReqVO createReqVO);

    /**
     * 更新邀请码
     *
     * @param updateReqVO 更新信息
     */
    void updateInviteCode(@Valid InviteCodeUpdateReqVO updateReqVO);

    /**
     * 删除邀请码
     *
     * @param id 编号
     */
    void deleteInviteCode(Long id);

    /**
     * 获得邀请码
     *
     * @param id 编号
     * @return 邀请码
     */
    InviteCodeDO getInviteCode(Long id);

    /**
     * 获得邀请码分页
     *
     * @param pageReqVO 分页查询
     * @return 邀请码分页
     */
    PageResult<InviteCodeDO> getInviteCodePage(InviteCodePageReqVO pageReqVO);

    /**
     * 根据邀请码获取邀请码信息
     *
     * @param code 邀请码
     * @return 邀请码信息
     */
    InviteCodeDO getInviteCodeByCode(String code);

    /**
     * 验证邀请码
     *
     * @param code 邀请码
     * @return 是否有效
     */
    boolean validateInviteCode(String code);

    /**
     * 验证邀请码并返回详细信息
     *
     * @param code 邀请码
     * @return 邀请码验证结果
     */
    InviteCodeValidateRespVO validateInviteCodeWithDetails(String code);

    /**
     * 使用邀请码
     *
     * @param code 邀请码
     */
    void useInviteCode(String code);

    /**
     * 根据租户ID获取邀请码列表
     *
     * @param tenantId 租户ID
     * @return 邀请码列表
     */
    List<InviteCodeDO> getInviteCodeListByTenantId(Long tenantId);

    /**
     * 根据租户ID和类型获取邀请码列表
     *
     * @param tenantId 租户ID
     * @param type 类型
     * @return 邀请码列表
     */
    List<InviteCodeDO> getInviteCodeListByTenantIdAndType(Long tenantId, Integer type);

    /**
     * 获取所有租户的邀请码统计信息
     *
     * @return 租户邀请码统计列表
     */
    List<TenantInviteCodeStatsVO> getTenantInviteCodeStats();

    /**
     * 批量更新过期邀请码状态
     */
    void updateExpiredInviteCodes();

    /**
     * 批量更新已用完邀请码状态
     */
    void updateUsedUpInviteCodes();

} 