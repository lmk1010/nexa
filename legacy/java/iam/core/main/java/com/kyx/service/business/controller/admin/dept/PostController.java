package com.kyx.service.business.controller.admin.dept;

import com.kyx.foundation.apilog.core.annotation.ApiAccessLog;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageParam;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.excel.core.util.ExcelUtils;
import com.kyx.service.business.controller.admin.dept.vo.post.PostPageReqVO;
import com.kyx.service.business.controller.admin.dept.vo.post.PostRespVO;
import com.kyx.service.business.controller.admin.dept.vo.post.PostSaveReqVO;
import com.kyx.service.business.controller.admin.dept.vo.post.PostSimpleRespVO;
import com.kyx.service.business.dal.dataobject.dept.PostDO;
import com.kyx.service.business.dal.dataobject.tenant.TenantDO;
import com.kyx.service.business.service.dept.PostService;
import com.kyx.service.business.service.tenant.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.kyx.foundation.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 岗位")
@RestController
@RequestMapping("/system/post")
@Validated
public class PostController {

    @Resource
    private PostService postService;

    @Resource
    private TenantService tenantService;

    @PostMapping("/create")
    @Operation(summary = "创建岗位")
    @PreAuthorize("@ss.hasPermission('system:post:create')")
    public CommonResult<Long> createPost(@Valid @RequestBody PostSaveReqVO createReqVO) {
        Long postId = postService.createPost(createReqVO);
        return success(postId);
    }

    @PutMapping("/update")
    @Operation(summary = "修改岗位")
    @PreAuthorize("@ss.hasPermission('system:post:update')")
    public CommonResult<Boolean> updatePost(@Valid @RequestBody PostSaveReqVO updateReqVO) {
        postService.updatePost(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除岗位")
    @PreAuthorize("@ss.hasPermission('system:post:delete')")
    public CommonResult<Boolean> deletePost(@RequestParam("id") Long id) {
        postService.deletePost(id);
        return success(true);
    }

    @GetMapping(value = "/get")
    @Operation(summary = "获得岗位信息")
    @Parameter(name = "id", description = "岗位编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:post:query')")
    public CommonResult<PostRespVO> getPost(@RequestParam("id") Long id) {
        PostDO post = postService.getPost(id);
        return success(BeanUtils.toBean(post, PostRespVO.class));
    }

    @GetMapping(value = {"/list-all-simple", "simple-list"})
    @Operation(summary = "获取岗位全列表", description = "只包含被开启的岗位，主要用于前端的下拉选项")
    public CommonResult<List<PostSimpleRespVO>> getSimplePostList() {
        // 获得岗位列表，只要开启状态的
        List<PostDO> list = postService.getPostList(null, Collections.singleton(CommonStatusEnum.ENABLE.getStatus()));
        // 排序后，返回给前端
        list.sort(Comparator.comparing(PostDO::getSort));
        return success(BeanUtils.toBean(list, PostSimpleRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得岗位分页列表")
    @PreAuthorize("@ss.hasPermission('system:post:query')")
    public CommonResult<PageResult<PostRespVO>> getPostPage(@Validated PostPageReqVO pageReqVO) {
        PageResult<PostDO> pageResult = postService.getPostPage(pageReqVO);
        PageResult<PostRespVO> voResult = BeanUtils.toBean(pageResult, PostRespVO.class);
        // 填充租户名称
        if (voResult.getList() != null && !voResult.getList().isEmpty()) {
            Map<Long, String> tenantMap = voResult.getList().stream()
                    .map(PostRespVO::getTenantId)
                    .filter(id -> id != null)
                    .distinct()
                    .collect(Collectors.toMap(
                            id -> id,
                            id -> {
                                TenantDO tenant = tenantService.getTenant(id);
                                return tenant != null ? tenant.getName() : "";
                            }
                    ));
            voResult.getList().forEach(vo -> {
                if (vo.getTenantId() != null) {
                    vo.setTenantName(tenantMap.get(vo.getTenantId()));
                }
            });
        }
        return success(voResult);
    }

    @GetMapping("/export")
    @Operation(summary = "岗位管理")
    @PreAuthorize("@ss.hasPermission('system:post:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void export(HttpServletResponse response, @Validated PostPageReqVO reqVO) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<PostDO> list = postService.getPostPage(reqVO).getList();
        // 输出
        ExcelUtils.write(response, "岗位数据.xls", "岗位列表", PostRespVO.class,
                BeanUtils.toBean(list, PostRespVO.class));
    }

}
