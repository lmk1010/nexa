package com.kyx.service.im.api.invitecode;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.tenant.core.aop.TenantIgnore;
import com.kyx.service.im.api.invitecode.dto.*;
import com.kyx.service.im.controller.admin.invitecode.vo.InviteCodeCreateReqVO;
import com.kyx.service.im.controller.admin.invitecode.vo.InviteCodePageReqVO;
import com.kyx.service.im.controller.admin.invitecode.vo.InviteCodeUpdateReqVO;
import com.kyx.service.im.controller.admin.invitecode.vo.InviteCodeValidateRespVO;
import com.kyx.service.im.dal.dataobject.invitecode.InviteCodeDO;
import com.kyx.service.im.dal.mysql.invitecode.TenantInviteCodeStatsVO;
import com.kyx.service.im.service.invitecode.InviteCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 邀请码 API 实现类
 *
 * @author MK
 */
@RestController // 提供 RESTful API 接口，给 Feign 调用
@Service
@Validated
@Slf4j
@TenantIgnore
public class InviteCodeApiImpl implements InviteCodeApi {

    @Resource
    private InviteCodeService inviteCodeService;

    @Override
    public InviteCodeRespDTO getInviteCodeByCode(String code) {
        InviteCodeDO inviteCode = inviteCodeService.getInviteCodeByCode(code);
        return convertInviteCodeRespDTO(inviteCode);
    }

    @Override
    public InviteCodeValidateRespDTO validateInviteCode(String code) {
        InviteCodeValidateRespVO respVO = inviteCodeService.validateInviteCodeWithDetails(code);
        return convertInviteCodeValidateRespDTO(respVO);
    }

    @Override
    public void useInviteCode(String code) {
        inviteCodeService.useInviteCode(code);
    }

    /**
     * 转换邀请码响应DTO
     *
     * @param inviteCode 邀请码DO
     * @return 邀请码响应DTO
     */
    private InviteCodeRespDTO convertInviteCodeRespDTO(InviteCodeDO inviteCode) {
        if (inviteCode == null) {
            return null;
        }
        InviteCodeRespDTO respDTO = new InviteCodeRespDTO();
        BeanUtils.copyProperties(inviteCode, respDTO);

        // 手动转换日期字段
        respDTO.setValidStartTime(inviteCode.getValidStartTime() != null ?
            Timestamp.valueOf(inviteCode.getValidStartTime()) : null);
        respDTO.setValidEndTime(inviteCode.getValidEndTime() != null ?
            Timestamp.valueOf(inviteCode.getValidEndTime()) : null);
        respDTO.setCreateTime(inviteCode.getCreateTime() != null ?
            Timestamp.valueOf(String.valueOf(inviteCode.getCreateTime())) : null);

        return respDTO;
    }

    /**
     * 转换租户邀请码统计响应DTO
     *
     * @param statsVO 统计VO
     * @return 统计响应DTO
     */
    private TenantInviteCodeStatsRespDTO convertTenantInviteCodeStatsRespDTO(TenantInviteCodeStatsVO statsVO) {
        if (statsVO == null) {
            return null;
        }
        TenantInviteCodeStatsRespDTO respDTO = new TenantInviteCodeStatsRespDTO();
        BeanUtils.copyProperties(statsVO, respDTO);
        return respDTO;
    }

    /**
     * 转换邀请码验证响应DTO
     *
     * @param respVO 邀请码验证响应VO
     * @return 邀请码验证响应DTO
     */
    private InviteCodeValidateRespDTO convertInviteCodeValidateRespDTO(InviteCodeValidateRespVO respVO) {
        if (respVO == null) {
            return null;
        }
        InviteCodeValidateRespDTO respDTO = new InviteCodeValidateRespDTO();
        BeanUtils.copyProperties(respVO, respDTO);
        return respDTO;
    }

} 