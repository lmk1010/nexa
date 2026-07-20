package com.kyx.service.business.controller.admin.notice;

import cn.hutool.core.lang.Assert;
import com.kyx.foundation.common.enums.UserTypeEnum;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.date.DateUtils;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.op.api.websocket.WebSocketSenderApi;
import com.kyx.service.business.controller.admin.notice.vo.NoticeConfirmDetailRespVO;
import com.kyx.service.business.controller.admin.notice.vo.NoticePageReqVO;
import com.kyx.service.business.controller.admin.notice.vo.NoticeRespVO;
import com.kyx.service.business.controller.admin.notice.vo.NoticeSaveReqVO;
import com.kyx.service.business.dal.dataobject.notice.NoticeConfirmDO;
import com.kyx.service.business.dal.dataobject.notice.NoticeDO;
import com.kyx.service.business.dal.dataobject.tenant.TenantDO;
import com.kyx.service.business.dal.dataobject.user.AdminUserDO;
import com.kyx.service.business.service.notice.NoticeConfirmService;
import com.kyx.service.business.service.notice.NoticeReceiverSupport;
import com.kyx.service.business.service.notice.NoticeService;
import com.kyx.service.business.service.notice.dto.NoticeConfirmStatsDTO;
import com.kyx.service.business.service.tenant.TenantService;
import com.kyx.service.business.service.user.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 通知公告")
@RestController
@RequestMapping("/system/notice")
@Validated
public class NoticeController {

    @Resource
    private NoticeService noticeService;
    @Resource
    private NoticeConfirmService noticeConfirmService;
    @Resource
    private AdminUserService adminUserService;

    @Resource
    private TenantService tenantService;

    @Resource
    private WebSocketSenderApi webSocketSenderApi;

    @PostMapping("/create")
    @Operation(summary = "创建通知公告")
    @PreAuthorize("@ss.hasPermission('system:notice:create')")
    public CommonResult<Long> createNotice(@Valid @RequestBody NoticeSaveReqVO createReqVO) {
        Long noticeId = noticeService.createNotice(createReqVO);
        return success(noticeId);
    }

    @PutMapping("/update")
    @Operation(summary = "修改通知公告")
    @PreAuthorize("@ss.hasPermission('system:notice:update')")
    public CommonResult<Boolean> updateNotice(@Valid @RequestBody NoticeSaveReqVO updateReqVO) {
        noticeService.updateNotice(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除通知公告")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:notice:delete')")
    public CommonResult<Boolean> deleteNotice(@RequestParam("id") Long id) {
        noticeService.deleteNotice(id);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获取通知公告列表")
    @PreAuthorize("@ss.hasPermission('system:notice:query')")
    public CommonResult<PageResult<NoticeRespVO>> getNoticePage(@Validated NoticePageReqVO pageReqVO) {
        PageResult<NoticeDO> pageResult = noticeService.getNoticePage(pageReqVO);
        List<NoticeRespVO> noticeList = pageResult.getList().stream()
                .map(this::convertNotice)
                .collect(Collectors.toList());
        PageResult<NoticeRespVO> result = new PageResult<>(noticeList, pageResult.getTotal());
        fillTenantNames(result.getList());
        fillConfirmStats(result.getList());
        return success(result);
    }

    @GetMapping("/get")
    @Operation(summary = "获得通知公告")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:notice:query')")
    public CommonResult<NoticeRespVO> getNotice(@RequestParam("id") Long id) {
        NoticeDO notice = noticeService.getNotice(id);
        NoticeRespVO respVO = convertNotice(notice);
        if (respVO != null) {
            fillTenantNames(java.util.Collections.singletonList(respVO));
            fillConfirmStats(java.util.Collections.singletonList(respVO));
        }
        return success(respVO);
    }

    @GetMapping("/confirm-detail")
    @Operation(summary = "获取通知确认明细")
    @Parameter(name = "id", description = "公告编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:notice:query')")
    public CommonResult<List<NoticeConfirmDetailRespVO>> getConfirmDetail(@RequestParam("id") Long id) {
        NoticeDO notice = noticeService.getNotice(id);
        Assert.notNull(notice, "公告不能为空");
        List<NoticeConfirmDO> detailList = noticeConfirmService.getConfirmDetailList(id);
        Map<Long, AdminUserDO> userMap = adminUserService.getUserMap(detailList.stream()
                .map(NoticeConfirmDO::getUserId)
                .collect(Collectors.toList()));
        return success(detailList.stream()
                .map(item -> convertConfirmDetail(item, userMap.get(item.getUserId())))
                .collect(Collectors.toList()));
    }


    private void fillTenantNames(List<NoticeRespVO> noticeList) {
        if (noticeList == null || noticeList.isEmpty()) {
            return;
        }
        Map<Long, String> tenantMap = noticeList.stream()
                .map(NoticeRespVO::getTenantId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> {
                            TenantDO tenant = tenantService.getTenant(id);
                            return tenant != null ? tenant.getName() : "";
                        }
                ));
        noticeList.forEach(notice -> {
            if (notice.getTenantId() != null) {
                notice.setTenantName(tenantMap.get(notice.getTenantId()));
            }
        });
    }

    private void fillConfirmStats(List<NoticeRespVO> noticeList) {
        if (noticeList == null || noticeList.isEmpty()) {
            return;
        }
        Map<Long, NoticeConfirmStatsDTO> statsMap = noticeConfirmService.getStatsMap(noticeList.stream()
                .map(NoticeRespVO::getId)
                .collect(Collectors.toList()));
        noticeList.forEach(notice -> fillConfirmStats(notice, statsMap.get(notice.getId())));
    }

    private void fillConfirmStats(NoticeRespVO notice, NoticeConfirmStatsDTO stats) {
        if (stats == null) {
            notice.setConfirmTotalCount(0);
            notice.setConfirmedCount(0);
            notice.setUnconfirmedCount(0);
            notice.setOverdueCount(0);
            return;
        }
        notice.setConfirmTotalCount(stats.getConfirmTotalCount());
        notice.setConfirmedCount(stats.getConfirmedCount());
        notice.setUnconfirmedCount(stats.getUnconfirmedCount());
        notice.setOverdueCount(stats.getOverdueCount());
    }

    private NoticeConfirmDetailRespVO convertConfirmDetail(NoticeConfirmDO item, AdminUserDO user) {
        NoticeConfirmDetailRespVO respVO = BeanUtils.toBean(item, NoticeConfirmDetailRespVO.class);
        if (respVO == null) {
            return null;
        }
        respVO.setConfirmed(item.getConfirmTime() != null);
        if (user != null) {
            respVO.setUserName(user.getNickname());
            respVO.setUserAccount(user.getUsername());
        }
        return respVO;
    }

    private NoticeRespVO convertNotice(NoticeDO notice) {
        if (notice == null) {
            return null;
        }
        NoticeRespVO respVO = new NoticeRespVO();
        respVO.setId(notice.getId());
        respVO.setTitle(notice.getTitle());
        respVO.setType(notice.getType());
        respVO.setContent(notice.getContent());
        respVO.setStatus(notice.getStatus());
        respVO.setReceiverType(NoticeReceiverSupport.normalizeReceiverType(notice.getReceiverType()));
        respVO.setReceiverUserIds(NoticeReceiverSupport.parseUserIds(notice.getReceiverUserIds()));
        respVO.setNeedConfirm(notice.getNeedConfirm());
        respVO.setConfirmDeadline(notice.getConfirmDeadline());
        respVO.setCreateTime(DateUtils.of(notice.getCreateTime()));
        respVO.setTenantId(notice.getTenantId());
        return respVO;
    }

    @PostMapping("/push")
    @Operation(summary = "推送通知公告", description = "只发送给 websocket 连接在线的用户")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:notice:update')")
    public CommonResult<Boolean> push(@RequestParam("id") Long id) {
        NoticeDO notice = noticeService.getNotice(id);
        Assert.notNull(notice, "公告不能为空");
        // 通过 websocket 推送给在线的用户
        if (NoticeReceiverSupport.isUserReceiver(notice.getReceiverType())) {
            NoticeReceiverSupport.parseUserIds(notice.getReceiverUserIds())
                    .forEach(userId -> webSocketSenderApi.sendObject(UserTypeEnum.ADMIN.getValue(), userId,
                            "notice-push", notice));
        } else {
            webSocketSenderApi.sendObject(UserTypeEnum.ADMIN.getValue(), "notice-push", notice);
        }
        return success(true);
    }

    @PostMapping("/remind-unconfirmed")
    @Operation(summary = "催办未确认通知")
    @Parameter(name = "id", description = "公告编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:notice:update')")
    public CommonResult<Integer> remindUnconfirmed(@RequestParam("id") Long id) {
        NoticeDO notice = noticeService.getNotice(id);
        Assert.notNull(notice, "公告不能为空");
        List<NoticeConfirmDO> remindedList = noticeConfirmService.remindUnconfirmed(id);
        Map<String, Object> payload = buildReminderPayload(notice);
        remindedList.forEach(item -> webSocketSenderApi.sendObject(UserTypeEnum.ADMIN.getValue(), item.getUserId(),
                "notice-push", payload));
        return success(remindedList.size());
    }

    private Map<String, Object> buildReminderPayload(NoticeDO notice) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", -notice.getId());
        payload.put("title", "通知确认催办：" + notice.getTitle());
        payload.put("summary", "请尽快进入 OA 协同办公通知中心确认该通知");
        payload.put("type", "announce");
        payload.put("sender", notice.getCreator());
        payload.put("redirectUrl", "/work/notice");
        payload.put("createTime", java.time.LocalDateTime.now());
        return payload;
    }

}
