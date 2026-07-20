export const DINGTALK_SOCIAL_TYPE = 20;
export const PASSWORD_LOGIN_MODE = 'password';
export const QRCODE_LOGIN_MODE = 'qrcode';

export function isDingTalkBrowser() {
  return /dingtalk/i.test(navigator.userAgent);
}

export function isMobileBrowser() {
  return /android|iphone|ipad|ipod|mobile|windows phone|harmonyos/i.test(
    navigator.userAgent.toLowerCase(),
  );
}

function buildDingTalkClientUrl(targetUrl: string) {
  return `dingtalk://dingtalkclient/page/link?url=${encodeURIComponent(
    targetUrl,
  )}`;
}

export function openDingTalkAuthorizeUrl(authorizeUrl: string) {
  if (isMobileBrowser() && !isDingTalkBrowser()) {
    window.location.href = buildDingTalkClientUrl(authorizeUrl);
    return;
  }
  window.location.href = authorizeUrl;
}
