package com.kyx.service.hr.controller.admin.questionnaire;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageParam;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePublicLinkRespVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePublicLinkSaveReqVO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnairePublicAnswerDO;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnairePublicAnswerMapper;
import com.kyx.service.hr.service.questionnaire.QuestionnairePublicLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static com.kyx.foundation.common.pojo.CommonResult.success;

/**
 * 问卷公开链接 Controller
 */
@Tag(name = "管理后台 - 问卷公开链接")
@RestController
@RequestMapping("/hr/questionnaire-public-link")
@Validated
public class QuestionnairePublicLinkController {

    @Resource
    private QuestionnairePublicLinkService publicLinkService;
    @Resource
    private QuestionnairePublicAnswerMapper publicAnswerMapper;

    @PostMapping("/create")
    @Operation(summary = "创建公开链接")
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:publish:create')")
    public CommonResult<Long> createPublicLink(@Valid @RequestBody QuestionnairePublicLinkSaveReqVO createReqVO) {
        return success(publicLinkService.createPublicLink(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新公开链接")
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:publish:update')")
    public CommonResult<Boolean> updatePublicLink(@Valid @RequestBody QuestionnairePublicLinkSaveReqVO updateReqVO) {
        publicLinkService.updatePublicLink(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除公开链接")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:publish:delete')")
    public CommonResult<Boolean> deletePublicLink(@RequestParam("id") Long id) {
        publicLinkService.deletePublicLink(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取公开链接")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:publish:query')")
    public CommonResult<QuestionnairePublicLinkRespVO> getPublicLink(@RequestParam("id") Long id) {
        return success(publicLinkService.getPublicLink(id));
    }

    @GetMapping("/list")
    @Operation(summary = "获取问卷的公开链接列表")
    @Parameter(name = "questionnaireId", description = "问卷ID", required = true)
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:publish:query')")
    public CommonResult<List<QuestionnairePublicLinkRespVO>> getPublicLinkList(
            @RequestParam("questionnaireId") Long questionnaireId) {
        return success(publicLinkService.getPublicLinksByQuestionnaireId(questionnaireId));
    }

    @GetMapping("/list-by-publish")
    @Operation(summary = "根据发布ID获取公开链接列表")
    @Parameter(name = "publishId", description = "发布ID", required = true)
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:publish:query')")
    public CommonResult<List<QuestionnairePublicLinkRespVO>> getPublicLinkListByPublish(
            @RequestParam("publishId") Long publishId) {
        return success(publicLinkService.getPublicLinksByPublishId(publishId));
    }

    @GetMapping("/answer-page")
    @Operation(summary = "获取公开问卷提交记录分页")
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:publish:query')")
    public CommonResult<PageResult<QuestionnairePublicAnswerDO>> getPublicAnswerPage(
            @RequestParam("linkId") Long linkId,
            @RequestParam(value = "respondentName", required = false) String respondentName,
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        PageParam pageParam = new PageParam();
        pageParam.setPageNo(pageNo);
        pageParam.setPageSize(pageSize);
        return success(publicAnswerMapper.selectPage(pageParam, linkId, respondentName));
    }

    @GetMapping("/answer-get")
    @Operation(summary = "获取公开问卷提交详情")
    @Parameter(name = "id", description = "提交记录ID", required = true)
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:publish:query')")
    public CommonResult<QuestionnairePublicAnswerDO> getPublicAnswer(@RequestParam("id") Long id) {
        return success(publicAnswerMapper.selectById(id));
    }

}
