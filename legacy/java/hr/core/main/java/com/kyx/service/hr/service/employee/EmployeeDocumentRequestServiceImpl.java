package com.kyx.service.hr.service.employee;

import com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.foundation.security.core.service.SecurityFrameworkService;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.service.business.api.dept.DeptApi;
import com.kyx.service.business.api.dept.dto.DeptRespDTO;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeDocumentRequestApplyReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeDocumentRequestHandleReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeDocumentRequestPageReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeDocumentRequestRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeDocumentTemplatePreviewRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeDocumentTemplateRespVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeDocumentRequestDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;
import com.kyx.service.hr.dal.mysql.employee.EmployeeDocumentRequestMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import com.kyx.service.hr.service.todo.HrTodoTaskService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Employee document request Service implementation.
 */
@Service
@Validated
@Slf4j
public class EmployeeDocumentRequestServiceImpl implements EmployeeDocumentRequestService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_CANCELED = "CANCELED";

    private static final String ACTION_PROCESS = "PROCESS";
    private static final String ACTION_COMPLETE = "COMPLETE";
    private static final String ACTION_REJECT = "REJECT";

    private static final String PERMISSION_MANAGE = "hr:document-request:handle";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();
    private static final Map<String, DocumentTemplate> TEMPLATES = buildTemplates();

    @Resource
    private EmployeeDocumentRequestMapper documentRequestMapper;
    @Resource
    private EmployeeProfileMapper employeeProfileMapper;
    @Resource
    private EmployeeMapper employeeMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private SecurityFrameworkService securityFrameworkService;
    @Resource
    private HrTodoTaskService hrTodoTaskService;
    @Resource
    private EmployeeMaterialService employeeMaterialService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long apply(EmployeeDocumentRequestApplyReqVO reqVO) {
        EmployeeProfileDO profile = resolveApplyProfile(reqVO.getProfileId(), reqVO.getUserId());
        EmployeeDocumentRequestDO request = new EmployeeDocumentRequestDO();
        request.setProfileId(profile.getId());
        request.setUserId(profile.getUserId());
        request.setRequestType(normalizeType(reqVO.getRequestType()));
        request.setTitle(resolveTitle(reqVO.getTitle(), reqVO.getRequestType()));
        request.setPurpose(reqVO.getPurpose());
        request.setExpectedDate(reqVO.getExpectedDate());
        request.setDeliveryMode(StringUtils.hasText(reqVO.getDeliveryMode()) ? reqVO.getDeliveryMode().trim().toUpperCase() : "ONLINE");
        request.setContactInfo(reqVO.getContactInfo());
        request.setAttachmentJson(reqVO.getAttachmentJson());
        request.setStatus(STATUS_PENDING);
        documentRequestMapper.insert(request);
        refreshTodoTasksQuietly();
        return request.getId();
    }

    @Override
    public PageResult<EmployeeDocumentRequestRespVO> getPage(EmployeeDocumentRequestPageReqVO pageReqVO) {
        if (!canManage()) {
            Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
            if (loginUserId == null) {
                return new PageResult<>(new ArrayList<>(), 0L);
            }
            pageReqVO.setUserId(loginUserId);
            pageReqVO.setProfileId(null);
        }
        normalizePageReq(pageReqVO);
        PageResult<EmployeeDocumentRequestDO> pageResult = documentRequestMapper.selectPage(pageReqVO);
        List<EmployeeDocumentRequestDO> rows = pageResult.getList();
        if (rows == null || rows.isEmpty()) {
            return new PageResult<>(new ArrayList<>(), pageResult.getTotal());
        }
        List<EmployeeDocumentRequestRespVO> respList = BeanUtils.toBean(rows, EmployeeDocumentRequestRespVO.class);
        fillPeopleInfo(rows, respList);
        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean handle(EmployeeDocumentRequestHandleReqVO reqVO) {
        EmployeeDocumentRequestDO request = documentRequestMapper.selectById(reqVO.getId());
        if (request == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "证明申请不存在");
        }
        if (STATUS_COMPLETED.equals(request.getStatus())
                || STATUS_REJECTED.equals(request.getStatus())
                || STATUS_CANCELED.equals(request.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "该证明申请已结束");
        }
        String action = reqVO.getAction().trim().toUpperCase();
        validateHandleAction(action, reqVO);
        EmployeeDocumentRequestDO updateDO = new EmployeeDocumentRequestDO();
        updateDO.setId(request.getId());
        updateDO.setHandlerId(SecurityFrameworkUtils.getLoginUserId());
        updateDO.setHandledTime(LocalDateTime.now());
        updateDO.setHandleRemark(defaultText(reqVO.getHandleRemark(), null));
        if (ACTION_PROCESS.equals(action)) {
            updateDO.setStatus(STATUS_PROCESSING);
        } else if (ACTION_COMPLETE.equals(action)) {
            updateDO.setStatus(STATUS_COMPLETED);
            updateDO.setResultFileUrl(reqVO.getResultFileUrl().trim());
            updateDO.setResultFileName(defaultText(reqVO.getResultFileName(), request.getTitle()));
        } else if (ACTION_REJECT.equals(action)) {
            updateDO.setStatus(STATUS_REJECTED);
        } else {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "处理动作不合法");
        }
        documentRequestMapper.updateById(updateDO);
        if (ACTION_COMPLETE.equals(action)) {
            request.setResultFileUrl(updateDO.getResultFileUrl());
            request.setResultFileName(updateDO.getResultFileName());
            request.setHandleRemark(updateDO.getHandleRemark());
            employeeMaterialService.archiveDocumentRequestResult(request);
        }
        refreshTodoTasksQuietly();
        return true;
    }

    private void validateHandleAction(String action, EmployeeDocumentRequestHandleReqVO reqVO) {
        if (ACTION_COMPLETE.equals(action)) {
            if (!StringUtils.hasText(reqVO.getResultFileUrl())) {
                throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "办结证明申请前请先上传交付文件");
            }
            return;
        }
        if (ACTION_REJECT.equals(action) && !StringUtils.hasText(reqVO.getHandleRemark())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "驳回证明申请请填写原因");
        }
    }

    @Override
    public List<EmployeeDocumentTemplateRespVO> listTemplates() {
        List<EmployeeDocumentTemplateRespVO> result = new ArrayList<>();
        for (DocumentTemplate template : TEMPLATES.values()) {
            EmployeeDocumentTemplateRespVO item = new EmployeeDocumentTemplateRespVO();
            item.setCode(template.code);
            item.setRequestType(template.requestType);
            item.setName(template.name);
            item.setDescription(template.description);
            item.setPlaceholders(template.placeholders);
            result.add(item);
        }
        return result;
    }

    @Override
    public EmployeeDocumentTemplatePreviewRespVO previewTemplate(Long id, String templateCode) {
        EmployeeDocumentRequestDO request = getAccessibleRequest(id);
        DocumentTemplate template = resolveTemplate(templateCode, request.getRequestType());
        Map<String, String> variables = buildTemplateVariables(request);
        EmployeeDocumentTemplatePreviewRespVO respVO = new EmployeeDocumentTemplatePreviewRespVO();
        respVO.setTemplateCode(template.code);
        respVO.setRequestType(template.requestType);
        respVO.setTitle(render(template.name, variables));
        respVO.setFileName(buildFileName(request, template, variables));
        respVO.setContent(renderContent(template, variables));
        respVO.setVariables(variables);
        return respVO;
    }

    @Override
    public byte[] exportTemplate(Long id, String templateCode) throws IOException {
        EmployeeDocumentTemplatePreviewRespVO preview = previewTemplate(id, templateCode);
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XWPFParagraph titleParagraph = document.createParagraph();
            titleParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setBold(true);
            titleRun.setFontFamily("宋体");
            titleRun.setFontSize(18);
            titleRun.setText(preview.getTitle());

            String[] paragraphs = preview.getContent().split("\\n\\n");
            for (String paragraphText : paragraphs) {
                XWPFParagraph paragraph = document.createParagraph();
                paragraph.setSpacingBetween(1.3);
                paragraph.setIndentationFirstLine(420);
                XWPFRun run = paragraph.createRun();
                run.setFontFamily("宋体");
                run.setFontSize(12);
                run.setText(paragraphText);
            }
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancel(Long id) {
        EmployeeDocumentRequestDO request = documentRequestMapper.selectById(id);
        if (request == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "证明申请不存在");
        }
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (!canManage() && (loginUserId == null || !loginUserId.equals(request.getUserId()))) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.FORBIDDEN, "无权撤销该证明申请");
        }
        if (!STATUS_PENDING.equals(request.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "只有待处理申请可以撤销");
        }
        EmployeeDocumentRequestDO updateDO = new EmployeeDocumentRequestDO();
        updateDO.setId(request.getId());
        updateDO.setStatus(STATUS_CANCELED);
        documentRequestMapper.updateById(updateDO);
        refreshTodoTasksQuietly();
        return true;
    }

    private EmployeeProfileDO resolveApplyProfile(Long profileId, Long userId) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.UNAUTHORIZED);
        }
        boolean manage = canManage();
        if ((profileId != null || userId != null) && !manage) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.FORBIDDEN, "无权替他人提交证明申请");
        }
        EmployeeProfileDO profile = profileId == null ? null : employeeProfileMapper.selectById(profileId);
        if (profile == null) {
            profile = employeeProfileMapper.selectByUserId(userId == null ? loginUserId : userId);
        }
        if (profile == null || profile.getUserId() == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "员工档案不存在或未绑定登录用户");
        }
        if (userId != null && !Objects.equals(userId, profile.getUserId())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "员工档案和用户不匹配");
        }
        return profile;
    }

    private void normalizePageReq(EmployeeDocumentRequestPageReqVO pageReqVO) {
        if (StringUtils.hasText(pageReqVO.getRequestType())) {
            pageReqVO.setRequestType(normalizeType(pageReqVO.getRequestType()));
        }
        if (StringUtils.hasText(pageReqVO.getStatus())) {
            pageReqVO.setStatus(pageReqVO.getStatus().trim().toUpperCase());
        }
    }

    private String normalizeType(String requestType) {
        String value = requestType == null ? "" : requestType.trim().toUpperCase();
        if (!StringUtils.hasText(value)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "证明类型不能为空");
        }
        return value;
    }

    private String resolveTitle(String title, String requestType) {
        if (StringUtils.hasText(title)) {
            return title.trim();
        }
        String type = normalizeType(requestType);
        if ("EMPLOYMENT".equals(type)) {
            return "在职证明";
        }
        if ("INCOME".equals(type)) {
            return "收入证明";
        }
        if ("INTERNSHIP".equals(type)) {
            return "实习证明";
        }
        if ("RESIGNATION".equals(type)) {
            return "离职证明";
        }
        return "其他证明";
    }

    private boolean canManage() {
        try {
            return securityFrameworkService.hasPermission(PERMISSION_MANAGE);
        } catch (Exception ex) {
            log.warn("check document request manage permission failed: {}", ex.getMessage());
            return false;
        }
    }

    private void refreshTodoTasksQuietly() {
        try {
            hrTodoTaskService.refreshGeneratedTasks();
        } catch (Exception ex) {
            log.warn("Refresh HR todo tasks after document request change failed: {}", ex.getMessage());
        }
    }

    private void fillPeopleInfo(List<EmployeeDocumentRequestDO> rows, List<EmployeeDocumentRequestRespVO> respList) {
        Set<Long> userIds = new HashSet<>();
        Set<Long> profileIds = new HashSet<>();
        for (EmployeeDocumentRequestDO row : rows) {
            if (row.getUserId() != null) {
                userIds.add(row.getUserId());
            }
            if (row.getHandlerId() != null) {
                userIds.add(row.getHandlerId());
            }
            if (row.getProfileId() != null) {
                profileIds.add(row.getProfileId());
            }
        }
        Map<Long, AdminUserRespDTO> userMap = loadUserMapSafe(userIds);
        Map<Long, EmployeeProfileDO> profileMap = loadProfileMapSafe(profileIds);
        for (EmployeeDocumentRequestRespVO item : respList) {
            AdminUserRespDTO user = userMap.get(item.getUserId());
            if (user != null) {
                item.setUserNickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
            }
            AdminUserRespDTO handler = userMap.get(item.getHandlerId());
            if (handler != null) {
                item.setHandlerName(StringUtils.hasText(handler.getNickname()) ? handler.getNickname() : handler.getUsername());
            }
            EmployeeProfileDO profile = profileMap.get(item.getProfileId());
            if (profile != null) {
                item.setProfileName(profile.getName());
            }
        }
    }

    private Map<Long, AdminUserRespDTO> loadUserMapSafe(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return adminUserApi.getUserMap(userIds);
        } catch (Exception ex) {
            log.warn("Failed to load admin users for document request page: {}", ex.getMessage());
            return new HashMap<>();
        }
    }

    private Map<Long, EmployeeProfileDO> loadProfileMapSafe(Set<Long> profileIds) {
        Map<Long, EmployeeProfileDO> profileMap = new HashMap<>();
        if (profileIds == null || profileIds.isEmpty()) {
            return profileMap;
        }
        List<EmployeeProfileDO> profiles = employeeProfileMapper.selectList(
                new LambdaQueryWrapperX<EmployeeProfileDO>().in(EmployeeProfileDO::getId, profileIds));
        if (profiles == null) {
            return profileMap;
        }
        for (EmployeeProfileDO profile : profiles) {
            if (profile.getId() != null) {
                profileMap.put(profile.getId(), profile);
            }
        }
        return profileMap;
    }

    private EmployeeDocumentRequestDO getAccessibleRequest(Long id) {
        EmployeeDocumentRequestDO request = documentRequestMapper.selectById(id);
        if (request == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "证明申请不存在");
        }
        if (!canManage()) {
            Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
            if (loginUserId == null || !Objects.equals(loginUserId, request.getUserId())) {
                throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.FORBIDDEN, "无权查看该证明申请");
            }
        }
        return request;
    }

    private DocumentTemplate resolveTemplate(String templateCode, String requestType) {
        String key = StringUtils.hasText(templateCode) ? templateCode.trim().toUpperCase() : normalizeType(requestType);
        DocumentTemplate template = TEMPLATES.get(key);
        if (template != null) {
            return template;
        }
        template = TEMPLATES.get(normalizeType(requestType));
        return template == null ? TEMPLATES.get("OTHER") : template;
    }

    private Map<String, String> buildTemplateVariables(EmployeeDocumentRequestDO request) {
        EmployeeProfileDO profile = employeeProfileMapper.selectById(request.getProfileId());
        if (profile == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "员工档案不存在");
        }
        EmployeeDO employee = loadEmployeeByProfileId(profile.getId());
        AdminUserRespDTO handler = request.getHandlerId() == null ? null : loadUserSafe(request.getHandlerId());

        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("employeeName", defaultText(profile.getName(), "员工"));
        variables.put("profileNo", defaultText(profile.getProfileNo(), "-"));
        variables.put("idNumber", maskIdNumber(profile.getIdNumber()));
        variables.put("mobile", defaultText(profile.getMobile(), "-"));
        variables.put("deptName", resolveDeptName(employee));
        variables.put("jobTitle", defaultText(employee == null ? null : employee.getJobTitle(), defaultText(profile.getJobLevel(), "岗位")));
        variables.put("onboardDate", formatDate(firstDate(profile.getOnboardDate(), employee == null ? null : employee.getEntryDate())));
        variables.put("expectedDate", formatDate(request.getExpectedDate()));
        variables.put("purpose", defaultText(request.getPurpose(), "相关事项"));
        variables.put("requestTitle", defaultText(request.getTitle(), resolveTitle(null, request.getRequestType())));
        variables.put("requestType", resolveTitle(null, request.getRequestType()));
        variables.put("requestDate", formatDate(request.getCreateTime()));
        variables.put("issueDate", formatDate(LocalDate.now()));
        variables.put("deliveryMode", deliveryModeText(request.getDeliveryMode()));
        variables.put("handlerName", handler == null ? "人力资源部" : defaultText(handler.getNickname(), handler.getUsername()));
        variables.put("companyName", "本公司");
        return variables;
    }

    private EmployeeDO loadEmployeeByProfileId(Long profileId) {
        if (profileId == null) {
            return null;
        }
        List<EmployeeDO> employees = employeeMapper.selectList(new LambdaQueryWrapperX<EmployeeDO>()
                .eq(EmployeeDO::getProfileId, profileId)
                .orderByDesc(EmployeeDO::getId)
                .last("LIMIT 1"));
        return employees == null || employees.isEmpty() ? null : employees.get(0);
    }

    private AdminUserRespDTO loadUserSafe(Long userId) {
        try {
            return userId == null ? null : adminUserApi.getUser(userId).getCheckedData();
        } catch (Exception ex) {
            log.warn("Failed to load user for document template, userId={}: {}", userId, ex.getMessage());
            return null;
        }
    }

    private String resolveDeptName(EmployeeDO employee) {
        if (employee == null || employee.getDeptId() == null) {
            return "所在部门";
        }
        try {
            DeptRespDTO dept = deptApi.getDept(employee.getDeptId()).getCheckedData();
            return dept == null ? "所在部门" : defaultText(dept.getName(), "所在部门");
        } catch (Exception ex) {
            log.warn("Failed to load dept for document template, deptId={}: {}", employee.getDeptId(), ex.getMessage());
            return "所在部门";
        }
    }

    private String renderContent(DocumentTemplate template, Map<String, String> variables) {
        List<String> rendered = new ArrayList<>();
        for (String paragraph : template.paragraphs) {
            rendered.add(render(paragraph, variables));
        }
        rendered.add(render("经办人：${handlerName}", variables));
        rendered.add(render("开具日期：${issueDate}", variables));
        return String.join("\n\n", rendered);
    }

    private String render(String text, Map<String, String> variables) {
        String rendered = text;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            rendered = rendered.replace("${" + entry.getKey() + "}", defaultText(entry.getValue(), "-"));
        }
        return rendered;
    }

    private String buildFileName(EmployeeDocumentRequestDO request, DocumentTemplate template, Map<String, String> variables) {
        String title = defaultText(request.getTitle(), template.name);
        String employeeName = defaultText(variables.get("employeeName"), "员工");
        return sanitizeFileName(title + "_" + employeeName + "_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".docx");
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private LocalDate firstDate(LocalDate first, LocalDate fallback) {
        return first != null ? first : fallback;
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : DATE_FORMATTER.format(date);
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "-";
        }
        return DATE_FORMATTER.format(date.toInstant().atZone(DEFAULT_ZONE).toLocalDate());
    }

    private String maskIdNumber(String idNumber) {
        if (!StringUtils.hasText(idNumber)) {
            return "-";
        }
        String value = idNumber.trim();
        if (value.length() <= 8) {
            return value;
        }
        return value.substring(0, 6) + "********" + value.substring(value.length() - 4);
    }

    private String deliveryModeText(String deliveryMode) {
        String value = StringUtils.hasText(deliveryMode) ? deliveryMode.trim().toUpperCase() : "ONLINE";
        if ("PICKUP".equals(value)) {
            return "纸质自取";
        }
        if ("MAIL".equals(value)) {
            return "快递邮寄";
        }
        return "线上交付";
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private static Map<String, DocumentTemplate> buildTemplates() {
        Map<String, DocumentTemplate> templates = new LinkedHashMap<>();
        putTemplate(templates, new DocumentTemplate(
                "EMPLOYMENT",
                "EMPLOYMENT",
                "在职证明",
                "用于银行、签证、落户、资质等需要确认劳动关系的场景",
                Arrays.asList("employeeName", "idNumber", "profileNo", "deptName", "jobTitle", "onboardDate", "purpose", "issueDate"),
                Arrays.asList(
                        "兹证明 ${employeeName}（身份证号：${idNumber}，员工编号：${profileNo}）为${companyName}员工。",
                        "该员工于 ${onboardDate} 入职，目前在 ${deptName} 担任 ${jobTitle}，劳动关系正常存续。",
                        "本证明仅用于 ${purpose}，不作其他用途。特此证明。"
                )));
        putTemplate(templates, new DocumentTemplate(
                "INCOME",
                "INCOME",
                "收入证明",
                "用于贷款、租房、签证等需要 HR 出具收入说明的场景",
                Arrays.asList("employeeName", "idNumber", "deptName", "jobTitle", "onboardDate", "purpose", "issueDate"),
                Arrays.asList(
                        "兹证明 ${employeeName}（身份证号：${idNumber}）为${companyName}员工，于 ${onboardDate} 入职。",
                        "该员工目前在 ${deptName} 担任 ${jobTitle}，薪酬收入以公司薪酬台账及财务发放记录为准。",
                        "本证明用于 ${purpose}，仅作为员工收入情况说明。特此证明。"
                )));
        putTemplate(templates, new DocumentTemplate(
                "INTERNSHIP",
                "INTERNSHIP",
                "实习证明",
                "用于学生实习、院校材料、实习经历确认等场景",
                Arrays.asList("employeeName", "idNumber", "deptName", "jobTitle", "onboardDate", "purpose", "issueDate"),
                Arrays.asList(
                        "兹证明 ${employeeName}（身份证号：${idNumber}）曾在${companyName}参加实习或实践工作。",
                        "实习岗位为 ${deptName} ${jobTitle}，起始日期为 ${onboardDate}，期间表现及具体事项以公司记录为准。",
                        "本证明用于 ${purpose}。特此证明。"
                )));
        putTemplate(templates, new DocumentTemplate(
                "RESIGNATION",
                "RESIGNATION",
                "离职证明",
                "用于员工离职后开具劳动关系解除说明",
                Arrays.asList("employeeName", "idNumber", "profileNo", "onboardDate", "purpose", "issueDate"),
                Arrays.asList(
                        "兹证明 ${employeeName}（身份证号：${idNumber}，员工编号：${profileNo}）曾为${companyName}员工。",
                        "该员工入职日期为 ${onboardDate}，离职证明办理事项已按公司流程提交。",
                        "本证明用于 ${purpose}。特此证明。"
                )));
        putTemplate(templates, new DocumentTemplate(
                "OTHER",
                "OTHER",
                "其他证明",
                "用于自定义用途的证明开具，可结合申请标题和用途说明使用",
                Arrays.asList("employeeName", "idNumber", "requestTitle", "purpose", "issueDate"),
                Arrays.asList(
                        "兹证明 ${employeeName}（身份证号：${idNumber}）申请办理“${requestTitle}”。",
                        "申请用途为 ${purpose}，相关信息以公司人事档案及业务记录为准。",
                        "特此证明。"
                )));
        return Collections.unmodifiableMap(templates);
    }

    private static void putTemplate(Map<String, DocumentTemplate> templates, DocumentTemplate template) {
        templates.put(template.code, template);
    }

    private static final class DocumentTemplate {

        private final String code;
        private final String requestType;
        private final String name;
        private final String description;
        private final List<String> placeholders;
        private final List<String> paragraphs;

        private DocumentTemplate(String code, String requestType, String name, String description,
                                 List<String> placeholders, List<String> paragraphs) {
            this.code = code;
            this.requestType = requestType;
            this.name = name;
            this.description = description;
            this.placeholders = placeholders;
            this.paragraphs = paragraphs;
        }
    }

}
