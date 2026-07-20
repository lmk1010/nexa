package com.kyx.service.bpm.api.task.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BpmCopyNoticeResendRespDTO {

    private Integer scannedCount;

    private Integer runningCount;

    private Integer receiverCount;

    private Integer taskScannedCount;

    private Integer taskReceiverCount;

    private Integer copyScannedCount;

    private Integer copyRunningCount;

    private Integer copyReceiverCount;

}
