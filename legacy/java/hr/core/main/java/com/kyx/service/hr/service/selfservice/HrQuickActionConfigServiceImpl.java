package com.kyx.service.hr.service.selfservice;

import com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.hr.controller.admin.selfservice.vo.HrQuickActionConfigRespVO;
import com.kyx.service.hr.controller.admin.selfservice.vo.HrQuickActionConfigSaveReqVO;
import com.kyx.service.hr.controller.admin.selfservice.vo.HrSelfServiceHomeRespVO;
import com.kyx.service.hr.dal.dataobject.selfservice.HrEmployeeQuickActionConfigDO;
import com.kyx.service.hr.dal.mysql.selfservice.HrEmployeeQuickActionConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@Validated
public class HrQuickActionConfigServiceImpl implements HrQuickActionConfigService {

    private static final Integer STATUS_ENABLE = 0;
    private static final Integer STATUS_DISABLE = 1;
    private static final String LEGACY_EXAM_ACTION_CODE = "EXAM";
    private static final String LEGACY_EXAM_ROUTE_PATH = "/hr/exam/my";

    @Resource
    private HrEmployeeQuickActionConfigMapper quickActionConfigMapper;

    @Override
    public List<HrQuickActionConfigRespVO> getList(Boolean enabledOnly) {
        List<HrEmployeeQuickActionConfigDO> configs = quickActionConfigMapper.selectListOrdered(enabledOnly);
        if (configs == null || configs.isEmpty()) {
            return new ArrayList<>();
        }
        List<HrEmployeeQuickActionConfigDO> visibleConfigs = new ArrayList<>(configs);
        visibleConfigs.removeIf(this::isLegacyExamAction);
        return BeanUtils.toBean(visibleConfigs, HrQuickActionConfigRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(HrQuickActionConfigSaveReqVO reqVO) {
        normalize(reqVO);
        HrEmployeeQuickActionConfigDO duplicate = quickActionConfigMapper.selectByActionCode(reqVO.getActionCode());
        if (duplicate != null && !Objects.equals(duplicate.getId(), reqVO.getId())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "入口编码已存在");
        }
        HrEmployeeQuickActionConfigDO saveDO = BeanUtils.toBean(reqVO, HrEmployeeQuickActionConfigDO.class);
        if (reqVO.getId() == null) {
            quickActionConfigMapper.insert(saveDO);
        } else {
            if (quickActionConfigMapper.selectById(reqVO.getId()) == null) {
                throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "快捷入口配置不存在");
            }
            quickActionConfigMapper.updateById(saveDO);
        }
        return saveDO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(Long id) {
        if (quickActionConfigMapper.selectById(id) == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "快捷入口配置不存在");
        }
        quickActionConfigMapper.deleteById(id);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer resetDefault() {
        int changed = 0;
        for (DefaultAction action : defaultActions()) {
            HrEmployeeQuickActionConfigDO existing = quickActionConfigMapper.selectByActionCode(action.actionCode);
            HrEmployeeQuickActionConfigDO saveDO = new HrEmployeeQuickActionConfigDO();
            saveDO.setActionCode(action.actionCode);
            saveDO.setActionName(action.actionName);
            saveDO.setIcon(action.icon);
            saveDO.setRoutePath(action.routePath);
            saveDO.setCategory(action.category);
            saveDO.setPermissionCode(action.permissionCode);
            saveDO.setSortOrder(action.sortOrder);
            saveDO.setStatus(STATUS_ENABLE);
            if (existing == null) {
                quickActionConfigMapper.insert(saveDO);
            } else {
                saveDO.setId(existing.getId());
                quickActionConfigMapper.updateById(saveDO);
            }
            changed++;
        }
        changed += disableLegacyExamAction();
        return changed;
    }

    @Override
    public List<HrSelfServiceHomeRespVO.QuickAction> getHomeActions() {
        List<HrEmployeeQuickActionConfigDO> configs = quickActionConfigMapper.selectListOrdered(true);
        if (configs == null || configs.isEmpty()) {
            return defaultHomeActions();
        }
        List<HrSelfServiceHomeRespVO.QuickAction> actions = new ArrayList<>();
        for (HrEmployeeQuickActionConfigDO config : configs) {
            if (isLegacyExamAction(config)) {
                continue;
            }
            HrSelfServiceHomeRespVO.QuickAction action = new HrSelfServiceHomeRespVO.QuickAction();
            action.setTitle(config.getActionName());
            action.setIcon(config.getIcon());
            action.setPath(config.getRoutePath());
            action.setCategory(config.getCategory());
            actions.add(action);
        }
        return actions.isEmpty() ? defaultHomeActions() : actions;
    }

    private void normalize(HrQuickActionConfigSaveReqVO reqVO) {
        reqVO.setActionCode(reqVO.getActionCode().trim().toUpperCase());
        reqVO.setActionName(reqVO.getActionName().trim());
        reqVO.setIcon(reqVO.getIcon().trim());
        reqVO.setRoutePath(reqVO.getRoutePath().trim());
        if (StringUtils.hasText(reqVO.getCategory())) {
            reqVO.setCategory(reqVO.getCategory().trim());
        }
        if (StringUtils.hasText(reqVO.getPermissionCode())) {
            reqVO.setPermissionCode(reqVO.getPermissionCode().trim());
        }
        if (reqVO.getSortOrder() == null) {
            reqVO.setSortOrder(100);
        }
        if (reqVO.getStatus() == null) {
            reqVO.setStatus(STATUS_ENABLE);
        }
        if (!STATUS_ENABLE.equals(reqVO.getStatus()) && !STATUS_DISABLE.equals(reqVO.getStatus())) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "状态只支持启用或停用");
        }
    }

    private List<HrSelfServiceHomeRespVO.QuickAction> defaultHomeActions() {
        List<HrSelfServiceHomeRespVO.QuickAction> actions = new ArrayList<>();
        for (DefaultAction action : defaultActions()) {
            HrSelfServiceHomeRespVO.QuickAction item = new HrSelfServiceHomeRespVO.QuickAction();
            item.setTitle(action.actionName);
            item.setIcon(action.icon);
            item.setPath(action.routePath);
            item.setCategory(action.category);
            actions.add(item);
        }
        return actions;
    }

    private List<DefaultAction> defaultActions() {
        return Arrays.asList(
                new DefaultAction("LEARNING_CENTER", "学习中心", "lucide:graduation-cap", "/hr/learning-center", "learning", "hr:learning:self", 115),
                new DefaultAction("CLOCK_IN", "打卡", "lucide:map-pin-check", "/attendance/clock-in", "attendance", "", 10),
                new DefaultAction("ATTENDANCE_RECORDS", "我的考勤", "lucide:calendar-days", "/attendance/records?scope=my", "attendance", "", 20),
                new DefaultAction("LEAVE_APPLY", "申请请假", "lucide:calendar-minus", "/administrative/leave/create", "application", "", 30),
                new DefaultAction("TRIP_APPLY", "申请出差", "lucide:plane", "/administrative/trip/create", "application", "", 40),
                new DefaultAction("CORRECTION_APPLY", "补卡外勤", "lucide:file-check-2", "/attendance/corrections", "application", "", 50),
                new DefaultAction("OVERTIME_APPLY", "加班调休", "lucide:timer-reset", "/attendance/overtime", "application", "", 60),
                new DefaultAction("ATTENDANCE_MONTHLY_CONFIRM", "月度确认", "lucide:calendar-check", "/attendance/my-monthly-confirm", "attendance", "", 70),
                new DefaultAction("PAYSLIP", "我的工资条", "lucide:receipt-text", "/hr/my-payslip", "salary", "hr:payslip:self", 80),
                new DefaultAction("PROFILE_CHANGE", "资料变更", "lucide:user-pen", "/hr/profile-change?scope=mine", "application", "", 90),
                new DefaultAction("DOCUMENT_REQUEST", "证明申请", "lucide:file-text", "/hr/document-request?scope=mine", "application", "", 100),
                new DefaultAction("EMPLOYEE_MATERIAL", "我的材料", "lucide:folder-open", "/hr/employee-material?scope=mine", "application", "hr:employee-material:self", 110),
                new DefaultAction("APPLICATIONS", "我的申请", "lucide:folder-clock", "/hr/self-service/applications", "application", "", 120),
                new DefaultAction("BPM_TASK", "我的流程", "lucide:workflow", "/bpm/task/my", "process", "", 150),
                new DefaultAction("HR_TODO", "我的待办", "lucide:list-checks", "/hr/todo?scope=mine", "process", "", 160));
    }

    private int disableLegacyExamAction() {
        HrEmployeeQuickActionConfigDO legacyExam = quickActionConfigMapper.selectByActionCode(LEGACY_EXAM_ACTION_CODE);
        if (legacyExam == null) {
            return 0;
        }
        boolean changed = false;
        if (!STATUS_DISABLE.equals(legacyExam.getStatus())) {
            legacyExam.setStatus(STATUS_DISABLE);
            changed = true;
        }
        if ("我的考试".equals(legacyExam.getActionName())) {
            legacyExam.setActionName("考试");
            changed = true;
        }
        if (LEGACY_EXAM_ROUTE_PATH.equals(legacyExam.getRoutePath())) {
            legacyExam.setRoutePath("/hr/learning-center?tab=exam");
            changed = true;
        }
        if (!changed) {
            return 0;
        }
        quickActionConfigMapper.updateById(legacyExam);
        return 1;
    }

    private boolean isLegacyExamAction(HrEmployeeQuickActionConfigDO config) {
        return LEGACY_EXAM_ACTION_CODE.equals(config.getActionCode())
                || LEGACY_EXAM_ROUTE_PATH.equals(config.getRoutePath());
    }

    private static class DefaultAction {
        private final String actionCode;
        private final String actionName;
        private final String icon;
        private final String routePath;
        private final String category;
        private final String permissionCode;
        private final Integer sortOrder;

        private DefaultAction(String actionCode, String actionName, String icon, String routePath,
                              String category, String permissionCode, Integer sortOrder) {
            this.actionCode = actionCode;
            this.actionName = actionName;
            this.icon = icon;
            this.routePath = routePath;
            this.category = category;
            this.permissionCode = permissionCode;
            this.sortOrder = sortOrder;
        }
    }

}
