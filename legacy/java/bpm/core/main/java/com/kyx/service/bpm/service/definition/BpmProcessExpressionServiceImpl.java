package com.kyx.service.bpm.service.definition;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.bpm.controller.admin.definition.vo.expression.BpmProcessExpressionPageReqVO;
import com.kyx.service.bpm.controller.admin.definition.vo.expression.BpmProcessExpressionSaveReqVO;
import com.kyx.service.bpm.dal.dataobject.definition.BpmProcessExpressionDO;
import com.kyx.service.bpm.dal.mysql.definition.BpmProcessExpressionMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.bpm.enums.ErrorCodeConstants.PROCESS_EXPRESSION_NOT_EXISTS;

/**
 * BPM 流程表达式 Service 实现类
 *
 * @author MK
 */
@Service
@Validated
public class BpmProcessExpressionServiceImpl implements BpmProcessExpressionService {

    @Resource
    private BpmProcessExpressionMapper processExpressionMapper;

    @Override
    public Long createProcessExpression(BpmProcessExpressionSaveReqVO createReqVO) {
        // 插入
        BpmProcessExpressionDO processExpression = BeanUtils.toBean(createReqVO, BpmProcessExpressionDO.class);
        processExpressionMapper.insert(processExpression);
        // 返回
        return processExpression.getId();
    }

    @Override
    public void updateProcessExpression(BpmProcessExpressionSaveReqVO updateReqVO) {
        // 校验存在
        validateProcessExpressionExists(updateReqVO.getId());
        // 更新
        BpmProcessExpressionDO updateObj = BeanUtils.toBean(updateReqVO, BpmProcessExpressionDO.class);
        processExpressionMapper.updateById(updateObj);
    }

    @Override
    public void deleteProcessExpression(Long id) {
        // 校验存在
        validateProcessExpressionExists(id);
        // 删除
        processExpressionMapper.deleteById(id);
    }

    private void validateProcessExpressionExists(Long id) {
        if (processExpressionMapper.selectById(id) == null) {
            throw exception(PROCESS_EXPRESSION_NOT_EXISTS);
        }
    }

    @Override
    public BpmProcessExpressionDO getProcessExpression(Long id) {
        return processExpressionMapper.selectById(id);
    }

    @Override
    public PageResult<BpmProcessExpressionDO> getProcessExpressionPage(BpmProcessExpressionPageReqVO pageReqVO) {
        return processExpressionMapper.selectPage(pageReqVO);
    }

}