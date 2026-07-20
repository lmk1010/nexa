package com.kyx.service.business.service.notice;

import cn.hutool.core.util.StrUtil;
import com.kyx.service.business.enums.notice.NoticeReceiverTypeEnum;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.business.enums.ErrorCodeConstants.NOTICE_RECEIVER_TYPE_INVALID;
import static com.kyx.service.business.enums.ErrorCodeConstants.NOTICE_RECEIVER_USER_REQUIRED;

public final class NoticeReceiverSupport {

    private NoticeReceiverSupport() {
    }

    public static String normalizeReceiverType(String receiverType) {
        if (StrUtil.isBlank(receiverType)) {
            return NoticeReceiverTypeEnum.ALL.getType();
        }
        String normalized = receiverType.trim().toUpperCase(Locale.ROOT);
        if (NoticeReceiverTypeEnum.ALL.getType().equals(normalized)
                || NoticeReceiverTypeEnum.USER.getType().equals(normalized)) {
            return normalized;
        }
        throw exception(NOTICE_RECEIVER_TYPE_INVALID);
    }

    public static void validateReceiver(String receiverType, Collection<Long> receiverUserIds) {
        if (isUserReceiver(receiverType) && normalizeUserIds(receiverUserIds).isEmpty()) {
            throw exception(NOTICE_RECEIVER_USER_REQUIRED);
        }
    }

    public static boolean isUserReceiver(String receiverType) {
        return NoticeReceiverTypeEnum.USER.getType().equals(normalizeReceiverType(receiverType));
    }

    public static List<Long> normalizeUserIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return userIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .collect(Collectors.toList());
    }

    public static List<Long> parseUserIds(String userIds) {
        if (StrUtil.isBlank(userIds)) {
            return Collections.emptyList();
        }
        return StrUtil.splitTrim(userIds, ',').stream()
                .map(NoticeReceiverSupport::parseLong)
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .collect(Collectors.toList());
    }

    public static String joinUserIds(Collection<Long> userIds) {
        List<Long> normalized = normalizeUserIds(userIds);
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private static Long parseLong(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

}
