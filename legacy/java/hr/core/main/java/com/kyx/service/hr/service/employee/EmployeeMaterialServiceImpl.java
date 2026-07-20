package com.kyx.service.hr.service.employee;

import com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.foundation.security.core.service.SecurityFrameworkService;
import com.kyx.foundation.security.core.util.SecurityFrameworkUtils;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeMaterialPageReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeMaterialRenewReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeMaterialReviewReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeMaterialRespVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeMaterialSaveReqVO;
import com.kyx.service.hr.controller.admin.employee.vo.EmployeeMaterialSubmitReqVO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeDocumentRequestDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeMaterialDO;
import com.kyx.service.hr.dal.dataobject.employee.EmployeeProfileDO;
import com.kyx.service.hr.dal.mysql.employee.EmployeeMaterialMapper;
import com.kyx.service.hr.dal.mysql.employee.EmployeeProfileMapper;
import com.kyx.service.hr.service.todo.HrTodoTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Validated
@Slf4j
public class EmployeeMaterialServiceImpl implements EmployeeMaterialService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_EXPIRED = "EXPIRED";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final String STATUS_MISSING = "MISSING";
    private static final String STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String ACTION_APPROVE = "APPROVE";
    private static final String ACTION_REJECT = "REJECT";
    private static final String ACTION_ARCHIVE = "ARCHIVE";
    private static final String SOURCE_DOCUMENT_REQUEST = "DOCUMENT_REQUEST";
    private static final String SOURCE_SELF_UPLOAD = "SELF_UPLOAD";
    private static final String CATEGORY_PROOF = "PROOF";
    private static final String PERMISSION_QUERY = "hr:employee-material:query";
    private static final String PERMISSION_MANAGE = "hr:employee-material:manage";

    @Resource
    private EmployeeMaterialMapper employeeMaterialMapper;
    @Resource
    private EmployeeProfileMapper employeeProfileMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private SecurityFrameworkService securityFrameworkService;
    @Resource
    private HrTodoTaskService hrTodoTaskService;

    @Override
    public PageResult<EmployeeMaterialRespVO> getPage(EmployeeMaterialPageReqVO pageReqVO) {
        if (!canViewAll()) {
            Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
            if (loginUserId == null) {
                return new PageResult<>(new ArrayList<>(), 0L);
            }
            pageReqVO.setUserId(loginUserId);
            pageReqVO.setProfileId(null);
            pageReqVO.setProfileIds(null);
        } else if (!prepareProfileFilter(pageReqVO)) {
            return new PageResult<>(new ArrayList<>(), 0L);
        }
        normalizePageReq(pageReqVO);
        PageResult<EmployeeMaterialDO> pageResult = employeeMaterialMapper.selectPage(pageReqVO);
        List<EmployeeMaterialDO> rows = pageResult.getList();
        if (rows == null || rows.isEmpty()) {
            return new PageResult<>(new ArrayList<>(), pageResult.getTotal());
        }
        List<EmployeeMaterialRespVO> respList = BeanUtils.toBean(rows, EmployeeMaterialRespVO.class);
        fillPeopleInfo(rows, respList);
        fillExpireDays(respList);
        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(EmployeeMaterialSaveReqVO reqVO) {
        EmployeeProfileDO profile = validateProfileExists(reqVO.getProfileId());
        EmployeeMaterialDO saveDO = BeanUtils.toBean(reqVO, EmployeeMaterialDO.class);
        saveDO.setUserId(profile.getUserId());
        normalizeMaterial(saveDO);
        if (saveDO.getId() == null) {
            employeeMaterialMapper.insert(saveDO);
        } else {
            if (employeeMaterialMapper.selectById(saveDO.getId()) == null) {
                throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "员工材料不存在");
            }
            employeeMaterialMapper.updateById(saveDO);
        }
        refreshTodoTasksQuietly();
        return saveDO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submit(EmployeeMaterialSubmitReqVO reqVO) {
        EmployeeMaterialDO material = reqVO.getId() == null ? createSelfMaterial(reqVO) : getAccessibleMaterial(reqVO.getId());
        if (STATUS_ARCHIVED.equals(material.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "已归档材料不能重新提交");
        }
        material.setFileUrl(trimRequired(reqVO.getFileUrl(), "文件链接不能为空"));
        material.setFileName(defaultText(reqVO.getFileName(), material.getMaterialName()));
        material.setFileSize(reqVO.getFileSize());
        material.setIssueDate(reqVO.getIssueDate());
        material.setExpireDate(reqVO.getExpireDate());
        material.setRemark(trimNullable(reqVO.getRemark()));
        material.setStatus(STATUS_PENDING_REVIEW);
        material.setSubmittedTime(LocalDateTime.now());
        material.setRejectReason("");
        if (!StringUtils.hasText(material.getSourceType())) {
            material.setSourceType(SOURCE_SELF_UPLOAD);
        }
        if (material.getId() == null) {
            employeeMaterialMapper.insert(material);
        } else {
            employeeMaterialMapper.updateById(material);
        }
        refreshTodoTasksQuietly();
        return material.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean review(EmployeeMaterialReviewReqVO reqVO) {
        EmployeeMaterialDO material = validateMaterialExists(reqVO.getId());
        String action = normalize(reqVO.getAction(), "");
        EmployeeMaterialDO updateDO = new EmployeeMaterialDO();
        updateDO.setId(material.getId());
        updateDO.setReviewerId(SecurityFrameworkUtils.getLoginUserId());
        updateDO.setReviewedTime(LocalDateTime.now());
        updateDO.setRemark(trimNullable(reqVO.getRemark()));
        if (ACTION_APPROVE.equals(action)) {
            if (!StringUtils.hasText(material.getFileUrl())) {
                throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "审核通过前请先上传材料文件");
            }
            updateDO.setStatus(STATUS_ACTIVE);
            updateDO.setRejectReason("");
            if (material.getIssueDate() == null) {
                updateDO.setIssueDate(LocalDate.now());
            }
        } else if (ACTION_REJECT.equals(action)) {
            updateDO.setStatus(STATUS_REJECTED);
            updateDO.setRejectReason(trimRequired(
                    defaultText(reqVO.getRejectReason(), reqVO.getRemark()), "驳回原因不能为空"));
        } else if (ACTION_ARCHIVE.equals(action)) {
            updateDO.setStatus(STATUS_ARCHIVED);
        } else {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "审核动作不合法");
        }
        employeeMaterialMapper.updateById(updateDO);
        refreshTodoTasksQuietly();
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean renew(EmployeeMaterialRenewReqVO reqVO) {
        EmployeeMaterialDO material = validateMaterialExists(reqVO.getId());
        if (STATUS_ARCHIVED.equals(material.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "已归档材料不能续期");
        }
        String fileUrl = StringUtils.hasText(reqVO.getFileUrl())
                ? reqVO.getFileUrl().trim() : material.getFileUrl();
        if (!StringUtils.hasText(fileUrl)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "续期前请先上传材料文件");
        }
        EmployeeMaterialDO updateDO = new EmployeeMaterialDO();
        updateDO.setId(material.getId());
        updateDO.setFileUrl(fileUrl);
        updateDO.setFileName(trimNullable(defaultText(reqVO.getFileName(), material.getFileName(), material.getMaterialName())));
        updateDO.setFileSize(reqVO.getFileSize() == null ? material.getFileSize() : reqVO.getFileSize());
        updateDO.setIssueDate(reqVO.getIssueDate() == null ? LocalDate.now() : reqVO.getIssueDate());
        updateDO.setExpireDate(reqVO.getExpireDate());
        updateDO.setStatus(STATUS_ACTIVE);
        updateDO.setRejectReason("");
        updateDO.setReviewerId(SecurityFrameworkUtils.getLoginUserId());
        updateDO.setReviewedTime(LocalDateTime.now());
        updateDO.setRemark(trimNullable(defaultText(reqVO.getRemark(), material.getRemark())));
        employeeMaterialMapper.updateById(updateDO);
        refreshTodoTasksQuietly();
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(Long id) {
        validateMaterialExists(id);
        employeeMaterialMapper.deleteById(id);
        refreshTodoTasksQuietly();
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archiveDocumentRequestResult(EmployeeDocumentRequestDO request) {
        if (request == null || request.getId() == null || !StringUtils.hasText(request.getResultFileUrl())) {
            return;
        }
        EmployeeMaterialDO existing = employeeMaterialMapper.selectBySource(SOURCE_DOCUMENT_REQUEST, request.getId());
        EmployeeMaterialDO material = existing == null ? new EmployeeMaterialDO() : existing;
        material.setProfileId(request.getProfileId());
        material.setUserId(request.getUserId());
        material.setCategory(CATEGORY_PROOF);
        material.setMaterialType(normalize(request.getRequestType(), "OTHER"));
        material.setMaterialName(defaultText(request.getTitle(), "证明材料"));
        material.setFileUrl(request.getResultFileUrl());
        material.setFileName(defaultText(request.getResultFileName(), material.getMaterialName()));
        material.setIssueDate(LocalDate.now());
        material.setStatus(STATUS_ACTIVE);
        material.setSourceType(SOURCE_DOCUMENT_REQUEST);
        material.setSourceId(request.getId());
        material.setRemark(request.getHandleRemark());
        if (material.getId() == null) {
            employeeMaterialMapper.insert(material);
        } else {
            employeeMaterialMapper.updateById(material);
        }
        refreshTodoTasksQuietly();
    }

    private void normalizePageReq(EmployeeMaterialPageReqVO reqVO) {
        reqVO.setCategory(normalizeNullable(reqVO.getCategory()));
        reqVO.setMaterialType(normalizeNullable(reqVO.getMaterialType()));
        reqVO.setStatus(normalizeNullable(reqVO.getStatus()));
        if (StringUtils.hasText(reqVO.getKeyword())) {
            reqVO.setKeyword(reqVO.getKeyword().trim());
        }
        if (StringUtils.hasText(reqVO.getMaterialName())) {
            reqVO.setMaterialName(reqVO.getMaterialName().trim());
        }
    }

    private void normalizeMaterial(EmployeeMaterialDO material) {
        material.setCategory(normalize(material.getCategory(), "OTHER"));
        material.setMaterialType(normalize(material.getMaterialType(), material.getCategory()));
        material.setStatus(normalize(material.getStatus(),
                StringUtils.hasText(material.getFileUrl()) ? STATUS_ACTIVE : STATUS_MISSING));
        material.setMaterialName(trimRequired(material.getMaterialName(), "材料名称不能为空"));
        material.setFileUrl(trimNullable(material.getFileUrl()));
        material.setFileName(trimNullable(material.getFileName()));
        material.setSourceType(normalizeNullable(material.getSourceType()));
        material.setRemark(trimNullable(material.getRemark()));
        material.setRejectReason(trimNullable(material.getRejectReason()));
    }

    private boolean prepareProfileFilter(EmployeeMaterialPageReqVO reqVO) {
        if (!StringUtils.hasText(reqVO.getProfileName())) {
            return true;
        }
        List<EmployeeProfileDO> profiles = employeeProfileMapper.selectList(new LambdaQueryWrapperX<EmployeeProfileDO>()
                .likeIfPresent(EmployeeProfileDO::getName, reqVO.getProfileName())
                .last("LIMIT 1000"));
        Set<Long> profileIds = new HashSet<>();
        if (profiles != null) {
            for (EmployeeProfileDO profile : profiles) {
                if (profile.getId() != null) {
                    profileIds.add(profile.getId());
                }
            }
        }
        if (profileIds.isEmpty()) {
            return false;
        }
        reqVO.setProfileIds(profileIds);
        return true;
    }

    private EmployeeProfileDO validateProfileExists(Long profileId) {
        EmployeeProfileDO profile = employeeProfileMapper.selectById(profileId);
        if (profile == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "员工档案不存在");
        }
        if (profile.getUserId() == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "员工档案未绑定登录用户");
        }
        return profile;
    }

    private EmployeeMaterialDO validateMaterialExists(Long id) {
        EmployeeMaterialDO material = id == null ? null : employeeMaterialMapper.selectById(id);
        if (material == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "员工材料不存在");
        }
        return material;
    }

    private EmployeeMaterialDO getAccessibleMaterial(Long id) {
        EmployeeMaterialDO material = validateMaterialExists(id);
        if (canViewAll()) {
            return material;
        }
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null || !loginUserId.equals(material.getUserId())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.FORBIDDEN, "无权提交该员工材料");
        }
        return material;
    }

    private EmployeeMaterialDO createSelfMaterial(EmployeeMaterialSubmitReqVO reqVO) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.UNAUTHORIZED);
        }
        EmployeeProfileDO profile = employeeProfileMapper.selectByUserId(loginUserId);
        if (profile == null || profile.getId() == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "员工档案不存在或未绑定登录用户");
        }
        EmployeeMaterialDO material = new EmployeeMaterialDO();
        material.setProfileId(profile.getId());
        material.setUserId(profile.getUserId());
        material.setCategory(normalize(reqVO.getCategory(), "OTHER"));
        material.setMaterialType(normalize(reqVO.getMaterialType(), material.getCategory()));
        material.setMaterialName(trimRequired(reqVO.getMaterialName(), "材料名称不能为空"));
        material.setStatus(STATUS_PENDING_REVIEW);
        material.setSourceType(SOURCE_SELF_UPLOAD);
        return material;
    }

    private void fillPeopleInfo(List<EmployeeMaterialDO> rows, List<EmployeeMaterialRespVO> respList) {
        Set<Long> userIds = new HashSet<>();
        Set<Long> profileIds = new HashSet<>();
        for (EmployeeMaterialDO row : rows) {
            if (row.getUserId() != null) {
                userIds.add(row.getUserId());
            }
            if (row.getReviewerId() != null) {
                userIds.add(row.getReviewerId());
            }
            if (row.getProfileId() != null) {
                profileIds.add(row.getProfileId());
            }
        }
        Map<Long, AdminUserRespDTO> userMap = loadUserMapSafe(userIds);
        Map<Long, EmployeeProfileDO> profileMap = loadProfileMapSafe(profileIds);
        for (EmployeeMaterialRespVO item : respList) {
            AdminUserRespDTO user = userMap.get(item.getUserId());
            if (user != null) {
                item.setUserNickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
            }
            AdminUserRespDTO reviewer = userMap.get(item.getReviewerId());
            if (reviewer != null) {
                item.setReviewerName(StringUtils.hasText(reviewer.getNickname()) ? reviewer.getNickname() : reviewer.getUsername());
            }
            EmployeeProfileDO profile = profileMap.get(item.getProfileId());
            if (profile != null) {
                item.setProfileName(profile.getName());
            }
        }
    }

    private void fillExpireDays(List<EmployeeMaterialRespVO> respList) {
        LocalDate today = LocalDate.now();
        for (EmployeeMaterialRespVO item : respList) {
            if (item.getExpireDate() != null) {
                long expireDays = ChronoUnit.DAYS.between(today, item.getExpireDate());
                item.setExpireDays(expireDays);
                if (expireDays < 0 && STATUS_ACTIVE.equals(item.getStatus())) {
                    item.setStatus(STATUS_EXPIRED);
                }
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
            log.warn("Failed to load admin users for employee material: {}", ex.getMessage());
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

    private boolean canViewAll() {
        try {
            return securityFrameworkService.hasPermission(PERMISSION_QUERY)
                    || securityFrameworkService.hasPermission(PERMISSION_MANAGE);
        } catch (Exception ex) {
            log.warn("check employee material permission failed: {}", ex.getMessage());
            return false;
        }
    }

    private void refreshTodoTasksQuietly() {
        try {
            hrTodoTaskService.refreshGeneratedTasks();
        } catch (Exception ex) {
            log.warn("Refresh HR todo tasks after employee material change failed: {}", ex.getMessage());
        }
    }

    private String normalize(String value, String defaultValue) {
        String normalized = normalizeNullable(value);
        return StringUtils.hasText(normalized) ? normalized : defaultValue;
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }

    private String trimNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String trimRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private String defaultText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

}
