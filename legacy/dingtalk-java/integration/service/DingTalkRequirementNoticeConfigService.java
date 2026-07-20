package com.kyx.service.hr.integration.dingtalk.service;

import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

@Service
public class DingTalkRequirementNoticeConfigService {

    public static final String SCENE_APPROVAL_TODO = "approval-todo";
    public static final String SCENE_ASSIGNED_DEV = "assigned-dev";
    public static final String SCENE_COMMENT_REMIND = "comment-remind";
    public static final String SCENE_BPM_COPY = "bpm-copy";

    private static final String ENABLED_KEY_PREFIX = "hr:dingtalk:requirement-notice:enabled:";
    private static final String SCENE_ENABLED_KEY_PREFIX = "hr:dingtalk:requirement-notice:scene-enabled:";
    private static final String DEFAULT_TENANT_KEY = "default";
    private static final List<SceneConfig> DEFAULT_SCENES = Arrays.asList(
            new SceneConfig(SCENE_APPROVAL_TODO, "BPM待办通知",
                    "BPM 流程节点流转到当前处理人或候选确认人时，发送钉钉通知。", true),
            new SceneConfig(SCENE_ASSIGNED_DEV, "开发任务通知",
                    "需求最终落到开发负责人时，给开发负责人发送钉钉通知。", true),
            new SceneConfig(SCENE_COMMENT_REMIND, "沟通提醒通知",
                    "需求沟通记录选择提醒对象后，给该提醒对象发送钉钉通知。", true),
            new SceneConfig(SCENE_BPM_COPY, "BPM知会通知",
                    "BPM 流程节点知会或抄送到用户时，发送钉钉通知。", true)
    );

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public boolean isEnabled() {
        return Boolean.parseBoolean(stringRedisTemplate.opsForValue().get(resolveEnabledKey()));
    }

    public boolean setEnabled(boolean enabled) {
        stringRedisTemplate.opsForValue().set(resolveEnabledKey(), String.valueOf(enabled));
        return enabled;
    }

    public boolean isSceneEnabled(String scene) {
        return isEnabled() && getSceneEnabled(scene);
    }

    public boolean setSceneEnabled(String scene, boolean enabled) {
        validateScene(scene);
        stringRedisTemplate.opsForValue().set(resolveSceneEnabledKey(scene), String.valueOf(enabled));
        return enabled;
    }

    public List<SceneConfig> getSceneConfigs() {
        return DEFAULT_SCENES.stream().map(this::withCurrentEnabled).collect(java.util.stream.Collectors.toList());
    }

    public boolean isKnownScene(String scene) {
        if (!StringUtils.hasText(scene)) {
            return false;
        }
        return DEFAULT_SCENES.stream().anyMatch(item -> item.getScene().equals(scene.trim()));
    }

    private String resolveEnabledKey() {
        return ENABLED_KEY_PREFIX + currentTenantKey();
    }

    private String resolveSceneEnabledKey(String scene) {
        return SCENE_ENABLED_KEY_PREFIX + currentTenantKey() + ":" + scene.trim();
    }

    private boolean getSceneEnabled(String scene) {
        validateScene(scene);
        String value = stringRedisTemplate.opsForValue().get(resolveSceneEnabledKey(scene));
        return value == null || Boolean.parseBoolean(value);
    }

    private SceneConfig withCurrentEnabled(SceneConfig source) {
        return new SceneConfig(source.getScene(), source.getName(), source.getDescription(), getSceneEnabled(source.getScene()));
    }

    private void validateScene(String scene) {
        if (!isKnownScene(scene)) {
            throw new IllegalArgumentException("未知的钉钉通知场景：" + scene);
        }
    }

    private String currentTenantKey() {
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId == null ? DEFAULT_TENANT_KEY : tenantId.toString();
    }

    public static class SceneConfig {
        private final String scene;
        private final String name;
        private final String description;
        private final Boolean enabled;

        public SceneConfig(String scene, String name, String description, Boolean enabled) {
            this.scene = scene;
            this.name = name;
            this.description = description;
            this.enabled = enabled;
        }

        public String getScene() {
            return scene;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public Boolean getEnabled() {
            return enabled;
        }
    }
}
