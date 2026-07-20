package com.kyx.service.hr.service.questionnaire;

import cn.hutool.core.util.IdUtil;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePublicLinkRespVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePublicLinkSaveReqVO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnairePublicLinkDO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnaireDO;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnairePublicLinkMapper;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnaireMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

import static com.kyx.service.hr.enums.ErrorCodeConstants.QUESTIONNAIRE_PUBLIC_LINK_NOT_EXISTS;

/**
 * 问卷公开链接 Service 实现类
 */
@Service
@Validated
@Slf4j
public class QuestionnairePublicLinkServiceImpl implements QuestionnairePublicLinkService {

    @Resource
    private QuestionnairePublicLinkMapper publicLinkMapper;
    @Resource
    private QuestionnaireMapper questionnaireMapper;

    @Override
    public Long createPublicLink(QuestionnairePublicLinkSaveReqVO createReqVO) {
        QuestionnairePublicLinkDO link = BeanUtils.toBean(createReqVO, QuestionnairePublicLinkDO.class);
        // 生成唯一token
        link.setToken(IdUtil.fastSimpleUUID());
        link.setSubmitCount(0);
        if (link.getEnabled() == null) {
            link.setEnabled(1);
        }
        publicLinkMapper.insert(link);
        return link.getId();
    }

    @Override
    public void updatePublicLink(QuestionnairePublicLinkSaveReqVO updateReqVO) {
        validateExists(updateReqVO.getId());
        QuestionnairePublicLinkDO updateObj = BeanUtils.toBean(updateReqVO, QuestionnairePublicLinkDO.class);

        // 密码处理逻辑：
        // - 如果password字段为null，表示不修改密码，保持原值
        // - 如果password字段为空字符串""，表示清除密码
        // - 如果password字段有值，表示设置新密码
        String password = updateReqVO.getPassword();
        boolean clearPassword = password != null && password.isEmpty();
        boolean setNewPassword = password != null && !password.isEmpty();

        if (clearPassword) {
            // 清除密码：设置为null，但updateById会忽略null，需要显式更新
            updateObj.setPassword(null);
        } else if (!setNewPassword) {
            // 不修改密码：设置为null让updateById忽略此字段
            updateObj.setPassword(null);
        }
        // 如果setNewPassword为true，password已经在BeanUtils.toBean中设置好了

        publicLinkMapper.updateById(updateObj);

        // 显式清除密码（因为updateById忽略null字段）
        if (clearPassword) {
            publicLinkMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<QuestionnairePublicLinkDO>()
                .eq(QuestionnairePublicLinkDO::getId, updateReqVO.getId())
                .set(QuestionnairePublicLinkDO::getPassword, null));
        }
    }

    @Override
    public void deletePublicLink(Long id) {
        validateExists(id);
        publicLinkMapper.deleteById(id);
    }

    @Override
    public QuestionnairePublicLinkRespVO getPublicLink(Long id) {
        QuestionnairePublicLinkDO link = validateExists(id);
        return convertToRespVO(link);
    }

    @Override
    public List<QuestionnairePublicLinkRespVO> getPublicLinksByQuestionnaireId(Long questionnaireId) {
        List<QuestionnairePublicLinkDO> list = publicLinkMapper.selectList(
            QuestionnairePublicLinkDO::getQuestionnaireId, questionnaireId);
        return list.stream().map(this::convertToRespVO).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<QuestionnairePublicLinkRespVO> getPublicLinksByPublishId(Long publishId) {
        List<QuestionnairePublicLinkDO> list = publicLinkMapper.selectList(
            QuestionnairePublicLinkDO::getPublishId, publishId);
        return list.stream().map(this::convertToRespVO).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public QuestionnairePublicLinkDO getPublicLinkByToken(String token) {
        return publicLinkMapper.selectByToken(token);
    }

    @Override
    public boolean verifyPassword(String token, String password) {
        QuestionnairePublicLinkDO link = publicLinkMapper.selectByToken(token);
        if (link == null) {
            return false;
        }
        if (link.getPassword() == null || link.getPassword().isEmpty()) {
            return true; // 无密码
        }
        return Objects.equals(link.getPassword(), password);
    }

    @Override
    public void incrementSubmitCount(Long linkId) {
        QuestionnairePublicLinkDO link = publicLinkMapper.selectById(linkId);
        if (link != null) {
            link.setSubmitCount((link.getSubmitCount() == null ? 0 : link.getSubmitCount()) + 1);
            publicLinkMapper.updateById(link);
        }
    }

    private QuestionnairePublicLinkDO validateExists(Long id) {
        QuestionnairePublicLinkDO link = publicLinkMapper.selectById(id);
        if (link == null) {
            throw ServiceExceptionUtil.exception(QUESTIONNAIRE_PUBLIC_LINK_NOT_EXISTS);
        }
        return link;
    }

    private QuestionnairePublicLinkRespVO convertToRespVO(QuestionnairePublicLinkDO link) {
        QuestionnairePublicLinkRespVO vo = BeanUtils.toBean(link, QuestionnairePublicLinkRespVO.class);
        // 获取问卷名称
        QuestionnaireDO questionnaire = questionnaireMapper.selectById(link.getQuestionnaireId());
        if (questionnaire != null) {
            vo.setQuestionnaireName(questionnaire.getName());
        }
        vo.setHasPassword(link.getPassword() != null && !link.getPassword().isEmpty());
        // 完整链接由前端拼接
        return vo;
    }

}
