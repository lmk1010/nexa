package com.kyx.service.biz.service.hotel;

import cn.hutool.core.util.StrUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.security.core.service.SecurityFrameworkService;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.business.api.dept.DeptApi;
import com.kyx.service.business.api.dept.dto.DeptRespDTO;
import com.kyx.service.business.api.user.AdminUserApi;
import com.kyx.service.business.api.user.dto.AdminUserRespDTO;
import com.kyx.service.biz.controller.admin.hotel.vo.HotelDashboardRespVO;
import com.kyx.service.biz.controller.admin.hotel.vo.HotelPermissionRespVO;
import com.kyx.service.biz.controller.admin.hotel.vo.HotelWorkOrderLogRespVO;
import com.kyx.service.biz.controller.admin.hotel.vo.HotelWorkOrderPageReqVO;
import com.kyx.service.biz.controller.admin.hotel.vo.HotelWorkOrderRespVO;
import com.kyx.service.biz.controller.admin.hotel.vo.HotelWorkOrderSaveReqVO;
import com.kyx.service.biz.controller.admin.hotel.vo.HotelWorkOrderStatusReqVO;
import com.kyx.service.biz.controller.admin.todo.vo.TodoSaveReqVO;
import com.kyx.service.biz.dal.dataobject.hotel.HotelWorkOrderDO;
import com.kyx.service.biz.dal.dataobject.hotel.HotelWorkOrderLogDO;
import com.kyx.service.biz.dal.mysql.hotel.HotelWorkOrderLogMapper;
import com.kyx.service.biz.dal.mysql.hotel.HotelWorkOrderMapper;
import com.kyx.service.biz.service.todo.TodoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.biz.enums.ErrorCodeConstants.HOTEL_WORK_ORDER_FORBIDDEN;
@Service
@Validated
public class HotelWorkOrderServiceImpl implements HotelWorkOrderService {
    private static final Integer STATUS_PENDING = 0;
    private static final Integer STATUS_DOING = 1;
    private static final Integer STATUS_DONE = 2;
    private static final String BUSINESS_TYPE = "HOTEL_WORK_ORDER";
    private static final String TASK_TYPE = "ASSIGNEE_CONFIRM";
    private static final String PERMISSION_FRONT_DESK = "hotel:front-desk:use";
    private static final String PERMISSION_DASHBOARD = "hotel:dashboard:query";
    private static final String PERMISSION_STORE_ALL = "hotel:store:all";
    private static final List<String> HOTEL_STORES = Arrays.asList("万达店", "聚云店", "高新店");
    private static final List<String> ADMIN_ROLES = Arrays.asList("super_admin", "tenant_admin", "system_admin", "biz_boss");

    @Resource
    private HotelWorkOrderMapper orderMapper;
    @Resource
    private HotelWorkOrderLogMapper logMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private SecurityFrameworkService securityFrameworkService;
    @Resource
    private TodoService todoService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(HotelWorkOrderSaveReqVO reqVO, Long userId) {
        UserHotelScope scope = resolveUserScope(userId);
        validateFrontDeskPermission(scope);
        AdminUserRespDTO creator = getUser(userId);
        AdminUserRespDTO assignee = getUser(reqVO.getAssigneeUserId());
        String scopedStore = resolveRequestedStore(reqVO.getStore(), scope);
        validateSameStoreAssignee(assignee, scopedStore, scope);
        reqVO.setStore(scopedStore);
        HotelWorkOrderDO order = BeanUtils.toBean(reqVO, HotelWorkOrderDO.class);
        order.setStatus(STATUS_PENDING);
        order.setCreatorUserId(userId);
        order.setCreatorName(displayName(creator, userId, null));
        order.setAssigneeName(displayName(assignee, reqVO.getAssigneeUserId(), reqVO.getAssigneeName()));
        order.setSource(StrUtil.blankToDefault(reqVO.getSource(), "手动创建"));
        order.setCustomerEmotion(StrUtil.blankToDefault(reqVO.getCustomerEmotion(), "平静"));
        orderMapper.insert(order);
        insertLog(order.getId(), null, STATUS_PENDING, userId, order.getCreatorName(), "工单已创建并派给" + order.getAssigneeName());
        upsertTodo(order);
        return order.getId();
    }

    @Override
    public PageResult<HotelWorkOrderRespVO> page(HotelWorkOrderPageReqVO reqVO, Long userId, boolean mine) {
        UserHotelScope scope = resolveUserScope(userId);
        validateFrontDeskPermission(scope);
        applyStoreScope(reqVO, scope);
        if (mine) reqVO.setAssigneeUserId(userId);
        PageResult<HotelWorkOrderDO> page = orderMapper.selectPage(reqVO);
        PageResult<HotelWorkOrderRespVO> result = BeanUtils.toBean(page, HotelWorkOrderRespVO.class);
        if (result.getList() != null) {
            result.getList().forEach(item -> item.setLogs(BeanUtils.toBean(logMapper.selectListByOrderId(item.getId()), HotelWorkOrderLogRespVO.class)));
        }
        return result;
    }

    @Override
    public HotelWorkOrderRespVO get(Long id, Long userId) {
        HotelWorkOrderDO order = orderMapper.selectById(id);
        if (order == null) throw new IllegalArgumentException("酒店工单不存在");
        validateOrderScope(order, resolveUserScope(userId));
        HotelWorkOrderRespVO resp = BeanUtils.toBean(order, HotelWorkOrderRespVO.class);
        resp.setLogs(BeanUtils.toBean(logMapper.selectListByOrderId(id), HotelWorkOrderLogRespVO.class));
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(HotelWorkOrderSaveReqVO reqVO, Long userId) {
        if (reqVO.getId() == null) throw new IllegalArgumentException("酒店工单编号不能为空");
        HotelWorkOrderDO old = orderMapper.selectById(reqVO.getId());
        if (old == null) throw new IllegalArgumentException("酒店工单不存在");
        UserHotelScope scope = resolveUserScope(userId);
        validateManagePermission(scope);
        validateOrderScope(old, scope);
        AdminUserRespDTO operator = getUser(userId);
        String operatorName = displayName(operator, userId, null);
        AdminUserRespDTO assignee = getUser(reqVO.getAssigneeUserId());
        String scopedStore = resolveRequestedStore(reqVO.getStore(), scope);
        validateSameStoreAssignee(assignee, scopedStore, scope);

        HotelWorkOrderDO update = new HotelWorkOrderDO();
        update.setId(old.getId());
        update.setStore(scopedStore);
        update.setRoomNo(StrUtil.trimToEmpty(reqVO.getRoomNo()));
        update.setTitle(StrUtil.trim(reqVO.getTitle()));
        update.setType(StrUtil.trim(reqVO.getType()));
        update.setPriority(StrUtil.trim(reqVO.getPriority()));
        update.setContent(StrUtil.trimToEmpty(reqVO.getContent()));
        update.setSource(StrUtil.blankToDefault(reqVO.getSource(), old.getSource()));
        update.setSourceRecordId(reqVO.getSourceRecordId());
        update.setSourceRecordTitle(reqVO.getSourceRecordTitle());
        update.setCustomerEmotion(StrUtil.blankToDefault(reqVO.getCustomerEmotion(), "平静"));
        update.setAssigneeUserId(reqVO.getAssigneeUserId());
        update.setAssigneeName(displayName(assignee, reqVO.getAssigneeUserId(), reqVO.getAssigneeName()));
        update.setAssigneeImUserId(reqVO.getAssigneeImUserId());
        orderMapper.updateById(update);

        HotelWorkOrderDO latest = orderMapper.selectById(old.getId());
        insertLog(old.getId(), old.getStatus(), old.getStatus(), userId, operatorName, "店长/管理员编辑了工单");
        if (!Objects.equals(old.getAssigneeUserId(), latest.getAssigneeUserId())) {
            todoService.completeGeneratedTodos(BUSINESS_TYPE, old.getId(), TASK_TYPE);
        }
        upsertTodo(latest);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long userId) {
        HotelWorkOrderDO order = orderMapper.selectById(id);
        if (order == null) return;
        UserHotelScope scope = resolveUserScope(userId);
        validateManagePermission(scope);
        validateOrderScope(order, scope);
        AdminUserRespDTO operator = getUser(userId);
        String operatorName = displayName(operator, userId, null);
        insertLog(order.getId(), order.getStatus(), order.getStatus(), userId, operatorName, "店长/管理员删除了工单");
        todoService.completeGeneratedTodos(BUSINESS_TYPE, order.getId(), TASK_TYPE);
        orderMapper.deleteById(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(HotelWorkOrderStatusReqVO reqVO, Long userId) {
        HotelWorkOrderDO order = orderMapper.selectById(reqVO.getId());
        if (order == null) throw new IllegalArgumentException("酒店工单不存在");
        validateOrderScope(order, resolveUserScope(userId));
        Integer oldStatus = order.getStatus();
        Integer nextStatus = reqVO.getStatus();
        if (!Objects.equals(nextStatus, STATUS_PENDING)
                && !Objects.equals(nextStatus, STATUS_DOING)
                && !Objects.equals(nextStatus, STATUS_DONE)) {
            throw new IllegalArgumentException("酒店工单状态不正确");
        }
        HotelWorkOrderDO update = new HotelWorkOrderDO();
        update.setId(order.getId());
        update.setStatus(nextStatus);
        Date now = new Date();
        AdminUserRespDTO operator = getUser(userId);
        String operatorName = displayName(operator, userId, null);
        if (Objects.equals(nextStatus, STATUS_DOING) && order.getAcceptedTime() == null) {
            update.setAcceptedTime(now);
            update.setAcceptedUserId(userId);
            update.setAcceptedUserName(operatorName);
        }
        if (Objects.equals(nextStatus, STATUS_DONE)) {
            if (order.getAcceptedTime() == null) {
                update.setAcceptedTime(now);
                update.setAcceptedUserId(userId);
                update.setAcceptedUserName(operatorName);
            }
            if (order.getFinishTime() == null) {
                update.setFinishTime(now);
                update.setFinishUserId(userId);
                update.setFinishUserName(operatorName);
            }
        }
        orderMapper.updateById(update);
        insertLog(order.getId(), oldStatus, nextStatus, userId, operatorName, StrUtil.blankToDefault(reqVO.getRemark(), statusText(nextStatus)));
        if (Objects.equals(nextStatus, STATUS_DONE)) {
            todoService.completeGeneratedTodos(BUSINESS_TYPE, order.getId(), TASK_TYPE);
        } else {
            order.setStatus(nextStatus);
            upsertTodo(order);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitAcceptance(HotelWorkOrderStatusReqVO reqVO, Long userId) {
        // 第一版轻闭环：员工/大姐提交处理结果即完成，不强制额外验收。
        HotelWorkOrderStatusReqVO statusReq = new HotelWorkOrderStatusReqVO();
        statusReq.setId(reqVO.getId());
        statusReq.setStatus(STATUS_DONE);
        statusReq.setRemark(StrUtil.blankToDefault(reqVO.getRemark(), "已处理完成"));
        updateStatus(statusReq, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptPass(HotelWorkOrderStatusReqVO reqVO, Long userId) {
        // 兼容预留接口：店长/前台确认时也直接完成。
        UserHotelScope scope = resolveUserScope(userId);
        validateManagePermission(scope);
        HotelWorkOrderDO order = requireOrder(reqVO.getId());
        validateOrderScope(order, scope);
        HotelWorkOrderStatusReqVO statusReq = new HotelWorkOrderStatusReqVO();
        statusReq.setId(reqVO.getId());
        statusReq.setStatus(STATUS_DONE);
        statusReq.setRemark(StrUtil.blankToDefault(reqVO.getRemark(), "确认完成"));
        updateStatus(statusReq, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptReject(HotelWorkOrderStatusReqVO reqVO, Long userId) {
        // 兼容预留接口：驳回后回到处理中，待处理人重新完成。
        UserHotelScope scope = resolveUserScope(userId);
        validateManagePermission(scope);
        HotelWorkOrderDO order = requireOrder(reqVO.getId());
        validateOrderScope(order, scope);
        HotelWorkOrderStatusReqVO statusReq = new HotelWorkOrderStatusReqVO();
        statusReq.setId(reqVO.getId());
        statusReq.setStatus(STATUS_DOING);
        statusReq.setRemark(StrUtil.blankToDefault(reqVO.getRemark(), "驳回重做"));
        updateStatus(statusReq, userId);
    }

    @Override
    public HotelDashboardRespVO dashboard(String store, Long userId) {
        UserHotelScope scope = resolveUserScope(userId);
        if (!scope.canViewDashboard) throw exception(HOTEL_WORK_ORDER_FORBIDDEN);
        HotelWorkOrderPageReqVO req = new HotelWorkOrderPageReqVO();
        req.setPageNo(1);
        req.setPageSize(500);
        req.setStore(store);
        applyStoreScope(req, scope);
        List<HotelWorkOrderRespVO> orders = page(req, userId, false).getList();
        HotelDashboardRespVO vo = new HotelDashboardRespVO();
        vo.setOrders(orders);
        vo.setMyTodoCount(badge(userId));
        vo.setTotal(orders.size());
        vo.setPending((int) orders.stream().filter(o -> Objects.equals(o.getStatus(), STATUS_PENDING)).count());
        vo.setDoing((int) orders.stream().filter(o -> Objects.equals(o.getStatus(), STATUS_DOING)).count());
        vo.setDone((int) orders.stream().filter(o -> Objects.equals(o.getStatus(), STATUS_DONE)).count());
        vo.setStoreCounts(countBy(orders, HotelWorkOrderRespVO::getStore));
        vo.setTypeCounts(countBy(orders, HotelWorkOrderRespVO::getType));
        vo.setStatusCounts(countBy(orders, o -> statusText(o.getStatus())));
        vo.setEmotionCounts(countBy(orders, HotelWorkOrderRespVO::getCustomerEmotion));
        return vo;
    }

    @Override
    public Integer badge(Long userId) {
        Long count = orderMapper.countOpenByAssignee(userId);
        return count == null ? 0 : count.intValue();
    }

    @Override
    public HotelPermissionRespVO permission(Long userId) {
        UserHotelScope scope = resolveUserScope(userId);
        HotelPermissionRespVO vo = new HotelPermissionRespVO();
        vo.setCanUseFrontDesk(scope.canUseFrontDesk);
        vo.setCanViewDashboard(scope.canViewDashboard);
        vo.setCanManageWorkOrder(scope.canManageWorkOrder);
        vo.setCanDeleteWorkOrder(scope.canDeleteWorkOrder);
        vo.setCanViewAllStores(scope.canViewAllStores);
        vo.setScopedStore(scope.store);
        vo.setDeptId(scope.deptId);
        vo.setDeptName(scope.deptName);
        vo.setStores(scope.canViewAllStores ? HOTEL_STORES : (StrUtil.isBlank(scope.store) ? java.util.Collections.emptyList() : java.util.Collections.singletonList(scope.store)));
        return vo;
    }

    private void upsertTodo(HotelWorkOrderDO order) {
        if (order.getAssigneeUserId() == null || Objects.equals(order.getStatus(), STATUS_DONE)) return;
        TodoSaveReqVO todo = new TodoSaveReqVO();
        todo.setTitle("酒店工单：" + StrUtil.blankToDefault(order.getTitle(), "待处理"));
        todo.setDescription(buildTodoDescription(order));
        todo.setPriority("紧急".equals(order.getPriority()) ? 3 : 2);
        todoService.upsertGeneratedTodo(todo, order.getAssigneeUserId(), BUSINESS_TYPE, order.getId(), TASK_TYPE, "/hotel/work-order/" + order.getId());
    }

    private String buildTodoDescription(HotelWorkOrderDO order) {
        return String.format("%s %s %s\n%s", StrUtil.blankToDefault(order.getStore(), ""), StrUtil.blankToDefault(order.getRoomNo(), ""), StrUtil.blankToDefault(order.getType(), ""), StrUtil.blankToDefault(order.getContent(), ""));
    }

    private void insertLog(Long orderId, Integer from, Integer to, Long userId, String name, String content) {
        HotelWorkOrderLogDO log = new HotelWorkOrderLogDO();
        log.setOrderId(orderId);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setOperatorUserId(userId);
        log.setOperatorName(name);
        log.setContent(content);
        logMapper.insert(log);
    }


    private HotelWorkOrderDO requireOrder(Long id) {
        HotelWorkOrderDO order = orderMapper.selectById(id);
        if (order == null) throw new IllegalArgumentException("酒店工单不存在");
        return order;
    }

    private void validateFrontDeskPermission(UserHotelScope scope) {
        if (!scope.canUseFrontDesk) throw exception(HOTEL_WORK_ORDER_FORBIDDEN);
        if (!scope.canViewAllStores && StrUtil.isBlank(scope.store)) throw exception(HOTEL_WORK_ORDER_FORBIDDEN);
    }

    private void validateManagePermission(UserHotelScope scope) {
        validateFrontDeskPermission(scope);
        if (!scope.canManageWorkOrder) throw exception(HOTEL_WORK_ORDER_FORBIDDEN);
    }

    private void applyStoreScope(HotelWorkOrderPageReqVO reqVO, UserHotelScope scope) {
        validateFrontDeskPermission(scope);
        reqVO.setStore(resolveRequestedStore(reqVO.getStore(), scope));
    }

    private String resolveRequestedStore(String requestedStore, UserHotelScope scope) {
        String requested = StrUtil.trimToNull(requestedStore);
        if (scope.canViewAllStores) return requested;
        if (StrUtil.isBlank(scope.store)) throw exception(HOTEL_WORK_ORDER_FORBIDDEN);
        if (requested != null && !Objects.equals(requested, scope.store)) throw exception(HOTEL_WORK_ORDER_FORBIDDEN);
        return scope.store;
    }

    private void validateOrderScope(HotelWorkOrderDO order, UserHotelScope scope) {
        validateFrontDeskPermission(scope);
        if (scope.canViewAllStores) return;
        if (!Objects.equals(order.getStore(), scope.store)) throw exception(HOTEL_WORK_ORDER_FORBIDDEN);
    }

    private void validateSameStoreAssignee(AdminUserRespDTO assignee, String store, UserHotelScope scope) {
        if (assignee == null || assignee.getId() == null) throw exception(HOTEL_WORK_ORDER_FORBIDDEN);
        if (scope.canViewAllStores) return;
        String assigneeStore = resolveStoreByDeptId(assignee.getDeptId());
        if (!Objects.equals(store, assigneeStore)) throw exception(HOTEL_WORK_ORDER_FORBIDDEN);
    }

    private UserHotelScope resolveUserScope(Long userId) {
        AdminUserRespDTO user = getUser(userId);
        UserHotelScope scope = new UserHotelScope();
        scope.deptId = user == null ? null : user.getDeptId();
        scope.deptName = resolveDeptName(scope.deptId);
        scope.store = resolveStoreByDeptId(scope.deptId);
        scope.canViewAllStores = securityFrameworkService.hasPermission(PERMISSION_STORE_ALL)
                || securityFrameworkService.hasAnyRoles(ADMIN_ROLES.toArray(new String[0]));
        scope.canViewDashboard = securityFrameworkService.hasPermission(PERMISSION_DASHBOARD)
                || scope.canViewAllStores;
        scope.canManageWorkOrder = securityFrameworkService.hasAnyPermissions("hotel:work-order:update", "hotel:work-order:delete")
                || scope.canViewDashboard;
        scope.canDeleteWorkOrder = securityFrameworkService.hasPermission("hotel:work-order:delete")
                || scope.canViewDashboard;
        scope.canUseFrontDesk = securityFrameworkService.hasAnyPermissions(PERMISSION_FRONT_DESK,
                "hotel:work-order:query", "hotel:work-order:create", "hotel:work-order:update")
                || scope.canViewDashboard
                || StrUtil.isNotBlank(scope.store);
        return scope;
    }

    private String resolveStoreByDeptId(Long deptId) {
        DeptRespDTO dept = getDept(deptId);
        int guard = 0;
        while (dept != null && guard++ < 8) {
            if (HOTEL_STORES.contains(dept.getName())) return dept.getName();
            dept = getDept(dept.getParentId());
        }
        return null;
    }

    private String resolveDeptName(Long deptId) {
        DeptRespDTO dept = getDept(deptId);
        return dept == null ? null : dept.getName();
    }

    private DeptRespDTO getDept(Long id) {
        if (id == null || id <= 0) return null;
        try { return deptApi.getDept(id).getCheckedData(); } catch (Exception ex) { return null; }
    }

    private AdminUserRespDTO getUser(Long id) {
        if (id == null) return null;
        try { return adminUserApi.getUser(id).getCheckedData(); } catch (Exception ex) { return null; }
    }

    private String displayName(AdminUserRespDTO user, Long id, String fallback) {
        if (StrUtil.isNotBlank(fallback)) return fallback;
        if (user == null) return id == null ? "-" : String.valueOf(id);
        return StrUtil.blankToDefault(user.getNickname(), StrUtil.blankToDefault(user.getUsername(), String.valueOf(id)));
    }

    private static String statusText(Integer status) {
        if (Objects.equals(status, STATUS_PENDING)) return "待确认";
        if (Objects.equals(status, STATUS_DOING)) return "已收到";
        if (Objects.equals(status, STATUS_DONE)) return "已完成";
        return "未知";
    }

    private static Map<String, Integer> countBy(List<HotelWorkOrderRespVO> orders, java.util.function.Function<HotelWorkOrderRespVO, String> getter) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (HotelWorkOrderRespVO order : orders) {
            String key = StrUtil.blankToDefault(getter.apply(order), "其他");
            map.put(key, map.getOrDefault(key, 0) + 1);
        }
        return map;
    }

    private static class UserHotelScope {
        private boolean canUseFrontDesk;
        private boolean canViewDashboard;
        private boolean canManageWorkOrder;
        private boolean canDeleteWorkOrder;
        private boolean canViewAllStores;
        private String store;
        private Long deptId;
        private String deptName;
    }
}
