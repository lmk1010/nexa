package com.kyx.service.hr.service.reminder;

import com.kyx.foundation.common.exception.enums.GlobalErrorCodeConstants;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.hr.controller.admin.reminder.vo.HrReminderRulePageReqVO;
import com.kyx.service.hr.controller.admin.reminder.vo.HrReminderRuleRespVO;
import com.kyx.service.hr.controller.admin.reminder.vo.HrReminderRuleSaveReqVO;
import com.kyx.service.hr.dal.dataobject.reminder.HrReminderRuleDO;
import com.kyx.service.hr.dal.mysql.reminder.HrReminderRuleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * HR reminder rule service implementation.
 */
@Service
@Validated
public class HrReminderRuleServiceImpl implements HrReminderRuleService {

    @Resource
    private HrReminderRuleMapper reminderRuleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRule(HrReminderRuleSaveReqVO reqVO) {
        normalize(reqVO);
        validateRuleCodeUnique(null, reqVO.getRuleCode());
        HrReminderRuleDO rule = BeanUtils.toBean(reqVO, HrReminderRuleDO.class);
        rule.setEnabled(reqVO.getEnabled() == null || reqVO.getEnabled());
        reminderRuleMapper.insert(rule);
        return rule.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRule(HrReminderRuleSaveReqVO reqVO) {
        if (reqVO.getId() == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "规则ID不能为空");
        }
        normalize(reqVO);
        validateRuleExists(reqVO.getId());
        validateRuleCodeUnique(reqVO.getId(), reqVO.getRuleCode());
        HrReminderRuleDO updateDO = BeanUtils.toBean(reqVO, HrReminderRuleDO.class);
        if (updateDO.getEnabled() == null) {
            updateDO.setEnabled(true);
        }
        reminderRuleMapper.updateById(updateDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRule(Long id) {
        validateRuleExists(id);
        reminderRuleMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableRule(Long id, Boolean enabled) {
        validateRuleExists(id);
        HrReminderRuleDO updateDO = new HrReminderRuleDO();
        updateDO.setId(id);
        updateDO.setEnabled(Boolean.TRUE.equals(enabled));
        reminderRuleMapper.updateById(updateDO);
    }

    @Override
    public HrReminderRuleDO getRule(Long id) {
        return reminderRuleMapper.selectById(id);
    }

    @Override
    public PageResult<HrReminderRuleRespVO> getRulePage(HrReminderRulePageReqVO pageReqVO) {
        PageResult<HrReminderRuleDO> pageResult = reminderRuleMapper.selectPage(pageReqVO);
        return BeanUtils.toBean(pageResult, HrReminderRuleRespVO.class);
    }

    private void validateRuleExists(Long id) {
        if (id == null || reminderRuleMapper.selectById(id) == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND, "提醒规则不存在");
        }
    }

    private void validateRuleCodeUnique(Long id, String ruleCode) {
        HrReminderRuleDO existing = reminderRuleMapper.selectByRuleCode(ruleCode);
        if (existing == null) {
            return;
        }
        if (id == null || !Objects.equals(existing.getId(), id)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.BAD_REQUEST, "提醒规则编码已存在");
        }
    }

    private void normalize(HrReminderRuleSaveReqVO reqVO) {
        reqVO.setRuleCode(trimUpper(reqVO.getRuleCode()));
        reqVO.setBusinessType(trimUpper(reqVO.getBusinessType()));
        reqVO.setTriggerType(trimUpper(reqVO.getTriggerType()));
        reqVO.setRuleName(trim(reqVO.getRuleName()));
    }

    private String trimUpper(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : value;
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : value;
    }

}
