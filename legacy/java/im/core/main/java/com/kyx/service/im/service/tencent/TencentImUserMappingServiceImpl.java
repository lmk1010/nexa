package com.kyx.service.im.service.tencent;

import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.im.config.tencent.TencentImProperties;
import com.kyx.service.im.controller.admin.tencent.vo.TencentImUserMappingCreateReqVO;
import com.kyx.service.im.controller.admin.tencent.vo.TencentImUserMappingPageReqVO;
import com.kyx.service.im.controller.admin.tencent.vo.TencentImUserMappingUpdateReqVO;
import com.kyx.service.im.dal.dataobject.tencent.TencentImUserMappingDO;
import com.kyx.service.im.dal.mysql.tencent.TencentImUserMappingMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.im.enums.ErrorCodeConstants.TENCENT_IM_MAPPING_EXISTS;
import static com.kyx.service.im.enums.ErrorCodeConstants.TENCENT_IM_MAPPING_INVALID;
import static com.kyx.service.im.enums.ErrorCodeConstants.TENCENT_IM_MAPPING_NOT_EXISTS;

@Service
@Validated
public class TencentImUserMappingServiceImpl implements TencentImUserMappingService {

    private static final long DEFAULT_TENANT_ID = 0L;
    private static final int STATUS_ENABLED = 0;

    @Resource
    private TencentImProperties properties;
    @Resource
    private TencentImUserMappingMapper userMappingMapper;

    @Override
    public Long createUserMapping(TencentImUserMappingCreateReqVO createReqVO) {
        TencentImUserMappingDO mapping = new TencentImUserMappingDO()
                .setTenantId(normalizeTenantId(createReqVO.getTenantId()))
                .setOaUserId(createReqVO.getOaUserId())
                .setOaUsername(trimToNull(createReqVO.getOaUsername()))
                .setOrdersysUserPrefix(defaultOrdersysPrefix(createReqVO.getOrdersysUserPrefix()))
                .setOrdersysUsername(requiredTrim(createReqVO.getOrdersysUsername()))
                .setFixedPrefix(defaultFixedPrefix(createReqVO.getFixedPrefix()))
                .setStatus(createReqVO.getStatus() == null ? STATUS_ENABLED : createReqVO.getStatus())
                .setRemark(trimToNull(createReqVO.getRemark()));
        mapping.setImUserId(resolveImUserId(createReqVO.getImUserId(), mapping));

        validateUnique(null, mapping);
        userMappingMapper.insert(mapping);
        return mapping.getId();
    }

    @Override
    public void updateUserMapping(TencentImUserMappingUpdateReqVO updateReqVO) {
        validateExists(updateReqVO.getId());

        TencentImUserMappingDO mapping = new TencentImUserMappingDO()
                .setId(updateReqVO.getId())
                .setTenantId(normalizeTenantId(updateReqVO.getTenantId()))
                .setOaUserId(updateReqVO.getOaUserId())
                .setOaUsername(trimToNull(updateReqVO.getOaUsername()))
                .setOrdersysUserPrefix(defaultOrdersysPrefix(updateReqVO.getOrdersysUserPrefix()))
                .setOrdersysUsername(requiredTrim(updateReqVO.getOrdersysUsername()))
                .setFixedPrefix(defaultFixedPrefix(updateReqVO.getFixedPrefix()))
                .setStatus(updateReqVO.getStatus() == null ? STATUS_ENABLED : updateReqVO.getStatus())
                .setRemark(trimToNull(updateReqVO.getRemark()));
        mapping.setImUserId(resolveImUserId(updateReqVO.getImUserId(), mapping));

        validateUnique(updateReqVO.getId(), mapping);
        userMappingMapper.updateById(mapping);
    }

    @Override
    public void deleteUserMapping(Long id) {
        validateExists(id);
        userMappingMapper.deleteById(id);
    }

    @Override
    public TencentImUserMappingDO getUserMapping(Long id) {
        return userMappingMapper.selectById(id);
    }

    @Override
    public PageResult<TencentImUserMappingDO> getUserMappingPage(TencentImUserMappingPageReqVO pageReqVO) {
        return userMappingMapper.selectPage(pageReqVO);
    }

    @Override
    public TencentImUserMappingDO getByOaUser(Long tenantId, Long oaUserId) {
        return userMappingMapper.selectByOaUser(normalizeTenantId(tenantId), oaUserId);
    }

    @Override
    public TencentImUserMappingDO getActiveByOaUser(Long tenantId, Long oaUserId) {
        TencentImUserMappingDO mapping = userMappingMapper.selectActiveByOaUser(normalizeTenantId(tenantId), oaUserId);
        return mapping != null ? mapping : userMappingMapper.selectActiveByOaUser(oaUserId);
    }

    @Override
    public TencentImUserMappingDO getActiveByOaUser(Long oaUserId) {
        return userMappingMapper.selectActiveByOaUser(oaUserId);
    }

    @Override
    public TencentImUserMappingDO createDefaultMapping(Long tenantId, Long oaUserId, String oaUsername) {
        TencentImUserMappingCreateReqVO createReqVO = new TencentImUserMappingCreateReqVO();
        createReqVO.setTenantId(normalizeTenantId(tenantId));
        createReqVO.setOaUserId(oaUserId);
        createReqVO.setOaUsername(oaUsername);
        createReqVO.setOrdersysUserPrefix(properties.getDefaultOrdersysUserPrefix());
        createReqVO.setOrdersysUsername(String.valueOf(oaUserId));
        createReqVO.setFixedPrefix(properties.getFixedPrefix());
        createReqVO.setStatus(STATUS_ENABLED);
        createReqVO.setRemark("Auto-created by OA Tencent IM wrapper");
        Long id = createUserMapping(createReqVO);
        return userMappingMapper.selectById(id);
    }

    @Override
    public List<TencentImUserMappingDO> getActiveContactList(
            Long tenantId, Long excludeOaUserId, String keyword, Integer limit) {
        return userMappingMapper.selectActiveContactList(
                normalizeTenantId(tenantId),
                excludeOaUserId,
                keyword,
                normalizeLimit(limit));
    }

    @Override
    public String composeImUserId(String fixedPrefix, String ordersysUserPrefix, String ordersysUsername) {
        String username = requiredTrim(ordersysUsername);
        String prefix = trimToNull(ordersysUserPrefix);
        String usernameWithFixedPrefix = StrUtil.nullToEmpty(fixedPrefix).trim() + username;
        return prefix == null ? usernameWithFixedPrefix : prefix + "_" + usernameWithFixedPrefix;
    }

    private void validateUnique(Long id, TencentImUserMappingDO mapping) {
        TencentImUserMappingDO sameOaUser = userMappingMapper.selectByOaUser(mapping.getTenantId(), mapping.getOaUserId());
        if (sameOaUser != null && !sameOaUser.getId().equals(id)) {
            throw exception(TENCENT_IM_MAPPING_EXISTS);
        }

        TencentImUserMappingDO sameImUser = userMappingMapper.selectByImUserId(mapping.getImUserId());
        if (sameImUser != null && !sameImUser.getId().equals(id)) {
            throw exception(TENCENT_IM_MAPPING_EXISTS);
        }
    }

    private TencentImUserMappingDO validateExists(Long id) {
        TencentImUserMappingDO mapping = userMappingMapper.selectById(id);
        if (mapping == null) {
            throw exception(TENCENT_IM_MAPPING_NOT_EXISTS);
        }
        return mapping;
    }

    private String resolveImUserId(String imUserId, TencentImUserMappingDO mapping) {
        String trimmed = trimToNull(imUserId);
        if (trimmed != null) {
            return trimmed;
        }
        return composeImUserId(
                mapping.getFixedPrefix(),
                mapping.getOrdersysUserPrefix(),
                mapping.getOrdersysUsername());
    }

    private String defaultOrdersysPrefix(String value) {
        String trimmed = trimToNull(value);
        if (trimmed != null) {
            return trimmed;
        }
        String defaultValue = trimToNull(properties.getDefaultOrdersysUserPrefix());
        return defaultValue == null ? "EMPLOYEE" : defaultValue;
    }

    private String defaultFixedPrefix(String value) {
        String trimmed = trimToNull(value);
        if (trimmed != null) {
            return trimmed;
        }
        return StrUtil.nullToEmpty(properties.getFixedPrefix()).trim();
    }

    private String requiredTrim(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw exception(TENCENT_IM_MAPPING_INVALID);
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }

    private Long normalizeTenantId(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return 200;
        }
        return Math.max(1, Math.min(limit, 500));
    }
}
