package com.kyx.service.bpm.dal.mysql.task;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.bpm.controller.admin.task.vo.instance.BpmProcessInstanceCopyPageReqVO;
import com.kyx.service.bpm.dal.dataobject.task.BpmProcessInstanceCopyDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BpmProcessInstanceCopyMapper extends BaseMapperX<BpmProcessInstanceCopyDO> {

    default PageResult<BpmProcessInstanceCopyDO> selectPage(Long loginUserId, BpmProcessInstanceCopyPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<BpmProcessInstanceCopyDO>()
                .eqIfPresent(BpmProcessInstanceCopyDO::getUserId, loginUserId)
                .likeIfPresent(BpmProcessInstanceCopyDO::getProcessInstanceName, reqVO.getProcessInstanceName())
                .betweenIfPresent(BpmProcessInstanceCopyDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(BpmProcessInstanceCopyDO::getId));
    }

    default void deleteByProcessInstanceId(String processInstanceId) {
        delete(BpmProcessInstanceCopyDO::getProcessInstanceId, processInstanceId);
    }

    default List<BpmProcessInstanceCopyDO> selectListByProcessInstanceId(String processInstanceId) {
        return selectList(BpmProcessInstanceCopyDO::getProcessInstanceId, processInstanceId);
    }

    default List<BpmProcessInstanceCopyDO> selectRecentList(Integer limit) {
        int safeLimit = limit == null ? 200 : Math.max(1, Math.min(limit, 500));
        return selectList(new LambdaQueryWrapperX<BpmProcessInstanceCopyDO>()
                .orderByDesc(BpmProcessInstanceCopyDO::getId)
                .last("LIMIT " + safeLimit));
    }

}
