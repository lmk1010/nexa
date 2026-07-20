package com.kyx.service.business.enums.notice;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通知接收范围。
 */
@Getter
@AllArgsConstructor
public enum NoticeReceiverTypeEnum {

    ALL("ALL", "全员"),
    USER("USER", "指定人员");

    private final String type;
    private final String name;

}
