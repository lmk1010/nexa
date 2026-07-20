package com.kyx.service.biz.service.todo;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.kyx.service.biz.controller.admin.todo.vo.TodoPageReqVO;
import com.kyx.service.biz.controller.admin.todo.vo.TodoSaveReqVO;
import com.kyx.service.biz.controller.admin.todo.vo.TodoUpdateStatusReqVO;
import com.kyx.service.biz.dal.dataobject.todo.TodoDO;
import com.kyx.service.biz.dal.mysql.todo.TodoMapper;
import com.kyx.service.biz.enums.TodoStatusEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.biz.enums.ErrorCodeConstants.TODO_FORBIDDEN;
import static com.kyx.service.biz.enums.ErrorCodeConstants.TODO_NOT_EXISTS;

@Service
@Validated
public class TodoServiceImpl implements TodoService {

    @Resource
    private TodoMapper todoMapper;
    @Resource
    private AdminUserApi adminUserApi;

    @Override
    public Long createTodo(TodoSaveReqVO createReqVO, Long userId) {
        return createTodoForUser(createReqVO, userId, false);
    }

    @Override
    public Long upsertGeneratedTodo(TodoSaveReqVO createReqVO, Long userId,
                                    String businessType, Long businessId, String taskType, String routePath) {
        TodoDO existing = todoMapper.selectOne(new LambdaQueryWrapper<TodoDO>()
                .eq(TodoDO::getGeneratedFlag, true)
                .eq(TodoDO::getUserId, userId)
                .eq(TodoDO::getBusinessType, businessType)
                .eq(TodoDO::getBusinessId, businessId)
                .eq(TodoDO::getTaskType, taskType)
                .orderByDesc(TodoDO::getId)
                .last("LIMIT 1"));
        if (existing == null) {
            TodoDO todo = buildTodoDO(createReqVO, userId, false);
            todo.setGeneratedFlag(true);
            todo.setBusinessType(businessType);
            todo.setBusinessId(businessId);
            todo.setTaskType(taskType);
            todo.setRoutePath(routePath);
            todoMapper.insert(todo);
            return todo.getId();
        }
        TodoDO updateObj = buildTodoDO(createReqVO, userId, false);
        updateObj.setId(existing.getId());
        updateObj.setGeneratedFlag(true);
        updateObj.setBusinessType(businessType);
        updateObj.setBusinessId(businessId);
        updateObj.setTaskType(taskType);
        updateObj.setRoutePath(routePath);
        updateObj.setStatus(TodoStatusEnum.PROCESS.getStatus());
        todoMapper.updateById(updateObj);
        return existing.getId();
    }

    @Override
    public int completeGeneratedTodos(String businessType, Long businessId, String taskType) {
        if (StrUtil.isBlank(businessType) || businessId == null) {
            return 0;
        }
        TodoDO updateObj = new TodoDO();
        updateObj.setStatus(TodoStatusEnum.DONE.getStatus());
        LambdaUpdateWrapper<TodoDO> wrapper = new LambdaUpdateWrapper<TodoDO>()
                .eq(TodoDO::getGeneratedFlag, true)
                .eq(TodoDO::getBusinessType, businessType)
                .eq(TodoDO::getBusinessId, businessId)
                .eq(TodoDO::getStatus, TodoStatusEnum.PROCESS.getStatus());
        if (StrUtil.isNotBlank(taskType)) {
            wrapper.eq(TodoDO::getTaskType, taskType);
        }
        return todoMapper.update(updateObj, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int broadcastTodo(TodoSaveReqVO createReqVO, Long senderUserId) {
        List<AdminUserRespDTO> users = adminUserApi
                .getUserListByStatus(CommonStatusEnum.ENABLE.getStatus())
                .getCheckedData();
        if (CollUtil.isEmpty(users)) {
            return 0;
        }
        int count = 0;
        for (AdminUserRespDTO user : users) {
            if (user == null || user.getId() == null) {
                continue;
            }
            createTodoForUser(createReqVO, user.getId(), true);
            count++;
        }
        return count;
    }

    private Long createTodoForUser(TodoSaveReqVO createReqVO, Long userId, boolean broadcast) {
        TodoDO todo = buildTodoDO(createReqVO, userId, broadcast);
        todoMapper.insert(todo);
        return todo.getId();
    }

    private TodoDO buildTodoDO(TodoSaveReqVO createReqVO, Long userId, boolean broadcast) {
        TodoDO todo = new TodoDO();
        todo.setTitle(createReqVO.getTitle());
        todo.setDescription(createReqVO.getDescription());
        todo.setDueDate(createReqVO.getDueDate());
        todo.setPriority(createReqVO.getPriority());
        todo.setTags(joinTags(createReqVO.getTags(), broadcast));
        todo.setStatus(TodoStatusEnum.PROCESS.getStatus());
        todo.setUserId(userId);
        return todo;
    }

    @Override
    public void updateTodo(TodoSaveReqVO updateReqVO, Long userId) {
        validateTodo(updateReqVO.getId(), userId);
        TodoDO updateObj = new TodoDO();
        updateObj.setId(updateReqVO.getId());
        updateObj.setTitle(updateReqVO.getTitle());
        updateObj.setDescription(updateReqVO.getDescription());
        updateObj.setDueDate(updateReqVO.getDueDate());
        updateObj.setPriority(updateReqVO.getPriority());
        updateObj.setTags(joinTags(updateReqVO.getTags()));
        todoMapper.updateById(updateObj);
    }

    @Override
    public void updateTodoStatus(TodoUpdateStatusReqVO reqVO, Long userId) {
        validateTodo(reqVO.getId(), userId);
        TodoDO updateObj = new TodoDO();
        updateObj.setId(reqVO.getId());
        updateObj.setStatus(reqVO.getStatus());
        todoMapper.updateById(updateObj);
    }

    @Override
    public void deleteTodo(Long id, Long userId) {
        validateTodo(id, userId);
        todoMapper.deleteById(id);
    }

    @Override
    public TodoDO getTodo(Long id, Long userId) {
        return validateTodo(id, userId);
    }

    @Override
    public PageResult<TodoDO> getTodoPage(TodoPageReqVO pageReqVO, Long userId) {
        return todoMapper.selectPage(pageReqVO, userId);
    }

    @Override
    public void completeAll(Long userId) {
        TodoDO updateObj = new TodoDO();
        updateObj.setStatus(TodoStatusEnum.DONE.getStatus());
        todoMapper.update(updateObj, new LambdaUpdateWrapper<TodoDO>()
                .eq(TodoDO::getUserId, userId)
                .eq(TodoDO::getGeneratedFlag, false)
                .eq(TodoDO::getStatus, TodoStatusEnum.PROCESS.getStatus()));
    }

    @Override
    public void clearCompleted(Long userId) {
        todoMapper.delete(new LambdaQueryWrapper<TodoDO>()
                .eq(TodoDO::getUserId, userId)
                .eq(TodoDO::getGeneratedFlag, false)
                .eq(TodoDO::getStatus, TodoStatusEnum.DONE.getStatus()));
    }

    private TodoDO validateTodo(Long id, Long userId) {
        TodoDO todo = todoMapper.selectById(id);
        if (todo == null) {
            throw exception(TODO_NOT_EXISTS);
        }
        if (!Objects.equals(todo.getUserId(), userId)) {
            throw exception(TODO_FORBIDDEN);
        }
        return todo;
    }

    private static String joinTags(Collection<String> tags) {
        return joinTags(tags, false);
    }

    private static String joinTags(Collection<String> tags, boolean broadcast) {
        if (CollUtil.isEmpty(tags) && !broadcast) {
            return null;
        }
        List<String> mergedTags = new ArrayList<>();
        if (CollUtil.isNotEmpty(tags)) {
            mergedTags.addAll(tags);
        }
        if (broadcast) {
            mergedTags.add("全员待办");
        }
        return mergedTags.stream()
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.joining(","));
    }

}
