package com.kyx.service.im.enums;

import com.kyx.foundation.common.exception.ErrorCode;

/**
 * IM 错误码枚举类
 *
 * IM 错误码区间 1-000-000-000 ~ 1-000-999-999
 *
 * @author MK
 */
public interface ErrorCodeConstants {

    // ========== 邀请码相关 1-000-001-000 ==========
    ErrorCode INVITE_CODE_NOT_EXISTS = new ErrorCode(1_000_001_000, "邀请码不存在");
    ErrorCode INVITE_CODE_EXISTS = new ErrorCode(1_000_001_001, "邀请码已存在");
    ErrorCode INVITE_CODE_INVALID = new ErrorCode(1_000_001_002, "邀请码无效");
    ErrorCode INVITE_CODE_EXPIRED = new ErrorCode(1_000_001_003, "邀请码已过期");
    ErrorCode INVITE_CODE_USED_UP = new ErrorCode(1_000_001_004, "邀请码已用完");

    // ========== Tencent IM related 1-000-002-000 ==========
    ErrorCode TENCENT_IM_CONFIG_INVALID = new ErrorCode(1_000_002_000, "Tencent IM config is invalid");
    ErrorCode TENCENT_IM_MAPPING_NOT_EXISTS = new ErrorCode(1_000_002_001, "Tencent IM user mapping does not exist");
    ErrorCode TENCENT_IM_MAPPING_EXISTS = new ErrorCode(1_000_002_002, "Tencent IM user mapping already exists");
    ErrorCode TENCENT_IM_MAPPING_INVALID = new ErrorCode(1_000_002_003, "Tencent IM user mapping is invalid");

}
