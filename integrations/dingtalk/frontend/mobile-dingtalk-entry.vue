<script lang="ts" setup>
defineOptions({ name: 'MobileDingTalkEntry' });

defineProps<{
  actionLoading?: boolean;
  errorText?: string;
  inDingTalk?: boolean;
  loading?: boolean;
  statusText?: string;
}>();

const emit = defineEmits<{
  open: [];
  password: [];
}>();
</script>

<template>
  <div class="mobile-dingtalk-entry">
    <div class="mobile-dingtalk-hero">
      <div class="mobile-dingtalk-badge">
        {{ inDingTalk ? '当前已在钉钉内' : '手机端一键登录' }}
      </div>
      <h2 class="mobile-dingtalk-title">钉钉登录</h2>
      <p class="mobile-dingtalk-subtitle">
        手机访问默认使用钉钉授权登录，点击下方按钮即可直接调用钉钉 App
      </p>
    </div>

    <div class="mobile-dingtalk-panel">
      <div v-if="loading" class="mobile-dingtalk-status">
        正在准备钉钉授权...
      </div>
      <div v-else class="mobile-dingtalk-status">
        {{
          errorText ||
          statusText ||
          (inDingTalk
            ? '当前已经处于钉钉环境中，点击后将直接进入授权流程'
            : '点击后会优先拉起钉钉 App，未安装或拉起失败时再回退到浏览器授权')
        }}
      </div>

      <button
        class="mobile-dingtalk-primary"
        :disabled="loading || actionLoading"
        type="button"
        @click="emit('open')"
      >
        {{ actionLoading ? '正在打开钉钉...' : '一键钉钉登录' }}
      </button>

      <button
        class="mobile-dingtalk-secondary"
        type="button"
        @click="emit('password')"
      >
        用户名登录
      </button>
    </div>
  </div>
</template>

<style scoped>
.mobile-dingtalk-entry {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: 430px;
}

.mobile-dingtalk-hero {
  display: flex;
  flex-direction: column;
}

.mobile-dingtalk-badge {
  align-self: flex-start;
  background: rgb(22 119 255 / 10%);
  border-radius: 999px;
  color: rgb(22 119 255);
  font-size: 12px;
  font-weight: 600;
  padding: 7px 12px;
}

.mobile-dingtalk-title {
  font-size: 30px;
  font-weight: 700;
  line-height: 1.25;
  margin-top: 14px;
}

.mobile-dingtalk-subtitle {
  color: rgb(107 114 128);
  font-size: 14px;
  line-height: 1.7;
  margin-top: 8px;
}

.mobile-dingtalk-panel {
  background: linear-gradient(
    180deg,
    rgb(248 250 252) 0%,
    rgb(255 255 255) 100%
  );
  border: 1px solid rgb(229 231 235);
  border-radius: 16px;
  box-shadow: 0 18px 40px rgb(15 23 42 / 6%);
  display: flex;
  flex: 1;
  flex-direction: column;
  justify-content: center;
  padding: 24px 18px;
}

.mobile-dingtalk-status {
  color: rgb(71 85 105);
  font-size: 14px;
  line-height: 1.75;
  text-align: center;
}

.mobile-dingtalk-primary,
.mobile-dingtalk-secondary {
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 600;
  margin-top: 14px;
  min-height: 44px;
  width: 100%;
}

.mobile-dingtalk-primary {
  background: rgb(22 119 255);
  color: #fff;
}

.mobile-dingtalk-primary:disabled {
  cursor: wait;
  opacity: 0.7;
}

.mobile-dingtalk-secondary {
  background: rgb(241 245 249);
  color: rgb(51 65 85);
}
</style>
