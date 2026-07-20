package com.kyx.service.business.service.notice;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.business.controller.admin.notice.vo.NoticePageReqVO;
import com.kyx.service.business.controller.admin.notice.vo.NoticeSaveReqVO;
import com.kyx.service.business.dal.dataobject.notice.NoticeDO;
import com.kyx.service.business.dal.mysql.notice.NoticeMapper;
import com.kyx.service.business.service.user.AdminUserService;
import com.google.common.annotations.VisibleForTesting;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.business.enums.ErrorCodeConstants.NOTICE_NOT_FOUND;

/**
 * 通知公告 Service 实现类
 *
 * @author MK
 */
@Service
public class NoticeServiceImpl implements NoticeService {

    @Resource
    private NoticeMapper noticeMapper;
    @Resource
    private NoticeConfirmService noticeConfirmService;
    @Resource
    private AdminUserService adminUserService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createNotice(NoticeSaveReqVO createReqVO) {
        NoticeDO notice = buildNoticeDO(createReqVO);
        noticeMapper.insert(notice);
        if (Boolean.TRUE.equals(notice.getNeedConfirm())) {
            noticeConfirmService.ensureNoticeConfirmReceipts(notice.getId());
        }
        return notice.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNotice(NoticeSaveReqVO updateReqVO) {
        // 校验是否存在
        validateNoticeExists(updateReqVO.getId());
        // 更新通知公告
        NoticeDO updateObj = buildNoticeDO(updateReqVO);
        noticeMapper.updateById(updateObj);
        if (Boolean.TRUE.equals(updateObj.getNeedConfirm())) {
            noticeConfirmService.ensureNoticeConfirmReceipts(updateObj.getId());
        } else {
            noticeConfirmService.deleteNoticeConfirmReceipts(updateObj.getId());
        }
    }

    @Override
    public void deleteNotice(Long id) {
        // 校验是否存在
        validateNoticeExists(id);
        // 删除通知公告
        noticeMapper.deleteById(id);
    }

    @Override
    public PageResult<NoticeDO> getNoticePage(NoticePageReqVO reqVO) {
        return noticeMapper.selectPage(reqVO);
    }

    @Override
    public NoticeDO getNotice(Long id) {
        return noticeMapper.selectById(id);
    }

    private NoticeDO buildNoticeDO(NoticeSaveReqVO reqVO) {
        String receiverType = NoticeReceiverSupport.normalizeReceiverType(reqVO.getReceiverType());
        List<Long> receiverUserIds = NoticeReceiverSupport.normalizeUserIds(reqVO.getReceiverUserIds());
        NoticeReceiverSupport.validateReceiver(receiverType, receiverUserIds);
        if (NoticeReceiverSupport.isUserReceiver(receiverType)) {
            adminUserService.validateUserList(receiverUserIds);
        }
        NoticeDO notice = new NoticeDO();
        notice.setId(reqVO.getId());
        notice.setTitle(reqVO.getTitle());
        notice.setType(reqVO.getType());
        notice.setContent(reqVO.getContent());
        notice.setStatus(reqVO.getStatus());
        notice.setReceiverType(receiverType);
        notice.setReceiverUserIds(NoticeReceiverSupport.isUserReceiver(receiverType)
                ? NoticeReceiverSupport.joinUserIds(receiverUserIds) : null);
        notice.setNeedConfirm(Boolean.TRUE.equals(reqVO.getNeedConfirm()));
        notice.setConfirmDeadline(Boolean.TRUE.equals(reqVO.getNeedConfirm()) ? reqVO.getConfirmDeadline() : null);
        return notice;
    }

    @VisibleForTesting
    public void validateNoticeExists(Long id) {
        if (id == null) {
            return;
        }
        NoticeDO notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw exception(NOTICE_NOT_FOUND);
        }
    }

}
