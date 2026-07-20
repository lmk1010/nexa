package com.kyx.service.biz.service.todo;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.biz.controller.admin.todo.vo.TodoPageReqVO;
import com.kyx.service.biz.controller.admin.todo.vo.TodoSaveReqVO;
import com.kyx.service.biz.controller.admin.todo.vo.TodoUpdateStatusReqVO;
import com.kyx.service.biz.dal.dataobject.todo.TodoDO;

import javax.validation.Valid;

public interface TodoService {

    Long createTodo(@Valid TodoSaveReqVO createReqVO, Long userId);

    Long upsertGeneratedTodo(@Valid TodoSaveReqVO createReqVO, Long userId,
                             String businessType, Long businessId, String taskType, String routePath);

    int completeGeneratedTodos(String businessType, Long businessId, String taskType);

    int broadcastTodo(@Valid TodoSaveReqVO createReqVO, Long senderUserId);

    void updateTodo(@Valid TodoSaveReqVO updateReqVO, Long userId);

    void updateTodoStatus(@Valid TodoUpdateStatusReqVO reqVO, Long userId);

    void deleteTodo(Long id, Long userId);

    TodoDO getTodo(Long id, Long userId);

    PageResult<TodoDO> getTodoPage(TodoPageReqVO pageReqVO, Long userId);

    void completeAll(Long userId);

    void clearCompleted(Long userId);

}
