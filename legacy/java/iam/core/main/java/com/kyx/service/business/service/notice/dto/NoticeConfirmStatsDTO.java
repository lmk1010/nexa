package com.kyx.service.business.service.notice.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class NoticeConfirmStatsDTO {

    private Long noticeId;

    private Integer confirmTotalCount = 0;

    private Integer confirmedCount = 0;

    private Integer unconfirmedCount = 0;

    private Integer overdueCount = 0;

}
