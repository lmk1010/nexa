package com.kyx.service.hr.service.joblevel;

import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.hr.controller.admin.joblevel.vo.JobLevelOptionVO;
import com.kyx.service.hr.controller.admin.joblevel.vo.JobLevelPageReqVO;
import com.kyx.service.hr.controller.admin.joblevel.vo.JobLevelRespVO;
import com.kyx.service.hr.controller.admin.joblevel.vo.JobLevelSaveReqVO;
import com.kyx.service.hr.dal.dataobject.joblevel.JobLevelDO;
import com.kyx.service.hr.dal.dataobject.sequence.SequenceDO;
import com.kyx.service.hr.dal.dataobject.tenant.TenantDO;
import com.kyx.service.hr.dal.mysql.joblevel.JobLevelMapper;
import com.kyx.service.hr.dal.mysql.tenant.TenantMapper;
import com.kyx.service.hr.service.sequence.SequenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.kyx.service.hr.enums.ErrorCodeConstants.*;

/**
 * 职级管理 Service 实现类
 *
 * @author MK
 */
@Service
@Validated
@Slf4j
public class JobLevelServiceImpl implements JobLevelService {

    @Resource
    private JobLevelMapper jobLevelMapper;

    @Resource
    private TenantMapper tenantMapper;

    @Resource
    private SequenceService sequenceService;

    @Override
    public Long createJobLevel(JobLevelSaveReqVO createReqVO) {
        normalizeSaveReq(createReqVO);
        validateLevelNameUnique(null, createReqVO.getLevelName());
        // 校验职级编码唯一性
        validateLevelCodeUnique(null, createReqVO.getLevelCode());
        
        // 如果设置了序列ID，校验序列是否存在
        if (createReqVO.getSequenceId() != null) {
            if (sequenceService.getSequence(createReqVO.getSequenceId()) == null) {
                throw ServiceExceptionUtil.exception(SEQUENCE_NOT_EXISTS);
            }
        }

        // 插入
        JobLevelDO jobLevel = BeanUtils.toBean(createReqVO, JobLevelDO.class);
        jobLevelMapper.insert(jobLevel);
        return jobLevel.getId();
    }

    @Override
    public void updateJobLevel(JobLevelSaveReqVO updateReqVO) {
        normalizeSaveReq(updateReqVO);
        // 校验存在
        validateJobLevelExists(updateReqVO.getId());
        
        validateLevelNameUnique(updateReqVO.getId(), updateReqVO.getLevelName());
        // 校验职级编码唯一性
        validateLevelCodeUnique(updateReqVO.getId(), updateReqVO.getLevelCode());
        
        // 如果设置了序列ID，校验序列是否存在
        if (updateReqVO.getSequenceId() != null) {
            if (sequenceService.getSequence(updateReqVO.getSequenceId()) == null) {
                throw ServiceExceptionUtil.exception(SEQUENCE_NOT_EXISTS);
            }
        }

        // 更新
        JobLevelDO updateObj = BeanUtils.toBean(updateReqVO, JobLevelDO.class);
        jobLevelMapper.updateById(updateObj);
    }

    @Override
    public void deleteJobLevel(Long id) {
        // 校验存在
        validateJobLevelExists(id);
        
        // 删除
        jobLevelMapper.deleteById(id);
    }

    @Override
    public JobLevelDO getJobLevel(Long id) {
        return jobLevelMapper.selectById(id);
    }

    @Override
    public PageResult<JobLevelRespVO> getJobLevelPage(JobLevelPageReqVO pageReqVO) {
        PageResult<JobLevelDO> pageResult = jobLevelMapper.selectPage(pageReqVO);
        List<JobLevelRespVO> respList = buildJobLevelRespVOList(pageResult.getList());
        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    public List<JobLevelRespVO> getJobLevelList() {
        List<JobLevelDO> list = jobLevelMapper.selectAll();
        return buildJobLevelRespVOList(list);
    }

    @Override
    public List<JobLevelOptionVO> getJobLevelOptions() {
        List<JobLevelDO> list = jobLevelMapper.selectListByStatus(CommonStatusEnum.ENABLE.getStatus());
        Map<String, JobLevelOptionVO> optionMap = new LinkedHashMap<>();
        for (JobLevelDO jobLevel : list) {
            if (!StringUtils.hasText(jobLevel.getLevelName())) {
                continue;
            }
            String label = jobLevel.getLevelName().trim();
            if (optionMap.containsKey(label)) {
                log.warn("Ignore duplicate HR job level option: levelName={}, id={}", label, jobLevel.getId());
                continue;
            }
            JobLevelOptionVO option = new JobLevelOptionVO();
            option.setValue(jobLevel.getId());
            option.setLabel(label);
            optionMap.put(label, option);
        }
        return new ArrayList<>(optionMap.values());
    }

    @Override
    public List<JobLevelRespVO> getJobLevelListBySequenceId(Long sequenceId) {
        List<JobLevelDO> list = jobLevelMapper.selectListBySequenceId(sequenceId);
        return buildJobLevelRespVOList(list);
    }

    @Override
    public JobLevelDO getJobLevelByCode(String levelCode) {
        return jobLevelMapper.selectByLevelCode(levelCode);
    }

    @Override
    public void validateJobLevelExists(Long id) {
        if (jobLevelMapper.selectById(id) == null) {
            throw ServiceExceptionUtil.exception(JOB_LEVEL_NOT_EXISTS);
        }
    }

    @Override
    public void validateLevelCodeUnique(Long id, String levelCode) {
        JobLevelDO jobLevel = jobLevelMapper.selectByLevelCode(levelCode);
        if (jobLevel == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的职级
        if (id == null) {
            throw ServiceExceptionUtil.exception(JOB_LEVEL_CODE_DUPLICATE);
        }
        if (!jobLevel.getId().equals(id)) {
            throw ServiceExceptionUtil.exception(JOB_LEVEL_CODE_DUPLICATE);
        }
    }

    private void validateLevelNameUnique(Long id, String levelName) {
        JobLevelDO jobLevel = jobLevelMapper.selectFirstByLevelName(levelName);
        if (jobLevel == null) {
            return;
        }
        if (id == null || !jobLevel.getId().equals(id)) {
            throw ServiceExceptionUtil.exception(JOB_LEVEL_NAME_DUPLICATE);
        }
    }

    private void normalizeSaveReq(JobLevelSaveReqVO reqVO) {
        reqVO.setLevelName(reqVO.getLevelName().trim());
        reqVO.setLevelCode(reqVO.getLevelCode().trim());
        if (StringUtils.hasText(reqVO.getDescription())) {
            reqVO.setDescription(reqVO.getDescription().trim());
        }
    }

    /**
     * 构建职级响应VO列表，包含序列名称和租户名称
     */
    private List<JobLevelRespVO> buildJobLevelRespVOList(List<JobLevelDO> jobLevelList) {
        if (jobLevelList.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取序列信息
        List<Long> sequenceIds = jobLevelList.stream()
                .map(JobLevelDO::getSequenceId)
                .filter(sequenceId -> sequenceId != null)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> sequenceMap = new HashMap<>();
        if (!sequenceIds.isEmpty()) {
            List<SequenceDO> sequenceList = sequenceService.getSequenceList(sequenceIds);
            sequenceMap = sequenceList.stream()
                    .collect(Collectors.toMap(SequenceDO::getId, SequenceDO::getSequenceName));
        }

        // 获取租户信息
        List<Long> tenantIds = jobLevelList.stream()
                .map(JobLevelDO::getTenantId)
                .filter(tenantId -> tenantId != null)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> tenantMap = new HashMap<>();
        if (!tenantIds.isEmpty()) {
            List<TenantDO> tenantList = tenantMapper.selectBatchIds(tenantIds);
            tenantMap = tenantList.stream()
                    .collect(Collectors.toMap(TenantDO::getId, TenantDO::getName));
        }

        // 转换为响应VO
        final Map<Long, String> finalSequenceMap = sequenceMap;
        final Map<Long, String> finalTenantMap = tenantMap;
        return BeanUtils.toBean(jobLevelList, JobLevelRespVO.class, respVO -> {
            if (respVO.getSequenceId() != null) {
                respVO.setSequenceName(finalSequenceMap.get(respVO.getSequenceId()));
            }
            if (respVO.getTenantId() != null) {
                respVO.setTenantName(finalTenantMap.get(respVO.getTenantId()));
            }
        });
    }

}
