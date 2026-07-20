package com.kyx.service.business.api.notice;

import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.business.api.notice.dto.NoticeCreateReqDTO;
import com.kyx.service.business.controller.admin.notice.vo.NoticeSaveReqVO;
import com.kyx.service.business.service.notice.NoticeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@RestController
@Validated
public class NoticeApiImpl implements NoticeApi {

    @Resource
    private NoticeService noticeService;

    @Override
    public CommonResult<Long> createNotice(NoticeCreateReqDTO reqDTO) {
        NoticeSaveReqVO saveReqVO = BeanUtils.toBean(reqDTO, NoticeSaveReqVO.class);
        if (saveReqVO.getStatus() == null) {
            saveReqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        }
        Long id = noticeService.createNotice(saveReqVO);
        return success(id);
    }

}
