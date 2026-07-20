package com.kyx.service.im.service.invitecode;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.foundation.web.core.util.WebFrameworkUtils;
import com.kyx.service.im.controller.admin.invitecode.vo.InviteCodeCreateReqVO;
import com.kyx.service.im.controller.admin.invitecode.vo.InviteCodePageReqVO;
import com.kyx.service.im.controller.admin.invitecode.vo.InviteCodeUpdateReqVO;
import com.kyx.service.im.controller.admin.invitecode.vo.InviteCodeValidateRespVO;
import com.kyx.service.im.dal.dataobject.invitecode.InviteCodeDO;
import com.kyx.service.im.dal.dataobject.invitecode.InviteCodeStatusEnum;
import com.kyx.service.im.dal.dataobject.invitecode.InviteCodeTypeEnum;
import com.kyx.service.im.dal.mysql.invitecode.InviteCodeMapper;
import com.kyx.service.im.dal.mysql.invitecode.TenantInviteCodeStatsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.im.enums.ErrorCodeConstants.*;

/**
 * 邀请码 Service 实现类
 *
 * @author MK
 */
@Service
@Validated
@Slf4j
public class InviteCodeServiceImpl implements InviteCodeService {

    @Resource
    private InviteCodeMapper inviteCodeMapper;

    @Resource
    private InviteCodeUsageLogService inviteCodeUsageLogService;

    @Override
    public Long createInviteCode(InviteCodeCreateReqVO createReqVO) {
        // 1. 生成邀请码（如果没有提供）
        if (StrUtil.isBlank(createReqVO.getCode())) {
            createReqVO.setCode(generateInviteCode());
        }

        // 2. 检查邀请码是否已存在
        InviteCodeDO existingCode = inviteCodeMapper.selectByCodeWithoutTenant(createReqVO.getCode());
        if (existingCode != null) {
            throw exception(INVITE_CODE_EXISTS);
        }

        // 3. 创建邀请码
        InviteCodeDO inviteCode = new InviteCodeDO();
        inviteCode.setCode(createReqVO.getCode());
        inviteCode.setType(createReqVO.getType());
        inviteCode.setUsageLimit(createReqVO.getUsageLimit());
        inviteCode.setUsedCount(0);
        inviteCode.setValidStartTime(createReqVO.getValidStartTime());
        inviteCode.setValidEndTime(createReqVO.getValidEndTime());
        inviteCode.setStatus(createReqVO.getStatus() != null ? createReqVO.getStatus() : InviteCodeStatusEnum.ENABLE.getStatus());
        inviteCode.setRemark(createReqVO.getRemark());
        inviteCode.setCreatorId(SecurityFrameworkUtils.getLoginUserId());
        inviteCode.setCreatorName(SecurityFrameworkUtils.getLoginUserNickname());

        // 4. 插入数据库
        inviteCodeMapper.insert(inviteCode);
        return inviteCode.getId();
    }

    @Override
    public void updateInviteCode(InviteCodeUpdateReqVO updateReqVO) {
        // 1. 校验存在
        InviteCodeDO inviteCode = validateInviteCodeExists(updateReqVO.getId());

        // 2. 检查邀请码是否重复（如果修改了邀请码）
        if (StrUtil.isNotBlank(updateReqVO.getCode()) && !updateReqVO.getCode().equals(inviteCode.getCode())) {
            InviteCodeDO existingCode = inviteCodeMapper.selectByCodeWithoutTenant(updateReqVO.getCode());
            if (existingCode != null) {
                throw exception(INVITE_CODE_EXISTS);
            }
        }

        // 3. 更新邀请码
        InviteCodeDO updateObj = new InviteCodeDO();
        updateObj.setId(updateReqVO.getId());
        updateObj.setCode(updateReqVO.getCode());
        updateObj.setType(updateReqVO.getType());
        updateObj.setUsageLimit(updateReqVO.getUsageLimit());
        updateObj.setValidStartTime(updateReqVO.getValidStartTime());
        updateObj.setValidEndTime(updateReqVO.getValidEndTime());
        updateObj.setStatus(updateReqVO.getStatus());
        updateObj.setRemark(updateReqVO.getRemark());

        inviteCodeMapper.updateById(updateObj);
    }

    @Override
    public void deleteInviteCode(Long id) {
        // 1. 校验存在
        validateInviteCodeExists(id);

        // 2. 删除
        inviteCodeMapper.deleteById(id);
    }

    @Override
    public InviteCodeDO getInviteCode(Long id) {
        return inviteCodeMapper.selectById(id);
    }

    @Override
    public PageResult<InviteCodeDO> getInviteCodePage(InviteCodePageReqVO pageReqVO) {
        // 使用XML方式执行分页查询
        IPage<InviteCodeDO> page = new Page<>(pageReqVO.getPageNo(), pageReqVO.getPageSize());
        IPage<InviteCodeDO> result = inviteCodeMapper.selectPageByXml(page, pageReqVO);
        // 转换为 PageResult
        return new PageResult<>(result.getRecords(), result.getTotal());
    }

    @Override
    public InviteCodeDO getInviteCodeByCode(String code) {
        return inviteCodeMapper.selectByCodeWithoutTenant(code);
    }

    @Override
    public boolean validateInviteCode(String code) {
        if (StrUtil.isBlank(code)) {
            return false;
        }

        InviteCodeDO inviteCode = inviteCodeMapper.selectByCodeWithoutTenant(code);
        if (inviteCode == null) {
            return false;
        }

        // 检查状态
        if (!InviteCodeStatusEnum.ENABLE.getStatus().equals(inviteCode.getStatus())) {
            return false;
        }

        // 检查有效期
        LocalDateTime now = LocalDateTime.now();
        if (inviteCode.getValidStartTime() != null && now.isBefore(inviteCode.getValidStartTime())) {
            return false;
        }
        if (inviteCode.getValidEndTime() != null && now.isAfter(inviteCode.getValidEndTime())) {
            return false;
        }

        // 检查使用次数
        if (inviteCode.getUsageLimit() != null && inviteCode.getUsedCount() >= inviteCode.getUsageLimit()) {
            return false;
        }

        return true;
    }

    @Override
    public InviteCodeValidateRespVO validateInviteCodeWithDetails(String code) {
        InviteCodeValidateRespVO respVO = new InviteCodeValidateRespVO();
        respVO.setCode(code);
        
        if (StrUtil.isBlank(code)) {
            respVO.setValid(false);
            return respVO;
        }

        InviteCodeDO inviteCode = inviteCodeMapper.selectByCodeWithTenant(code);
        if (inviteCode == null) {
            respVO.setValid(false);
            return respVO;
        }

        // 设置基本信息
        respVO.setTenantId(inviteCode.getTenantId());
        respVO.setTenantName(inviteCode.getTenantName());
        respVO.setTenantStatus(inviteCode.getTenantStatus());
        respVO.setType(inviteCode.getType());
        respVO.setUsageLimit(inviteCode.getUsageLimit());
        respVO.setUsedCount(inviteCode.getUsedCount());
        respVO.setStatus(inviteCode.getStatus());

        // 检查状态
        if (!InviteCodeStatusEnum.ENABLE.getStatus().equals(inviteCode.getStatus())) {
            respVO.setValid(false);
            return respVO;
        }

        // 检查有效期
        LocalDateTime now = LocalDateTime.now();
        if (inviteCode.getValidStartTime() != null && now.isBefore(inviteCode.getValidStartTime())) {
            respVO.setValid(false);
            return respVO;
        }
        if (inviteCode.getValidEndTime() != null && now.isAfter(inviteCode.getValidEndTime())) {
            respVO.setValid(false);
            return respVO;
        }

        // 检查使用次数
        if (inviteCode.getUsageLimit() != null && inviteCode.getUsedCount() >= inviteCode.getUsageLimit()) {
            respVO.setValid(false);
            return respVO;
        }

        respVO.setValid(true);
        return respVO;
    }

    @Override
    public void useInviteCode(String code) {
        InviteCodeDO inviteCode = inviteCodeMapper.selectByCodeWithoutTenant(code);
        if (inviteCode == null) {
            // 记录邀请码不存在的失败日志
            recordFailedUsageLogForNonExistentCode(code, "邀请码不存在");
            throw exception(INVITE_CODE_NOT_EXISTS);
        }

        // 验证邀请码
        if (!validateInviteCode(code)) {
            // 记录验证失败日志
            recordFailedUsageLog(inviteCode, code, "邀请码验证失败");
            throw exception(INVITE_CODE_INVALID);
        }

        // 更新使用次数
        inviteCodeMapper.updateUsedCount(inviteCode.getId());

        // 记录使用日志
        try {
            // 获取当前用户信息
            Long userId = SecurityFrameworkUtils.getLoginUserId();
            String userName = SecurityFrameworkUtils.getLoginUserNickname();
            
            // 获取请求信息（这里简化处理，实际可以从请求上下文获取）
            String userIp = "127.0.0.1"; // 可以从请求上下文获取
            String userAgent = "Unknown"; // 可以从请求上下文获取
            String deviceType = "web"; // 可以从请求上下文获取
            String deviceId = "web-" + System.currentTimeMillis(); // 可以从请求上下文获取
            
            // 记录成功使用日志
            inviteCodeUsageLogService.createUsageLog(
                inviteCode.getId(), 
                code, 
                userId, 
                userName, 
                userIp, 
                userAgent, 
                deviceType, 
                deviceId, 
                1, // 成功
                null // 无错误信息
            );
        } catch (Exception e) {
            log.error("记录邀请码使用日志失败", e);
            // 不影响主流程，只记录日志
        }
    }

    @Override
    public List<InviteCodeDO> getInviteCodeListByTenantId(Long tenantId) {
        return inviteCodeMapper.selectListByTenantId(tenantId);
    }

    @Override
    public List<InviteCodeDO> getInviteCodeListByTenantIdAndType(Long tenantId, Integer type) {
        return inviteCodeMapper.selectListByTenantIdAndType(tenantId, type);
    }

    @Override
    public List<TenantInviteCodeStatsVO> getTenantInviteCodeStats() {
        return inviteCodeMapper.selectTenantInviteCodeStats();
    }

    @Override
    public void updateExpiredInviteCodes() {
        inviteCodeMapper.updateExpiredStatus();
    }

    @Override
    public void updateUsedUpInviteCodes() {
        inviteCodeMapper.updateUsedUpStatus();
    }

    /**
     * 生成邀请码
     *
     * @return 邀请码
     */
    private String generateInviteCode() {
        return "INV" + IdUtil.fastSimpleUUID().substring(0, 8).toUpperCase();
    }

    /**
     * 校验邀请码是否存在
     *
     * @param id 邀请码编号
     * @return 邀请码
     */
    private InviteCodeDO validateInviteCodeExists(Long id) {
        InviteCodeDO inviteCode = inviteCodeMapper.selectById(id);
        if (inviteCode == null) {
            throw exception(INVITE_CODE_NOT_EXISTS);
        }
        return inviteCode;
    }

    /**
     * 记录失败的使用日志
     */
    private void recordFailedUsageLog(InviteCodeDO inviteCode, String code, String errorMessage) {
        try {
            // 获取当前用户信息
            Long userId = SecurityFrameworkUtils.getLoginUserId();
            String userName = SecurityFrameworkUtils.getLoginUserNickname();
            
            // 获取请求信息（这里简化处理，实际可以从请求上下文获取）
            String userIp = "127.0.0.1"; // 可以从请求上下文获取
            String userAgent = "Unknown"; // 可以从请求上下文获取
            String deviceType = "web"; // 可以从请求上下文获取
            String deviceId = "web-" + System.currentTimeMillis(); // 可以从请求上下文获取
            
            // 记录失败使用日志，使用邀请码的租户信息
            inviteCodeUsageLogService.createUsageLog(
                inviteCode.getId(), 
                code, 
                userId, 
                userName, 
                userIp, 
                userAgent, 
                deviceType, 
                deviceId, 
                0, // 失败
                errorMessage
            );
        } catch (Exception e) {
            log.error("记录邀请码使用失败日志失败", e);
            // 不影响主流程，只记录日志
        }
    }

    /**
     * 记录邀请码不存在时的失败日志
     */
    private void recordFailedUsageLogForNonExistentCode(String code, String errorMessage) {
        try {
            // 获取当前用户信息
            Long userId = SecurityFrameworkUtils.getLoginUserId();
            String userName = SecurityFrameworkUtils.getLoginUserNickname();
            
            // 获取请求信息（这里简化处理，实际可以从请求上下文获取）
            String userIp = "127.0.0.1"; // 可以从请求上下文获取
            String userAgent = "Unknown"; // 可以从请求上下文获取
            String deviceType = "web"; // 可以从请求上下文获取
            String deviceId = "web-" + System.currentTimeMillis(); // 可以从请求上下文获取
            
            // 记录失败使用日志（邀请码ID为null，但会通过邀请码查询租户信息）
            inviteCodeUsageLogService.createUsageLog(
                null, // 邀请码ID为null，因为邀请码不存在
                code, 
                userId, 
                userName, 
                userIp, 
                userAgent, 
                deviceType, 
                deviceId, 
                0, // 失败
                errorMessage
            );
        } catch (Exception e) {
            log.error("记录邀请码使用失败日志失败", e);
            // 不影响主流程，只记录日志
        }
    }

} 