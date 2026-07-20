package com.kyx.service.bpm.service.definition;

import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.bpm.controller.admin.definition.vo.listener.BpmProcessListenerPageReqVO;
import com.kyx.service.bpm.controller.admin.definition.vo.listener.BpmProcessListenerSaveReqVO;
import com.kyx.service.bpm.dal.dataobject.definition.BpmProcessListenerDO;
import com.kyx.service.bpm.dal.mysql.definition.BpmProcessListenerMapper;
import com.kyx.service.bpm.enums.definition.BpmProcessListenerTypeEnum;
import com.kyx.service.bpm.enums.definition.BpmProcessListenerValueTypeEnum;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.delegate.TaskListener;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.bpm.enums.ErrorCodeConstants.*;

/**
 * BPM 流程监听器 Service 实现类
 *
 * @author MK
 */
@Service
@Validated
public class BpmProcessListenerServiceImpl implements BpmProcessListenerService {

    @Resource
    private BpmProcessListenerMapper processListenerMapper;

    @Override
    public Long createProcessListener(BpmProcessListenerSaveReqVO createReqVO) {
        // 校验
        validateCreateProcessListenerValue(createReqVO);
        // 插入
        BpmProcessListenerDO processListener = BeanUtils.toBean(createReqVO, BpmProcessListenerDO.class);
        processListenerMapper.insert(processListener);
        return processListener.getId();
    }

    @Override
    public void updateProcessListener(BpmProcessListenerSaveReqVO updateReqVO) {
        // 校验存在
        validateProcessListenerExists(updateReqVO.getId());
        validateCreateProcessListenerValue(updateReqVO);
        // 更新
        BpmProcessListenerDO updateObj = BeanUtils.toBean(updateReqVO, BpmProcessListenerDO.class);
        processListenerMapper.updateById(updateObj);
    }

    private void validateCreateProcessListenerValue(BpmProcessListenerSaveReqVO createReqVO) {
        // class 类型
        if (createReqVO.getValueType().equals(BpmProcessListenerValueTypeEnum.CLASS.getType())) {
            try {
                Class<?> clazz = Class.forName(createReqVO.getValue());
                if (createReqVO.getType().equals(BpmProcessListenerTypeEnum.EXECUTION.getType())
                        && !JavaDelegate.class.isAssignableFrom(clazz)) {
                    throw exception(PROCESS_LISTENER_CLASS_IMPLEMENTS_ERROR, createReqVO.getValue(),
                            JavaDelegate.class.getName());
                } else if (createReqVO.getType().equals(BpmProcessListenerTypeEnum.TASK.getType())
                        && !TaskListener.class.isAssignableFrom(clazz)) {
                    throw exception(PROCESS_LISTENER_CLASS_IMPLEMENTS_ERROR, createReqVO.getValue(),
                            TaskListener.class.getName());
                }
            } catch (ClassNotFoundException e) {
                throw exception(PROCESS_LISTENER_CLASS_NOT_FOUND, createReqVO.getValue());
            }
            return;
        }
        // 表达式
        if (!StrUtil.startWith(createReqVO.getValue(), "${") || !StrUtil.endWith(createReqVO.getValue(), "}")) {
            throw exception(PROCESS_LISTENER_EXPRESSION_INVALID, createReqVO.getValue());
        }
    }

    @Override
    public void deleteProcessListener(Long id) {
        // 校验存在
        validateProcessListenerExists(id);
        // 删除
        processListenerMapper.deleteById(id);
    }

    private void validateProcessListenerExists(Long id) {
        if (processListenerMapper.selectById(id) == null) {
            throw exception(PROCESS_LISTENER_NOT_EXISTS);
        }
    }

    @Override
    public BpmProcessListenerDO getProcessListener(Long id) {
        return processListenerMapper.selectById(id);
    }

    @Override
    public PageResult<BpmProcessListenerDO> getProcessListenerPage(BpmProcessListenerPageReqVO pageReqVO) {
        return processListenerMapper.selectPage(pageReqVO);
    }

}