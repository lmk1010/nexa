package com.kyx.service.business.controller.admin.work;

import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.common.enums.UserTypeEnum;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.date.DateUtils;
import com.kyx.service.business.controller.admin.notify.vo.message.NotifyMessageMyPageReqVO;
import com.kyx.service.business.controller.admin.work.vo.WorkNoticePageReqVO;
import com.kyx.service.business.controller.admin.work.vo.WorkNoticeRespVO;
import com.kyx.service.business.dal.dataobject.notice.NoticeConfirmDO;
import com.kyx.service.business.dal.dataobject.notice.NoticeDO;
import com.kyx.service.business.dal.dataobject.notify.NotifyMessageDO;
import com.kyx.service.business.dal.dataobject.notify.NotifyTemplateDO;
import com.kyx.service.business.dal.mysql.notice.NoticeMapper;
import com.kyx.service.business.dal.mysql.notify.NotifyTemplateMapper;
import com.kyx.service.business.enums.notice.NoticeTypeEnum;
import com.kyx.service.business.enums.notify.NotifyTemplateTypeEnum;
import com.kyx.service.business.service.notify.NotifyMessageService;
import com.kyx.service.business.service.notice.NoticeConfirmService;
import com.kyx.service.business.service.notice.NoticeReadService;
import com.kyx.service.business.service.notice.dto.NoticeConfirmStatsDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.kyx.foundation.common.pojo.CommonResult.success;
import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "Admin - Work Notice")
@RestController
@RequestMapping("/system/work/notice")
@Validated
public class WorkNoticeController {

    private static final String TYPE_SYSTEM = "system";
    private static final String TYPE_MEETING = "meeting";
    private static final String TYPE_TASK = "task";
    private static final String TYPE_APPROVAL = "approval";
    private static final String TYPE_ANNOUNCE = "announce";

    private static final Map<String, String> TEMPLATE_CODE_TYPE_MAP = buildTemplateCodeTypeMap();

    private static final String[] MEETING_CODE_PREFIXES = {"meeting", "meeting_", "meeting-"};
    private static final String[] TASK_CODE_PREFIXES = {"task", "task_", "task-", "bpm_task_"};
    private static final String[] APPROVAL_CODE_PREFIXES = {"approve", "approve_", "approval", "approval_", "bpm_process_instance_"};

    @Resource
    private NotifyMessageService notifyMessageService;
    @Resource
    private NotifyTemplateMapper notifyTemplateMapper;
    @Resource
    private NoticeMapper noticeMapper;
    @Resource
    private NoticeReadService noticeReadService;
    @Resource
    private NoticeConfirmService noticeConfirmService;

    @GetMapping("/page")
    @Operation(summary = "Get work notice page")
    public CommonResult<PageResult<WorkNoticeRespVO>> getWorkNoticePage(@Valid WorkNoticePageReqVO reqVO) {
        int fetchSize = reqVO.getPageNo() * reqVO.getPageSize();
        NotifyMessageMyPageReqVO pageReqVO = new NotifyMessageMyPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(fetchSize);
        pageReqVO.setReadStatus(reqVO.getReadStatus());
        pageReqVO.setCreateTime(reqVO.getCreateTime());

        Long userId = getLoginUserId();
        Integer userType = UserTypeEnum.ADMIN.getValue();
        PageResult<NotifyMessageDO> messagePage = notifyMessageService.getMyMyNotifyMessagePage(pageReqVO, userId, userType);
        List<NoticeDO> notices = noticeMapper.selectWorkNoticeList(userId, userType, reqVO.getReadStatus(),
                reqVO.getCreateTime(), fetchSize);
        Long noticeTotal = noticeMapper.selectWorkNoticeCount(userId, userType, reqVO.getReadStatus(), reqVO.getCreateTime());
        List<Long> noticeIds = notices.stream().map(NoticeDO::getId).collect(Collectors.toList());

        Set<Long> readNoticeIds = Collections.emptySet();
        if (reqVO.getReadStatus() == null && !notices.isEmpty()) {
            readNoticeIds = noticeReadService.getReadNoticeIds(userId, userType,
                    noticeIds);
        }
        Map<Long, NoticeConfirmDO> confirmMap = noticeConfirmService.getUserConfirmMap(noticeIds, userId, userType);
        Map<Long, NoticeConfirmStatsDTO> statsMap = noticeConfirmService.getStatsMap(noticeIds);

        List<WorkNoticeRespVO> list = new ArrayList<>();
        list.addAll(convertMessageList(messagePage.getList()));
        list.addAll(convertNoticeList(notices, reqVO.getReadStatus(), readNoticeIds, confirmMap, statsMap));
        list.sort(workNoticeComparator());

        int fromIndex = Math.min((reqVO.getPageNo() - 1) * reqVO.getPageSize(), list.size());
        int toIndex = Math.min(fromIndex + reqVO.getPageSize(), list.size());
        List<WorkNoticeRespVO> pageList = list.subList(fromIndex, toIndex);
        long total = messagePage.getTotal() + (noticeTotal == null ? 0L : noticeTotal);
        return success(new PageResult<>(pageList, total));
    }

    @PutMapping("/read")
    @Operation(summary = "Mark work notices as read")
    @Parameter(name = "ids", description = "IDs", required = true, example = "1024,2048")
    public CommonResult<Boolean> updateWorkNoticeRead(@RequestParam("ids") List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return success(Boolean.TRUE);
        }
        List<Long> messageIds = new ArrayList<>();
        List<Long> noticeIds = new ArrayList<>();
        for (Long id : ids) {
            if (id == null) {
                continue;
            }
            if (id < 0) {
                noticeIds.add(Math.abs(id));
            } else {
                messageIds.add(id);
            }
        }
        Long userId = getLoginUserId();
        Integer userType = UserTypeEnum.ADMIN.getValue();
        if (!messageIds.isEmpty()) {
            notifyMessageService.updateNotifyMessageRead(messageIds, userId, userType);
        }
        if (!noticeIds.isEmpty()) {
            noticeReadService.markRead(noticeIds, userId, userType);
        }
        return success(Boolean.TRUE);
    }

    @PutMapping("/confirm")
    @Operation(summary = "Confirm work notice")
    @Parameter(name = "id", description = "Work notice ID", required = true, example = "-1024")
    public CommonResult<Boolean> confirmWorkNotice(@RequestParam("id") Long id) {
        Long noticeId = resolveSystemNoticeId(id);
        if (noticeId == null) {
            return success(Boolean.FALSE);
        }
        NoticeDO notice = noticeMapper.selectById(noticeId);
        if (notice == null || !Boolean.TRUE.equals(notice.getNeedConfirm())) {
            return success(Boolean.FALSE);
        }
        Long userId = getLoginUserId();
        Integer userType = UserTypeEnum.ADMIN.getValue();
        if (noticeMapper.selectVisibleNoticeIds(userId, userType, Collections.singletonList(noticeId)).isEmpty()) {
            return success(Boolean.FALSE);
        }
        noticeConfirmService.confirmNotice(noticeId, userId, userType);
        noticeReadService.markRead(Collections.singletonList(noticeId), userId, userType);
        return success(Boolean.TRUE);
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all work notices as read")
    public CommonResult<Boolean> updateAllWorkNoticeRead() {
        Long userId = getLoginUserId();
        Integer userType = UserTypeEnum.ADMIN.getValue();
        notifyMessageService.updateAllNotifyMessageRead(userId, userType);
        noticeReadService.markAllRead(userId, userType);
        return success(Boolean.TRUE);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread work notice count")
    public CommonResult<Long> getUnreadWorkNoticeCount() {
        Long userId = getLoginUserId();
        Integer userType = UserTypeEnum.ADMIN.getValue();
        long notifyUnread = notifyMessageService.getUnreadNotifyMessageCount(userId, userType);
        Long noticeUnread = noticeMapper.selectWorkNoticeCount(userId, userType, false, null);
        long noticeUnreadCount = noticeUnread == null ? 0L : noticeUnread;
        return success(notifyUnread + noticeUnreadCount);
    }

    private List<WorkNoticeRespVO> convertMessageList(List<NotifyMessageDO> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, NotifyTemplateDO> templateMap = loadTemplateMap(list);
        return list.stream()
                .map(item -> convert(item, templateMap.get(item.getTemplateId())))
                .collect(Collectors.toList());
    }

    private List<WorkNoticeRespVO> convertNoticeList(List<NoticeDO> notices, Boolean readStatus, Set<Long> readNoticeIds,
                                                     Map<Long, NoticeConfirmDO> confirmMap,
                                                     Map<Long, NoticeConfirmStatsDTO> statsMap) {
        if (notices == null || notices.isEmpty()) {
            return Collections.emptyList();
        }
        return notices.stream()
                .map(notice -> convert(notice, resolveNoticeReadStatus(notice, readStatus, readNoticeIds),
                        confirmMap.get(notice.getId()), statsMap.get(notice.getId())))
                .collect(Collectors.toList());
    }

    private Map<Long, NotifyTemplateDO> loadTemplateMap(List<NotifyMessageDO> list) {
        List<Long> templateIds = list.stream()
                .map(NotifyMessageDO::getTemplateId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (templateIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, NotifyTemplateDO> map = new HashMap<>();
        notifyTemplateMapper.selectBatchIds(templateIds)
                .forEach(template -> map.put(template.getId(), template));
        return map;
    }

    private WorkNoticeRespVO convert(NotifyMessageDO message, NotifyTemplateDO template) {
        WorkNoticeRespVO respVO = new WorkNoticeRespVO();
        respVO.setId(message.getId());
        respVO.setTitle(buildTitle(message, template));
        respVO.setSummary(buildSummary(message));
        respVO.setRedirectUrl(resolveRedirectUrl(message));
        respVO.setType(resolveType(message));
        respVO.setSender(StrUtil.blankToDefault(message.getTemplateNickname(), "System"));
        respVO.setReadStatus(Boolean.TRUE.equals(message.getReadStatus()));
        respVO.setNeedConfirm(Boolean.FALSE);
        respVO.setConfirmed(Boolean.FALSE);
        fillConfirmStats(respVO, null);
        respVO.setCreateTime(toLocalDateTime(message.getCreateTime()));
        return respVO;
    }

    private WorkNoticeRespVO convert(NoticeDO notice, boolean readStatus, NoticeConfirmDO confirm,
                                     NoticeConfirmStatsDTO stats) {
        WorkNoticeRespVO respVO = new WorkNoticeRespVO();
        // Use negative IDs for system notices to avoid collisions with notify messages.
        respVO.setId(-notice.getId());
        respVO.setTitle(StrUtil.blankToDefault(notice.getTitle(), "Notice"));
        respVO.setSummary(buildNoticeSummary(notice));
        respVO.setType(resolveNoticeType(notice));
        respVO.setSender(StrUtil.blankToDefault(notice.getCreator(), "System"));
        respVO.setReadStatus(readStatus);
        respVO.setNeedConfirm(Boolean.TRUE.equals(notice.getNeedConfirm()));
        respVO.setConfirmed(confirm != null && confirm.getConfirmTime() != null);
        respVO.setConfirmTime(confirm == null ? null : confirm.getConfirmTime());
        respVO.setConfirmDeadline(notice.getConfirmDeadline());
        fillConfirmStats(respVO, stats);
        respVO.setCreateTime(toLocalDateTime(notice.getCreateTime()));
        return respVO;
    }

    private static void fillConfirmStats(WorkNoticeRespVO respVO, NoticeConfirmStatsDTO stats) {
        if (stats == null) {
            respVO.setConfirmTotalCount(0);
            respVO.setConfirmedCount(0);
            respVO.setUnconfirmedCount(0);
            respVO.setOverdueCount(0);
            return;
        }
        respVO.setConfirmTotalCount(stats.getConfirmTotalCount());
        respVO.setConfirmedCount(stats.getConfirmedCount());
        respVO.setUnconfirmedCount(stats.getUnconfirmedCount());
        respVO.setOverdueCount(stats.getOverdueCount());
    }

    private static String buildTitle(NotifyMessageDO message, NotifyTemplateDO template) {
        if (template != null && StrUtil.isNotBlank(template.getName())) {
            return template.getName();
        }
        if (StrUtil.isNotBlank(message.getTemplateCode())) {
            return message.getTemplateCode();
        }
        String content = StrUtil.trim(message.getTemplateContent());
        if (StrUtil.isNotBlank(content)) {
            return trimContent(content, 24);
        }
        return "Notification";
    }

    private static String buildSummary(NotifyMessageDO message) {
        String content = StrUtil.trim(message.getTemplateContent());
        if (StrUtil.isBlank(content)) {
            return "";
        }
        return trimContent(content, 140);
    }

    private static String resolveRedirectUrl(NotifyMessageDO message) {
        Map<String, Object> templateParams = message.getTemplateParams();
        if (templateParams == null || templateParams.isEmpty()) {
            return null;
        }
        for (String key : new String[]{"redirectUrl", "detailUrl", "url"}) {
            String url = normalizeRedirectUrl(templateParams.get(key));
            if (StrUtil.isNotBlank(url)) {
                return url;
            }
        }
        return null;
    }

    private static String normalizeRedirectUrl(Object value) {
        if (value == null) {
            return null;
        }
        String url = StrUtil.trim(String.valueOf(value));
        return StrUtil.isBlank(url) ? null : url;
    }

    private static String buildNoticeSummary(NoticeDO notice) {
        String content = StrUtil.trim(notice.getContent());
        if (StrUtil.isBlank(content)) {
            return "";
        }
        return trimContent(content, 140);
    }

    private static String trimContent(String content, int maxLength) {
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }

    private static LocalDateTime toLocalDateTime(LocalDateTime time) {
        return time;
    }

    private static LocalDateTime toLocalDateTime(Date time) {
        return DateUtils.of(time);
    }

    private static String resolveType(NotifyMessageDO message) {
        String code = StrUtil.trimToEmpty(message.getTemplateCode()).toLowerCase(Locale.ROOT);
        if (StrUtil.isNotBlank(code)) {
            String mappedType = TEMPLATE_CODE_TYPE_MAP.get(code);
            if (mappedType != null) {
                return mappedType;
            }
            if (startsWithAny(code, MEETING_CODE_PREFIXES)) {
                return TYPE_MEETING;
            }
            if (startsWithAny(code, TASK_CODE_PREFIXES)) {
                return TYPE_TASK;
            }
            if (startsWithAny(code, APPROVAL_CODE_PREFIXES)) {
                return TYPE_APPROVAL;
            }
        }
        if (Objects.equals(message.getTemplateType(), NotifyTemplateTypeEnum.SYSTEM_MESSAGE.getType())) {
            return TYPE_SYSTEM;
        }
        return TYPE_APPROVAL;
    }

    private static String resolveNoticeType(NoticeDO notice) {
        if (Objects.equals(notice.getType(), NoticeTypeEnum.ANNOUNCEMENT.getType())) {
            return TYPE_ANNOUNCE;
        }
        return TYPE_SYSTEM;
    }

    private static boolean resolveNoticeReadStatus(NoticeDO notice, Boolean readStatus, Set<Long> readNoticeIds) {
        if (readStatus != null) {
            return readStatus;
        }
        return readNoticeIds != null && readNoticeIds.contains(notice.getId());
    }

    private static Long resolveSystemNoticeId(Long id) {
        if (id == null) {
            return null;
        }
        return id < 0 ? Math.abs(id) : id;
    }

    private static boolean startsWithAny(String value, String[] prefixes) {
        for (String prefix : prefixes) {
            if (value.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static Comparator<WorkNoticeRespVO> workNoticeComparator() {
        return Comparator.comparing(WorkNoticeRespVO::getCreateTime,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed()
                .thenComparing(WorkNoticeRespVO::getId);
    }

    private static Map<String, String> buildTemplateCodeTypeMap() {
        Map<String, String> map = new HashMap<>();
        map.put("bpm_process_instance_approve", TYPE_APPROVAL);
        map.put("bpm_process_instance_reject", TYPE_APPROVAL);
        map.put("bpm_task_assigned", TYPE_TASK);
        map.put("bpm_task_timeout", TYPE_TASK);
        return map;
    }
}
