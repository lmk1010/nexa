package com.kyx.service.bpm.dal.mysql.definition;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface BpmProcessDefinitionInfoMapper extends BaseMapperX<BpmProcessDefinitionInfoDO> {

    default List<BpmProcessDefinitionInfoDO> selectListByProcessDefinitionIds(Collection<String> processDefinitionIds) {
        return selectList(BpmProcessDefinitionInfoDO::getProcessDefinitionId, processDefinitionIds);
    }

    default BpmProcessDefinitionInfoDO selectByProcessDefinitionId(String processDefinitionId) {
        return selectOne(BpmProcessDefinitionInfoDO::getProcessDefinitionId, processDefinitionId);
    }

    default void updateByModelId(String modelId, BpmProcessDefinitionInfoDO updateObj) {
        update(updateObj,
                new LambdaQueryWrapperX<BpmProcessDefinitionInfoDO>().eq(BpmProcessDefinitionInfoDO::getModelId, modelId));
    }

    /**
     * 查询当前租户可见的流程定义列表（支持跨租户）
     *
     * @param tenantId 当前租户ID
     * @return 可见的流程定义列表
     */
    List<BpmProcessDefinitionInfoDO> selectVisibleList(Long tenantId);

}
