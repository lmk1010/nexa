package com.kyx.service.biz.controller.admin.todo;

import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.biz.controller.admin.todo.vo.TodoPageReqVO;
import com.kyx.service.biz.controller.admin.todo.vo.TodoRespVO;
import com.kyx.service.biz.controller.admin.todo.vo.TodoSaveReqVO;
import com.kyx.service.biz.controller.admin.todo.vo.TodoUpdateStatusReqVO;
import com.kyx.service.biz.dal.dataobject.todo.TodoDO;
import com.kyx.service.biz.service.todo.TodoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.kyx.foundation.common.pojo.CommonResult.success;
import static com.kyx.foundation.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "Admin - Todo")
@RestController
@RequestMapping("/business/todo")
@Validated
public class TodoController {

    @Resource
    private TodoService todoService;

    @PostMapping("/create")
    @Operation(summary = "Create todo")
    @PreAuthorize("@ss.hasPermission('business:todo:create')")
    public CommonResult<Long> createTodo(@Valid @RequestBody TodoSaveReqVO createReqVO) {
        return success(todoService.createTodo(createReqVO, getLoginUserId()));
    }

    @PostMapping("/broadcast")
    @Operation(summary = "Broadcast todo to all enabled users")
    @PreAuthorize("@ss.hasAnyPermissions('business:todo:broadcast,system:notice:create,system:user:query')")
    public CommonResult<Integer> broadcastTodo(@Valid @RequestBody TodoSaveReqVO createReqVO) {
        return success(todoService.broadcastTodo(createReqVO, getLoginUserId()));
    }

    @PutMapping("/update")
    @Operation(summary = "Update todo")
    @PreAuthorize("@ss.hasPermission('business:todo:update')")
    public CommonResult<Boolean> updateTodo(@Valid @RequestBody TodoSaveReqVO updateReqVO) {
        todoService.updateTodo(updateReqVO, getLoginUserId());
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "Update todo status")
    @PreAuthorize("@ss.hasPermission('business:todo:update')")
    public CommonResult<Boolean> updateTodoStatus(@Valid @RequestBody TodoUpdateStatusReqVO updateReqVO) {
        todoService.updateTodoStatus(updateReqVO, getLoginUserId());
        return success(true);
    }

    @PutMapping("/complete-all")
    @Operation(summary = "Complete all todos")
    @PreAuthorize("@ss.hasPermission('business:todo:update')")
    public CommonResult<Boolean> completeAll() {
        todoService.completeAll(getLoginUserId());
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Delete todo")
    @Parameter(name = "id", description = "ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('business:todo:delete')")
    public CommonResult<Boolean> deleteTodo(@RequestParam("id") Long id) {
        todoService.deleteTodo(id, getLoginUserId());
        return success(true);
    }

    @DeleteMapping("/clear-completed")
    @Operation(summary = "Clear completed todos")
    @PreAuthorize("@ss.hasPermission('business:todo:delete')")
    public CommonResult<Boolean> clearCompleted() {
        todoService.clearCompleted(getLoginUserId());
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "Get todo")
    @Parameter(name = "id", description = "ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('business:todo:query')")
    public CommonResult<TodoRespVO> getTodo(@RequestParam("id") Long id) {
        TodoDO todo = todoService.getTodo(id, getLoginUserId());
        return success(convertTodo(todo));
    }

    @GetMapping("/page")
    @Operation(summary = "Get todo page")
    @PreAuthorize("@ss.hasPermission('business:todo:query')")
    public CommonResult<PageResult<TodoRespVO>> getTodoPage(@Valid TodoPageReqVO pageReqVO) {
        PageResult<TodoDO> pageResult = todoService.getTodoPage(pageReqVO, getLoginUserId());
        List<TodoRespVO> list = pageResult.getList().stream()
                .map(this::convertTodo)
                .collect(Collectors.toList());
        return success(new PageResult<>(list, pageResult.getTotal()));
    }

    private TodoRespVO convertTodo(TodoDO todo) {
        TodoRespVO respVO = BeanUtils.toBean(todo, TodoRespVO.class);
        respVO.setTags(splitTags(todo.getTags()));
        return respVO;
    }

    private static List<String> splitTags(String tags) {
        if (StrUtil.isBlank(tags)) {
            return Collections.emptyList();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }

}
