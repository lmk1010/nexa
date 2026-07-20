package com.kyx.service.hr.service.sequence;

import com.kyx.foundation.common.exception.util.ServiceExceptionUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.hr.controller.admin.sequence.vo.SequenceOptionVO;
import com.kyx.service.hr.controller.admin.sequence.vo.SequencePageReqVO;
import com.kyx.service.hr.controller.admin.sequence.vo.SequenceRespVO;
import com.kyx.service.hr.controller.admin.sequence.vo.SequenceSaveReqVO;
import com.kyx.service.hr.dal.dataobject.sequence.SequenceDO;
import com.kyx.service.hr.dal.mysql.sequence.SequenceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.kyx.service.hr.enums.ErrorCodeConstants.*;

/**
 * 序列管理 Service 实现类
 *
 * @author MK
 */
@Service
@Validated
@Slf4j
public class SequenceServiceImpl implements SequenceService {

    @Resource
    private SequenceMapper sequenceMapper;

    @Override
    public Long createSequence(SequenceSaveReqVO createReqVO) {
        // 校验序列名称不重复
        validateSequenceNameUnique(null, createReqVO.getSequenceName());
        
        // 校验上级序列存在
        if (createReqVO.getParentId() != null) {
            validateSequenceExists(createReqVO.getParentId());
        }

        // 插入
        SequenceDO sequence = BeanUtils.toBean(createReqVO, SequenceDO.class);
        
        // 计算层级
        if (createReqVO.getParentId() != null) {
            SequenceDO parent = sequenceMapper.selectById(createReqVO.getParentId());
            sequence.setLevel(parent.getLevel() + 1);
        } else {
            sequence.setLevel(1);
        }
        
        // 设置默认排序
        if (sequence.getSort() == null) {
            sequence.setSort(0);
        }
        
        sequenceMapper.insert(sequence);
        return sequence.getId();
    }

    @Override
    public void updateSequence(SequenceSaveReqVO updateReqVO) {
        // 校验存在
        validateSequenceExists(updateReqVO.getId());
        
        // 校验序列名称不重复
        validateSequenceNameUnique(updateReqVO.getId(), updateReqVO.getSequenceName());
        
        // 校验上级序列存在且不能是自己及其子级
        if (updateReqVO.getParentId() != null) {
            validateParentSequence(updateReqVO.getId(), updateReqVO.getParentId());
        }

        // 更新
        SequenceDO updateObj = BeanUtils.toBean(updateReqVO, SequenceDO.class);
        
        // 重新计算层级
        if (updateReqVO.getParentId() != null) {
            SequenceDO parent = sequenceMapper.selectById(updateReqVO.getParentId());
            updateObj.setLevel(parent.getLevel() + 1);
        } else {
            updateObj.setLevel(1);
        }
        
        sequenceMapper.updateById(updateObj);
    }

    @Override
    public void deleteSequence(Long id) {
        // 校验存在
        validateSequenceExists(id);
        
        // 校验是否存在子序列
        List<SequenceDO> children = sequenceMapper.selectListByParentId(id);
        if (!children.isEmpty()) {
            throw ServiceExceptionUtil.exception(SEQUENCE_EXISTS_CHILDREN);
        }

        // 删除
        sequenceMapper.deleteById(id);
    }

    private void validateSequenceExists(Long id) {
        if (sequenceMapper.selectById(id) == null) {
            throw ServiceExceptionUtil.exception(SEQUENCE_NOT_EXISTS);
        }
    }

    private void validateSequenceNameUnique(Long id, String sequenceName) {
        SequenceDO sequence = sequenceMapper.selectBySequenceName(sequenceName);
        if (sequence == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的序列
        if (id == null) {
            throw ServiceExceptionUtil.exception(SEQUENCE_NAME_DUPLICATE);
        }
        if (!sequence.getId().equals(id)) {
            throw ServiceExceptionUtil.exception(SEQUENCE_NAME_DUPLICATE);
        }
    }

    private void validateParentSequence(Long id, Long parentId) {
        // 校验上级序列存在
        validateSequenceExists(parentId);
        
        // 不能设置自己为上级
        if (id.equals(parentId)) {
            throw ServiceExceptionUtil.exception(SEQUENCE_PARENT_ERROR);
        }
        
        // 不能设置自己的子级为上级（避免循环引用）
        List<Long> childIds = getChildSequenceIds(id);
        if (childIds.contains(parentId)) {
            throw ServiceExceptionUtil.exception(SEQUENCE_PARENT_ERROR);
        }
    }

    private List<Long> getChildSequenceIds(Long parentId) {
        List<Long> childIds = new ArrayList<>();
        List<SequenceDO> children = sequenceMapper.selectListByParentId(parentId);
        for (SequenceDO child : children) {
            childIds.add(child.getId());
            // 递归获取子级的子级
            childIds.addAll(getChildSequenceIds(child.getId()));
        }
        return childIds;
    }

    @Override
    public SequenceDO getSequence(Long id) {
        return sequenceMapper.selectById(id);
    }

    @Override
    public PageResult<SequenceRespVO> getSequencePage(SequencePageReqVO pageReqVO) {
        PageResult<SequenceDO> pageResult = sequenceMapper.selectPage(pageReqVO);
        
        // 转换为 VO 并设置上级序列名称
        List<SequenceRespVO> list = new ArrayList<>();
        Map<Long, String> parentNameMap = new HashMap<>();
        
        for (SequenceDO sequence : pageResult.getList()) {
            SequenceRespVO vo = BeanUtils.toBean(sequence, SequenceRespVO.class);
            
            // 设置上级序列名称
            if (sequence.getParentId() != null) {
                String parentName = parentNameMap.get(sequence.getParentId());
                if (parentName == null) {
                    SequenceDO parent = sequenceMapper.selectById(sequence.getParentId());
                    parentName = parent != null ? parent.getSequenceName() : "";
                    parentNameMap.put(sequence.getParentId(), parentName);
                }
                vo.setParentName(parentName);
            }
            
            list.add(vo);
        }
        
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public List<SequenceDO> getSequenceList(List<Long> ids) {
        return sequenceMapper.selectBatchIds(ids);
    }

    @Override
    public List<SequenceRespVO> getSequenceTree() {
        List<SequenceDO> sequences = sequenceMapper.selectTreeList();
        return buildSequenceTree(sequences, null);
    }

    private List<SequenceRespVO> buildSequenceTree(List<SequenceDO> sequences, Long parentId) {
        List<SequenceRespVO> result = new ArrayList<>();
        
        for (SequenceDO sequence : sequences) {
            if ((parentId == null && sequence.getParentId() == null) || 
                (parentId != null && parentId.equals(sequence.getParentId()))) {
                
                SequenceRespVO vo = BeanUtils.toBean(sequence, SequenceRespVO.class);
                
                // 递归设置子序列
                List<SequenceRespVO> children = buildSequenceTree(sequences, sequence.getId());
                if (!children.isEmpty()) {
                    vo.setChildren(children);
                }
                
                result.add(vo);
            }
        }
        
        return result;
    }

    @Override
    public List<SequenceOptionVO> getSequenceOptions() {
        List<SequenceDO> sequences = sequenceMapper.selectListByStatus(0); // 只返回启用的序列
        return sequences.stream()
                .map(sequence -> {
                    SequenceOptionVO option = new SequenceOptionVO();
                    option.setValue(sequence.getId());
                    option.setLabel(sequence.getSequenceName());
                    return option;
                })
                .collect(Collectors.toList());
    }

    @Override
    public SequenceDO getSequenceByName(String sequenceName) {
        return sequenceMapper.selectBySequenceName(sequenceName);
    }

    @Override
    public List<SequenceDO> getSequenceListByStatus(Integer status) {
        return sequenceMapper.selectListByStatus(status);
    }

} 