package com.kyx.service.hr.dal.mysql.integration;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.dal.dataobject.integration.DingTalkSyncSnapshotDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Mapper
public interface DingTalkSyncSnapshotMapper extends BaseMapperX<DingTalkSyncSnapshotDO> {

    default DingTalkSyncSnapshotDO selectByBatchIdAndKey(String snapshotBatchId, String snapshotKey) {
        if (!StringUtils.hasText(snapshotBatchId) || !StringUtils.hasText(snapshotKey)) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<DingTalkSyncSnapshotDO>()
                .eq(DingTalkSyncSnapshotDO::getSnapshotBatchId, snapshotBatchId.trim())
                .eq(DingTalkSyncSnapshotDO::getSnapshotKey, snapshotKey.trim())
                .last("LIMIT 1"));
    }

    default List<DingTalkSyncSnapshotDO> selectListByBatchId(String snapshotBatchId) {
        if (!StringUtils.hasText(snapshotBatchId)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<DingTalkSyncSnapshotDO>()
                .eq(DingTalkSyncSnapshotDO::getSnapshotBatchId, snapshotBatchId.trim())
                .orderByAsc(DingTalkSyncSnapshotDO::getId));
    }
}
