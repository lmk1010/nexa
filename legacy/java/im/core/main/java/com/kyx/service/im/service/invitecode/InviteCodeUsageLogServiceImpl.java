package com.kyx.service.im.service.invitecode;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.service.im.controller.admin.invitecode.vo.InviteCodeUsageLogPageReqVO;
import com.kyx.service.im.dal.dataobject.invitecode.InviteCodeUsageLogDO;
import com.kyx.service.im.dal.mysql.invitecode.InviteCodeUsageLogMapper;
import com.kyx.service.im.dal.mysql.invitecode.InviteCodeMapper;
import com.kyx.service.im.dal.dataobject.invitecode.InviteCodeDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 邀请码使用记录 Service 实现类
 *
 * @author kyx
 */
@Service
@Validated
@Slf4j
public class InviteCodeUsageLogServiceImpl implements InviteCodeUsageLogService {

    @Resource
    private InviteCodeUsageLogMapper inviteCodeUsageLogMapper;

    @Resource
    private InviteCodeMapper inviteCodeMapper;

    @Override
    public Long createUsageLog(Long inviteCodeId, String inviteCode, Long userId, String userName,
                              String userIp, String userAgent, String deviceType, String deviceId,
                              Integer usageResult, String errorMessage) {
        // 获取租户ID和租户名称
        Long tenantId = null;
        String tenantName = null;
        
        if (inviteCodeId != null) {
            // 如果有邀请码ID，直接从邀请码表获取租户信息
            InviteCodeDO inviteCodeDO = inviteCodeMapper.selectById(inviteCodeId);
            if (inviteCodeDO != null) {
                tenantId = inviteCodeDO.getTenantId();
                tenantName = inviteCodeDO.getTenantName();
            }
        } else if (inviteCode != null) {
            // 如果没有邀请码ID但有邀请码，通过邀请码查询
            InviteCodeDO inviteCodeDO = inviteCodeMapper.selectByCodeWithTenant(inviteCode);
            if (inviteCodeDO != null) {
                tenantId = inviteCodeDO.getTenantId();
                tenantName = inviteCodeDO.getTenantName();
            }
        }
        
        // 如果通过邀请码无法获取租户信息，则使用当前上下文中的租户ID
        if (tenantId == null) {
            tenantId = TenantContextHolder.getTenantId();
            log.warn("无法从邀请码获取租户信息，使用当前上下文租户ID: {}", tenantId);
        }

        // 构建使用记录对象
        InviteCodeUsageLogDO usageLog = new InviteCodeUsageLogDO()
                .setInviteCodeId(inviteCodeId)
                .setInviteCode(inviteCode)
                .setTenantId(tenantId)
                .setTenantName(tenantName)
                .setUserId(userId)
                .setUserName(userName)
                .setUserIp(userIp)
                .setUserAgent(userAgent)
                .setDeviceType(deviceType)
                .setDeviceId(deviceId)
                .setUsageTime(LocalDateTime.now())
                .setUsageResult(usageResult)
                .setErrorMessage(errorMessage);

        // 插入数据库
        inviteCodeUsageLogMapper.insert(usageLog);
        return usageLog.getId();
    }

    @Override
    public InviteCodeUsageLogDO getUsageLog(Long id) {
        return inviteCodeUsageLogMapper.selectById(id);
    }

    @Override
    public PageResult<InviteCodeUsageLogDO> getUsageLogPage(@Valid InviteCodeUsageLogPageReqVO pageReqVO) {
        try {
            // 创建分页对象
            IPage<InviteCodeUsageLogDO> page = new Page<>(pageReqVO.getPageNo(), pageReqVO.getPageSize());
            
            // 使用XML映射的分页查询
            IPage<InviteCodeUsageLogDO> pageResult = inviteCodeUsageLogMapper.selectPageByXml(page, pageReqVO);
            
            // 转换为PageResult
            return new PageResult<>(pageResult.getRecords(), pageResult.getTotal());
        } catch (Exception e) {
            log.error("查询邀请码使用记录分页失败", e);
            return new PageResult<>();
        }
    }

    @Override
    public List<InviteCodeUsageLogDO> getUsageLogsByInviteCodeId(Long inviteCodeId) {
        return inviteCodeUsageLogMapper.selectListByInviteCodeId(inviteCodeId);
    }

    @Override
    public List<InviteCodeUsageLogDO> getUsageLogsByInviteCodeIdWithTenant(Long inviteCodeId) {
        return inviteCodeUsageLogMapper.selectListByInviteCodeIdWithTenant(inviteCodeId);
    }

    @Override
    public List<InviteCodeUsageLogDO> getUsageLogsByInviteCode(String inviteCode) {
        return inviteCodeUsageLogMapper.selectListByInviteCode(inviteCode);
    }

    @Override
    public List<InviteCodeUsageLogDO> getUsageLogsByInviteCodeWithTenant(String inviteCode) {
        return inviteCodeUsageLogMapper.selectListByInviteCodeWithTenant(inviteCode);
    }

    @Override
    public List<InviteCodeUsageLogDO> getUsageLogsByTenantId(Long tenantId) {
        return inviteCodeUsageLogMapper.selectListByTenantId(tenantId);
    }

    @Override
    public List<InviteCodeUsageLogDO> getUsageLogsByTenantIdWithTenant(Long tenantId) {
        return inviteCodeUsageLogMapper.selectListByTenantIdWithTenant(tenantId);
    }

    @Override
    public Long getSuccessUsageCount(Long inviteCodeId) {
        return inviteCodeUsageLogMapper.countSuccessByInviteCodeId(inviteCodeId);
    }

    @Override
    public Long getFailureUsageCount(Long inviteCodeId) {
        return inviteCodeUsageLogMapper.countFailureByInviteCodeId(inviteCodeId);
    }
} 