package com.kyx.service.business.controller.admin.socail;

import com.kyx.foundation.common.enums.UserTypeEnum;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.business.api.social.dto.SocialUserBindReqDTO;
import com.kyx.service.business.controller.admin.socail.vo.user.SocialUserBindReqVO;
import com.kyx.service.business.controller.admin.socail.vo.user.SocialUserPageReqVO;
import com.kyx.service.business.controller.admin.socail.vo.user.SocialUserRespVO;
import com.kyx.service.business.controller.admin.socail.vo.user.SocialUserUnbindReqVO;
import com.kyx.service.business.dal.dataobject.social.SocialUserBindDO;
import com.kyx.service.business.dal.dataobject.social.SocialUserDO;
import com.kyx.service.business.dal.dataobject.user.AdminUserDO;
import com.kyx.service.business.dal.mysql.social.SocialUserBindMapper;
import com.kyx.service.business.service.user.AdminUserService;
import com.kyx.service.business.service.social.SocialUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.kyx.foundation.common.pojo.CommonResult.success;
import static com.kyx.foundation.common.util.collection.CollectionUtils.convertList;
import static com.kyx.foundation.common.util.collection.CollectionUtils.convertSet;
import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 社交用户")
@RestController
@RequestMapping("/system/social-user")
@Validated
public class SocialUserController {

    @Resource
    private SocialUserService socialUserService;
    @Resource
    private SocialUserBindMapper socialUserBindMapper;
    @Resource
    private AdminUserService adminUserService;

    @PostMapping("/bind")
    @Operation(summary = "社交绑定，使用 code 授权码")
    public CommonResult<Boolean> socialBind(@RequestBody @Valid SocialUserBindReqVO reqVO) {
        socialUserService.bindSocialUser(BeanUtils.toBean(reqVO, SocialUserBindReqDTO.class)
                .setUserId(getLoginUserId()).setUserType(UserTypeEnum.ADMIN.getValue()));
        return CommonResult.success(true);
    }

    @DeleteMapping("/unbind")
    @Operation(summary = "取消社交绑定")
    public CommonResult<Boolean> socialUnbind(@RequestBody SocialUserUnbindReqVO reqVO) {
        socialUserService.unbindSocialUser(getLoginUserId(), UserTypeEnum.ADMIN.getValue(), reqVO.getType(), reqVO.getOpenid());
        return CommonResult.success(true);
    }

    @GetMapping("/get-bind-list")
    @Operation(summary = "获得绑定社交用户列表")
    public CommonResult<List<SocialUserRespVO>> getBindSocialUserList() {
        List<SocialUserDO> list = socialUserService.getSocialUserList(getLoginUserId(), UserTypeEnum.ADMIN.getValue());
        return success(convertList(list, socialUser -> new SocialUserRespVO() // 返回精简信息
                .setId(socialUser.getId()).setType(socialUser.getType()).setOpenid(socialUser.getOpenid())
                .setNickname(socialUser.getNickname()).setAvatar(socialUser.getNickname())));
    }

    // ==================== 社交用户 CRUD ====================

    @GetMapping("/get")
    @Operation(summary = "获得社交用户")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:social-user:query')")
    public CommonResult<SocialUserRespVO> getSocialUser(@RequestParam("id") Long id) {
        SocialUserDO socialUser = socialUserService.getSocialUser(id);
        return success(BeanUtils.toBean(socialUser, SocialUserRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得社交用户分页")
    @PreAuthorize("@ss.hasPermission('system:social-user:query')")
    public CommonResult<PageResult<SocialUserRespVO>> getSocialUserPage(@Valid SocialUserPageReqVO pageVO) {
        PageResult<SocialUserDO> pageResult = socialUserService.getSocialUserPage(pageVO);
        if (pageResult == null || pageResult.getTotal() == 0 || pageResult.getList().isEmpty()) {
            return success(new PageResult<>(Collections.emptyList(), pageResult == null ? 0L : pageResult.getTotal()));
        }
        List<SocialUserRespVO> rows = BeanUtils.toBean(pageResult.getList(), SocialUserRespVO.class);
        List<SocialUserBindDO> bindList = socialUserBindMapper.selectListBySocialUserIdsAndUserType(
                convertSet(pageResult.getList(), SocialUserDO::getId),
                UserTypeEnum.ADMIN.getValue()
        );
        Map<Long, SocialUserBindDO> bindMap = bindList.stream()
                .filter(bind -> bind.getSocialUserId() != null)
                .collect(java.util.stream.Collectors.toMap(
                        SocialUserBindDO::getSocialUserId,
                        bind -> bind,
                        (a, b) -> a
                ));
        Map<Long, AdminUserDO> userMap = adminUserService.getUserMap(
                convertSet(bindList, SocialUserBindDO::getUserId)
        );
        for (SocialUserRespVO row : rows) {
            SocialUserBindDO bind = bindMap.get(row.getId());
            boolean bound = bind != null;
            row.setBound(bound);
            if (!bound) {
                continue;
            }
            row.setUserId(bind.getUserId());
            AdminUserDO user = userMap.get(bind.getUserId());
            if (user != null) {
                String displayName = user.getNickname() != null && !user.getNickname().trim().isEmpty()
                        ? user.getNickname()
                        : user.getUsername();
                row.setUserName(displayName);
            }
        }
        return success(new PageResult<>(rows, pageResult.getTotal()));
    }

}
