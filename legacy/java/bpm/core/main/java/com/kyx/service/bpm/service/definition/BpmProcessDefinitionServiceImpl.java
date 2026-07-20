package com.kyx.service.bpm.service.definition;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.foundation.common.util.object.PageUtils;
import com.kyx.foundation.tenant.core.context.TenantContextHolder;
import com.kyx.foundation.tenant.core.util.TenantUtils;
import com.kyx.service.bpm.controller.admin.definition.vo.model.BpmModelMetaInfoVO;
import com.kyx.service.bpm.controller.admin.definition.vo.process.BpmProcessDefinitionPageReqVO;
import com.kyx.service.bpm.dal.dataobject.definition.BpmFormDO;
import com.kyx.service.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import com.kyx.service.bpm.dal.mysql.definition.BpmProcessDefinitionInfoMapper;
import com.kyx.service.bpm.enums.definition.BpmCrossTenantModeEnum;
import com.kyx.service.bpm.framework.flowable.core.enums.BpmnModelConstants;
import com.kyx.service.bpm.framework.flowable.core.util.FlowableUtils;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.common.engine.impl.db.SuspensionState;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.Model;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.*;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.foundation.common.util.collection.CollectionUtils.addIfNotNull;
import static com.kyx.foundation.common.util.collection.CollectionUtils.convertMap;
import static com.kyx.foundation.common.util.collection.CollectionUtils.convertSet;
import static com.kyx.service.bpm.enums.ErrorCodeConstants.*;
import static java.util.Collections.emptyList;

/**
 * 流程定义实现
 * 主要进行 Flowable {@link ProcessDefinition} 和 {@link Deployment} 的维护
 *
 * @author yunlongn
 * @author ZJQ
 * @author MK
 */
@Service
@Validated
@Slf4j
public class BpmProcessDefinitionServiceImpl implements BpmProcessDefinitionService {

    @Resource
    private RepositoryService repositoryService;

    @Resource
    private BpmProcessDefinitionInfoMapper processDefinitionMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Override
    public ProcessDefinition getProcessDefinition(String id) {
        return repositoryService.getProcessDefinition(id);
    }

    @Override
    public List<ProcessDefinition> getProcessDefinitionList(Set<String> ids) {
        return repositoryService.createProcessDefinitionQuery().processDefinitionIds(ids).list();
    }

    @Override
    public ProcessDefinition getProcessDefinitionByDeploymentId(String deploymentId) {
        if (StrUtil.isEmpty(deploymentId)) {
            return null;
        }
        return repositoryService.createProcessDefinitionQuery().deploymentId(deploymentId).singleResult();
    }

    @Override
    public List<ProcessDefinition> getProcessDefinitionListByDeploymentIds(Set<String> deploymentIds) {
        if (CollUtil.isEmpty(deploymentIds)) {
            return emptyList();
        }
        return repositoryService.createProcessDefinitionQuery().deploymentIds(deploymentIds).list();
    }

    @Override
    public ProcessDefinition getActiveProcessDefinition(String key) {
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionTenantId(FlowableUtils.getTenantId())
                .processDefinitionKey(key).active().singleResult();
        if (definition != null) {
            return definition;
        }
        List<ProcessDefinition> definitionList = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(key).active()
                .orderByProcessDefinitionVersion().desc()
                .list();
        if (CollUtil.isEmpty(definitionList)) {
            return null;
        }
        Map<String, BpmProcessDefinitionInfoDO> infoMap = convertMap(
                getProcessDefinitionInfoList(convertSet(definitionList, ProcessDefinition::getId)),
                BpmProcessDefinitionInfoDO::getProcessDefinitionId);
        Long tenantId = TenantContextHolder.getTenantId();
        for (ProcessDefinition candidate : definitionList) {
            BpmProcessDefinitionInfoDO info = infoMap.get(candidate.getId());
            if (info != null && hasProcessDefinitionAccess(info, tenantId)) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    public boolean canUserStartProcessDefinition(BpmProcessDefinitionInfoDO processDefinition, Long userId) {
        if (processDefinition == null) {
            return false;
        }
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null && !hasProcessDefinitionAccess(processDefinition, tenantId)) {
            return false;
        }

        // 校验用户是否在允许发起的用户列表中
        if (CollUtil.isNotEmpty(processDefinition.getStartUserIds())) {
            return processDefinition.getStartUserIds().contains(userId);
        }

        // 校验用户是否在允许发起的部门列表中
        if (CollUtil.isNotEmpty(processDefinition.getStartDeptIds())) {
            AdminUserRespDTO user = adminUserApi.getUser(userId).getCheckedData();
            return user != null
                    && user.getDeptId() != null
                    && processDefinition.getStartDeptIds().contains(user.getDeptId());
        }

        // 都为空，则所有人都可以发起
        return true;
    }

    @Override
    public List<Deployment> getDeploymentList(Set<String> ids) {
        if (CollUtil.isEmpty(ids)) {
            return emptyList();
        }
        List<Deployment> list = new ArrayList<>(ids.size());
        for (String id : ids) {
            addIfNotNull(list, getDeployment(id));
        }
        return list;
    }

    @Override
    public Deployment getDeployment(String id) {
        if (StrUtil.isEmpty(id)) {
            return null;
        }
        return repositoryService.createDeploymentQuery().deploymentId(id).singleResult();
    }

    @Override
    public String createProcessDefinition(Model model, BpmModelMetaInfoVO modelMetaInfo,
                                          byte[] bpmnBytes, String simpleJson, BpmFormDO form) {
        // 创建 Deployment 部署
        Deployment deploy = repositoryService.createDeployment()
                .key(model.getKey()).name(model.getName()).category(model.getCategory())
                .addBytes(model.getKey() + BpmnModelConstants.BPMN_FILE_SUFFIX, bpmnBytes)
                .tenantId(FlowableUtils.getTenantId())
                .disableSchemaValidation() // 禁用 XML Schema 验证，因为有自定义的属性
                .deploy();

        // 设置 ProcessDefinition 的 category 分类
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deploy.getId()).singleResult();
        repositoryService.setProcessDefinitionCategory(definition.getId(), model.getCategory());
        // 注意 1，ProcessDefinition 的 key 和 name 是通过 BPMN 中的 <bpmn2:process /> 的 id 和 name 决定
        // 注意 2，目前该项目的设计上，需要保证 Model、Deployment、ProcessDefinition 使用相同的 key，保证关联性。
        //          否则，会导致 ProcessDefinition 的分页无法查询到。
        if (!Objects.equals(definition.getKey(), model.getKey())) {
            throw exception(PROCESS_DEFINITION_KEY_NOT_MATCH, model.getKey(), definition.getKey());
        }
        if (!Objects.equals(definition.getName(), model.getName())) {
            throw exception(PROCESS_DEFINITION_NAME_NOT_MATCH, model.getName(), definition.getName());
        }

        // 插入拓展表
        BpmProcessDefinitionInfoDO definitionDO = BeanUtils.toBean(modelMetaInfo, BpmProcessDefinitionInfoDO.class)
                .setModelId(model.getId()).setCategory(model.getCategory()).setProcessDefinitionId(definition.getId())
                .setModelType(modelMetaInfo.getType()).setSimpleModel(simpleJson);
        if (form != null) {
            definitionDO.setFormFields(form.getFields()).setFormConf(form.getConf());
        } else if (modelMetaInfo.getFormType() != null && modelMetaInfo.getFormType() == 20 && modelMetaInfo.getCustomFormFields() != null) {
            // 业务表单：将自定义字段配置转换为JSON字符串数组
            List<String> customFieldsJson = new ArrayList<>();
            for (BpmModelMetaInfoVO.CustomFormField customField : modelMetaInfo.getCustomFormFields()) {
                try {
                    String fieldJson = String.format("{\"field\":\"%s\",\"title\":\"%s\",\"type\":\"%s\",\"required\":%s}",
                            customField.getField(), customField.getTitle(), customField.getType(), customField.getRequired());
                    customFieldsJson.add(fieldJson);
                } catch (Exception e) {
                    log.warn("[createProcessDefinition][业务表单字段配置转换失败] customField={}", customField, e);
                }
            }
            definitionDO.setFormFields(customFieldsJson);
        }
        processDefinitionMapper.insert(definitionDO);
        return definition.getId();
    }

    @Override
    public void updateProcessDefinitionState(String id, Integer state) {
        ProcessDefinition processDefinition = repositoryService.getProcessDefinition(id);
        if (processDefinition == null) {
            throw exception(PROCESS_DEFINITION_NOT_EXISTS);
        }

        // 激活
        if (Objects.equals(SuspensionState.ACTIVE.getStateCode(), state)) {
            if (processDefinition.isSuspended()) {
                repositoryService.activateProcessDefinitionById(id, false, null);
            }
            return;
        }
        // 挂起
        if (Objects.equals(SuspensionState.SUSPENDED.getStateCode(), state)) {
            // suspendProcessInstances = false，进行中的任务，不进行挂起。
            // 原因：只要新的流程不允许发起即可，老流程继续可以执行。
            if (!processDefinition.isSuspended()) {
                repositoryService.suspendProcessDefinitionById(id, false, null);
            }
            return;
        }
        log.error("[updateProcessDefinitionState][流程定义({}) 修改未知状态({})]", id, state);
    }

    @Override
    public void updateProcessDefinitionSortByModelId(String modelId, Long sort) {
        processDefinitionMapper.updateByModelId(modelId, new BpmProcessDefinitionInfoDO().setSort(sort));
    }

    @Override
    public BpmnModel getProcessDefinitionBpmnModel(String id) {
        return repositoryService.getBpmnModel(id);
    }

    @Override
    public BpmProcessDefinitionInfoDO getProcessDefinitionInfo(String id) {
        return TenantUtils.executeIgnore(() -> processDefinitionMapper.selectByProcessDefinitionId(id));
    }

    @Override
    public List<BpmProcessDefinitionInfoDO> getProcessDefinitionInfoList(Collection<String> ids) {
        return TenantUtils.executeIgnore(() -> processDefinitionMapper.selectListByProcessDefinitionIds(ids));
    }

    @Override
    public PageResult<ProcessDefinition> getProcessDefinitionPage(BpmProcessDefinitionPageReqVO pageVO) {
        List<BpmProcessDefinitionInfoDO> visibleDefinitions = getVisibleProcessDefinitionList();
        if (CollUtil.isEmpty(visibleDefinitions)) {
            return PageResult.empty(0L);
        }
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery()
                .processDefinitionIds(convertSet(visibleDefinitions, BpmProcessDefinitionInfoDO::getProcessDefinitionId));
        if (StrUtil.isNotBlank(pageVO.getKey())) {
            query.processDefinitionKey(pageVO.getKey());
        }
        // 执行查询
        long count = query.count();
        if (count == 0) {
            return PageResult.empty(count);
        }
        List<ProcessDefinition> list = query.orderByProcessDefinitionVersion().desc()
                .listPage(PageUtils.getStart(pageVO), pageVO.getPageSize());
        return new PageResult<>(list, count);
    }

    @Override
    public List<ProcessDefinition> getProcessDefinitionListBySuspensionState(Integer suspensionState) {
        List<BpmProcessDefinitionInfoDO> visibleDefinitions = getVisibleProcessDefinitionList();
        if (CollUtil.isEmpty(visibleDefinitions)) {
            return emptyList();
        }
        // 拼接查询条件
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery()
                .processDefinitionIds(convertSet(visibleDefinitions, BpmProcessDefinitionInfoDO::getProcessDefinitionId));
        if (Objects.equals(SuspensionState.SUSPENDED.getStateCode(), suspensionState)) {
            query.suspended();
        } else if (Objects.equals(SuspensionState.ACTIVE.getStateCode(), suspensionState)) {
            query.active();
        }
        // 执行查询
        return query.list();
    }

    @Override
    public List<BpmProcessDefinitionInfoDO> getVisibleProcessDefinitionList() {
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId == null) {
            return emptyList();
        }
        return TenantUtils.executeIgnore(() -> processDefinitionMapper.selectVisibleList(currentTenantId));
    }

    @Override
    public boolean hasProcessDefinitionAccess(BpmProcessDefinitionInfoDO processDefinitionInfo, Long currentTenantId) {
        if (processDefinitionInfo == null || currentTenantId == null) {
            return false;
        }

        Integer crossTenantMode = processDefinitionInfo.getCrossTenantMode();
        // 默认为租户隔离模式
        if (crossTenantMode == null) {
            crossTenantMode = BpmCrossTenantModeEnum.TENANT_ISOLATED.getMode();
        }

        // 租户隔离模式：只能访问本租户的流程
        if (BpmCrossTenantModeEnum.TENANT_ISOLATED.getMode().equals(crossTenantMode)) {
            return currentTenantId.equals(processDefinitionInfo.getTenantId());
        }

        // 全局可见模式：所有租户都可以访问
        if (BpmCrossTenantModeEnum.GLOBAL_VISIBLE.getMode().equals(crossTenantMode)) {
            return true;
        }

        // 指定租户可见模式：检查当前租户是否在可见列表中
        if (BpmCrossTenantModeEnum.SPECIFIED_TENANTS.getMode().equals(crossTenantMode)) {
            List<Long> visibleTenantIds = processDefinitionInfo.getVisibleTenantIds();
            return visibleTenantIds != null && visibleTenantIds.contains(currentTenantId);
        }

        return false;
    }

}
