package com.kyx.service.hr.service.todo;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.todo.vo.HrTodoCompleteReqVO;
import com.kyx.service.hr.controller.admin.todo.vo.HrTodoPageReqVO;
import com.kyx.service.hr.controller.admin.todo.vo.HrTodoRespVO;
import com.kyx.service.hr.controller.admin.todo.vo.HrTodoSummaryReqVO;
import com.kyx.service.hr.controller.admin.todo.vo.HrTodoSummaryRespVO;

public interface HrTodoTaskService {

    PageResult<HrTodoRespVO> getPage(HrTodoPageReqVO pageReqVO);

    HrTodoSummaryRespVO getSummary(Boolean mine);

    HrTodoSummaryRespVO getSummary(HrTodoSummaryReqVO reqVO);

    Boolean complete(HrTodoCompleteReqVO reqVO);

    Integer refreshGeneratedTasks();

}
