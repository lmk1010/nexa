package com.kyx.service.hr.integration.dingtalk.service;

import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.service.hr.config.DingTalkProperties;
import lombok.Data;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class DingTalkSyncConfigService {

    private static final String ATTENDANCE_ENABLED_KEY_PREFIX = "hr:dingtalk:sync:attendance-enabled:";
    private static final String LEAVE_ENABLED_KEY_PREFIX = "hr:dingtalk:sync:leave-enabled:";
    private static final String DEFAULT_TENANT_KEY = "default";

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private DingTalkProperties dingTalkProperties;

    public SyncConfig getConfig() {
        SyncConfig config = new SyncConfig();
        config.setAttendanceEnabled(isAttendanceEnabled());
        config.setLeaveEnabled(isLeaveEnabled());
        return config;
    }

    public boolean isAttendanceEnabled() {
        return readBoolean(resolveAttendanceEnabledKey(), defaultAttendanceEnabled());
    }

    public boolean setAttendanceEnabled(boolean enabled) {
        stringRedisTemplate.opsForValue().set(resolveAttendanceEnabledKey(), String.valueOf(enabled));
        return enabled;
    }

    public boolean isLeaveEnabled() {
        return readBoolean(resolveLeaveEnabledKey(), defaultLeaveEnabled());
    }

    public boolean setLeaveEnabled(boolean enabled) {
        stringRedisTemplate.opsForValue().set(resolveLeaveEnabledKey(), String.valueOf(enabled));
        return enabled;
    }

    private boolean readBoolean(String key, boolean defaultValue) {
        String value = stringRedisTemplate.opsForValue().get(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }

    private boolean defaultAttendanceEnabled() {
        return dingTalkProperties.getSync() == null
                || !Boolean.FALSE.equals(dingTalkProperties.getSync().getAttendanceEnabled());
    }

    private boolean defaultLeaveEnabled() {
        return dingTalkProperties.getSync() == null
                || !Boolean.FALSE.equals(dingTalkProperties.getSync().getLeaveEnabled());
    }

    private String resolveAttendanceEnabledKey() {
        return ATTENDANCE_ENABLED_KEY_PREFIX + currentTenantKey();
    }

    private String resolveLeaveEnabledKey() {
        return LEAVE_ENABLED_KEY_PREFIX + currentTenantKey();
    }

    private String currentTenantKey() {
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId == null ? DEFAULT_TENANT_KEY : tenantId.toString();
    }

    @Data
    public static class SyncConfig {
        private Boolean attendanceEnabled;
        private Boolean leaveEnabled;
    }

}
