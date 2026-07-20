package com.kyx.service.erp.service.purchase;

import cn.hutool.core.collection.CollUtil;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.bpm.api.task.BpmProcessInstanceApi;
import com.kyx.service.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.kyx.service.bpm.enums.task.BpmTaskStatusEnum;
import com.kyx.service.erp.controller.admin.purchase.vo.request.ErpPurchaseRequestPageReqVO;
import com.kyx.service.erp.controller.admin.purchase.vo.request.ErpPurchaseRequestSaveReqVO;
import com.kyx.service.erp.dal.dataobject.purchase.ErpPurchaseRequestDO;
import com.kyx.service.erp.dal.dataobject.purchase.ErpPurchaseRequestItemDO;
import com.kyx.service.erp.dal.mysql.purchase.ErpPurchaseRequestItemMapper;
import com.kyx.service.erp.dal.mysql.purchase.ErpPurchaseRequestMapper;
import com.kyx.service.erp.dal.redis.no.ErpNoRedisDAO;
import com.kyx.service.erp.enums.ErrorCodeConstants;
import com.kyx.service.erp.enums.purchase.ErpPurchaseRequestBmpStatusEnum;
import com.kyx.service.erp.enums.purchase.ErpPurchaseRequestStatusEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;

/**
 * ERP 采购申请 Service 实现类
 *
 * @author MK
 */
@Service
@Validated
public class ErpPurchaseRequestServiceImpl implements ErpPurchaseRequestService {

    /**
     * 采购申请对应的流程定义 KEY
     */
    public static final String PROCESS_KEY = "purchase-request";

    @Resource
    private ErpPurchaseRequestMapper purchaseRequestMapper;
    @Resource
    private ErpPurchaseRequestItemMapper purchaseRequestItemMapper;
    @Resource
    private ErpNoRedisDAO noRedisDAO;

    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPurchaseRequest(ErpPurchaseRequestSaveReqVO createReqVO) {
        // 0. 验证日期字段的有效性
        validateRequestDates(createReqVO);
        
        // 1. 生成申请单号，并校验唯一性
        String requestNo = noRedisDAO.generate(ErpNoRedisDAO.PURCHASE_REQUEST_NO_PREFIX);
        while (purchaseRequestMapper.selectByRequestNo(requestNo) != null) {
            requestNo = noRedisDAO.generate(ErpNoRedisDAO.PURCHASE_REQUEST_NO_PREFIX);
        }

        // 2.1 插入采购申请
        ErpPurchaseRequestDO purchaseRequest = BeanUtils.toBean(createReqVO, ErpPurchaseRequestDO.class);
        purchaseRequest.setRequestNo(requestNo);
        purchaseRequestMapper.insert(purchaseRequest);

        // 2.2 插入明细
        createPurchaseRequestItemList(purchaseRequest.getId(), createReqVO.getItems());

        // 2.3 计算总金额
        updatePurchaseRequestTotalAmount(purchaseRequest.getId());
        return purchaseRequest.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPurchaseRequestAndSubmit(Long userId, ErpPurchaseRequestSaveReqVO createReqVO) {
        
        // 1. 生成申请单号，并校验唯一性
        String requestNo = noRedisDAO.generate(ErpNoRedisDAO.PURCHASE_REQUEST_NO_PREFIX);
        while (purchaseRequestMapper.selectByRequestNo(requestNo) != null) {
            requestNo = noRedisDAO.generate(ErpNoRedisDAO.PURCHASE_REQUEST_NO_PREFIX);
        }

        // 2.1 插入采购申请 - 设置为流程中状态
        ErpPurchaseRequestDO purchaseRequest = BeanUtils.toBean(createReqVO, ErpPurchaseRequestDO.class);
        purchaseRequest.setRequestNo(requestNo);
        purchaseRequest.setStatus(ErpPurchaseRequestStatusEnum.PENDING.getStatus());
        purchaseRequest.setBmpStatus(ErpPurchaseRequestBmpStatusEnum.IN_PROGRESS.getStatus());
        purchaseRequestMapper.insert(purchaseRequest);

        // 2.2 插入明细
        createPurchaseRequestItemList(purchaseRequest.getId(), createReqVO.getItems());

        // 2.3 计算总金额
        updatePurchaseRequestTotalAmount(purchaseRequest.getId());

        // 2.4 发起 BPM 流程
        Map<String, Object> processInstanceVariables = new HashMap<>();
        processInstanceVariables.put("requestId", purchaseRequest.getId());
        processInstanceVariables.put("applicant", purchaseRequest.getApplicant());
        processInstanceVariables.put("department", purchaseRequest.getDepartment());
        processInstanceVariables.put("totalAmount", purchaseRequest.getTotalAmount());
        processInstanceVariables.put("urgentLevel", purchaseRequest.getUrgentLevel());
        processInstanceVariables.put("title", purchaseRequest.getTitle());
        processInstanceVariables.put("reason", purchaseRequest.getReason());

        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(PROCESS_KEY)
                        .setVariables(processInstanceVariables).setBusinessKey(String.valueOf(purchaseRequest.getId()))
                        .setStartUserSelectAssignees(createReqVO.getStartUserSelectAssignees())).getCheckedData();

        // 2.5 将工作流的编号，更新到采购申请中
        ErpPurchaseRequestDO updateObj = new ErpPurchaseRequestDO();
        updateObj.setId(purchaseRequest.getId());
        updateObj.setProcessInstanceId(processInstanceId);
        // 注意：这里不需要再次更新status和bmpStatus，因为在插入时已经设置了正确的状态
        purchaseRequestMapper.updateById(updateObj);

        return purchaseRequest.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePurchaseRequest(ErpPurchaseRequestSaveReqVO updateReqVO) {
        // 1.1 校验存在
        validatePurchaseRequestExists(updateReqVO.getId());
        ErpPurchaseRequestDO purchaseRequest = purchaseRequestMapper.selectById(updateReqVO.getId());
        // 1.2 校验状态：只有草稿状态才能修改
        if (!ErpPurchaseRequestStatusEnum.DRAFT.getStatus().equals(purchaseRequest.getStatus())) {
            throw exception(ErrorCodeConstants.PURCHASE_REQUEST_UPDATE_FAIL_STATUS_NOT_DRAFT);
        }

        // 2.1 更新采购申请
        ErpPurchaseRequestDO updateObj = BeanUtils.toBean(updateReqVO, ErpPurchaseRequestDO.class);
        purchaseRequestMapper.updateById(updateObj);

        // 2.2 更新明细
        updatePurchaseRequestItemList(updateReqVO.getId(), updateReqVO.getItems());

        // 2.3 计算总金额
        updatePurchaseRequestTotalAmount(updateReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePurchaseRequest(Long id) {
        // 1.1 校验存在
        ErpPurchaseRequestDO purchaseRequest = validatePurchaseRequestExists(id);
        // 1.2 校验状态：只有草稿状态才能删除
        if (!ErpPurchaseRequestStatusEnum.DRAFT.getStatus().equals(purchaseRequest.getStatus())) {
            throw exception(ErrorCodeConstants.PURCHASE_REQUEST_DELETE_FAIL_STATUS_NOT_DRAFT);
        }

        // 2.1 删除采购申请
        purchaseRequestMapper.deleteById(id);
        // 2.2 删除明细
        purchaseRequestItemMapper.deleteByRequestId(id);
    }

    private ErpPurchaseRequestDO validatePurchaseRequestExists(Long id) {
        ErpPurchaseRequestDO purchaseRequest = purchaseRequestMapper.selectById(id);
        if (purchaseRequest == null) {
            throw exception(ErrorCodeConstants.PURCHASE_REQUEST_NOT_EXISTS);
        }
        return purchaseRequest;
    }

    @Override
    public ErpPurchaseRequestDO getPurchaseRequest(Long id) {
        return purchaseRequestMapper.selectById(id);
    }

    @Override
    public List<ErpPurchaseRequestDO> getPurchaseRequestList(Collection<Long> ids) {
        return purchaseRequestMapper.selectBatchIds(ids);
    }

    @Override
    public PageResult<ErpPurchaseRequestDO> getPurchaseRequestPage(ErpPurchaseRequestPageReqVO pageReqVO) {
        return purchaseRequestMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitPurchaseRequest(Long id, Long userId) {
        // 1.1 校验存在
        ErpPurchaseRequestDO purchaseRequest = validatePurchaseRequestExists(id);
        // 1.2 校验状态：只有草稿状态才能提交
        if (!ErpPurchaseRequestStatusEnum.DRAFT.getStatus().equals(purchaseRequest.getStatus())) {
            throw exception(ErrorCodeConstants.PURCHASE_REQUEST_SUBMIT_FAIL_STATUS_NOT_DRAFT);
        }

        // 2.1 更新状态为待审核
        ErpPurchaseRequestDO updateObj = new ErpPurchaseRequestDO();
        updateObj.setId(id);
        updateObj.setStatus(ErpPurchaseRequestStatusEnum.PENDING.getStatus());
        purchaseRequestMapper.updateById(updateObj);

        // 2.2 发起 BPM 流程
        Map<String, Object> processInstanceVariables = new HashMap<>();
        processInstanceVariables.put("requestId", id);
        processInstanceVariables.put("applicant", purchaseRequest.getApplicant());
        processInstanceVariables.put("department", purchaseRequest.getDepartment());
        processInstanceVariables.put("totalAmount", purchaseRequest.getTotalAmount());
        processInstanceVariables.put("urgentLevel", purchaseRequest.getUrgentLevel());
        processInstanceVariables.put("title", purchaseRequest.getTitle());
        processInstanceVariables.put("reason", purchaseRequest.getReason());

        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(PROCESS_KEY)
                        .setVariables(processInstanceVariables).setBusinessKey(String.valueOf(id))
                        .setStartUserSelectAssignees(null)).getCheckedData();

        // 2.3 将工作流的编号，更新到采购申请中，同时更新业务状态 - 使用XML方式稳定更新
        purchaseRequestMapper.updateProcessInstanceAndStatus(id, processInstanceId, 
                ErpPurchaseRequestStatusEnum.PENDING.getStatus(), 
                ErpPurchaseRequestBmpStatusEnum.IN_PROGRESS.getStatus());
    }

    @Override
    public void updatePurchaseRequestStatus(Long id, Integer status) {
        validatePurchaseRequestExists(id);
        ErpPurchaseRequestDO updateObj = new ErpPurchaseRequestDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        purchaseRequestMapper.updateById(updateObj);
    }

    @Override
    public void updatePurchaseRequestStatus(Long id, Integer status, String reason) {
        validatePurchaseRequestExists(id);
        ErpPurchaseRequestDO updateObj = new ErpPurchaseRequestDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        updateObj.setAuditReason(reason);
        purchaseRequestMapper.updateById(updateObj);
    }

    @Override
    public void updatePurchaseRequestBmpStatus(Long id, Integer status, Integer bmpStatus) {
        validatePurchaseRequestExists(id);
        ErpPurchaseRequestDO updateObj = new ErpPurchaseRequestDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        updateObj.setBmpStatus(bmpStatus);
        purchaseRequestMapper.updateById(updateObj);
    }

    // ==================== 明细相关 ====================

    private void createPurchaseRequestItemList(Long requestId, List<ErpPurchaseRequestSaveReqVO.Item> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        List<ErpPurchaseRequestItemDO> itemList = BeanUtils.toBean(items, ErpPurchaseRequestItemDO.class);
        itemList.forEach(item -> item.setRequestId(requestId));
        purchaseRequestItemMapper.insertBatch(itemList);
    }

    private void updatePurchaseRequestItemList(Long requestId, List<ErpPurchaseRequestSaveReqVO.Item> items) {
        // 删除旧的，插入新的
        purchaseRequestItemMapper.deleteByRequestId(requestId);
        createPurchaseRequestItemList(requestId, items);
    }

    /**
     * 更新采购申请的总金额
     *
     * @param requestId 采购申请编号
     */
    private void updatePurchaseRequestTotalAmount(Long requestId) {
        List<ErpPurchaseRequestItemDO> items = purchaseRequestItemMapper.selectListByRequestId(requestId);
        BigDecimal totalAmount = items.stream()
                .map(ErpPurchaseRequestItemDO::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        ErpPurchaseRequestDO updateObj = new ErpPurchaseRequestDO();
        updateObj.setId(requestId);
        updateObj.setTotalAmount(totalAmount);
        purchaseRequestMapper.updateById(updateObj);
    }

    /**
     * 验证采购申请的日期字段
     *
     * @param createReqVO 采购申请信息
     */
    private void validateRequestDates(ErpPurchaseRequestSaveReqVO createReqVO) {
        if (createReqVO.getApplyDate() == null) {
            throw exception(ErrorCodeConstants.PURCHASE_REQUEST_APPLY_DATE_NOT_NULL);
        }
        if (createReqVO.getRequiredDate() == null) {
            throw exception(ErrorCodeConstants.PURCHASE_REQUEST_REQUIRED_DATE_NOT_NULL);
        }
        
        // 验证申请日期不能是1970年（表示无效日期）
        Date epoch1970 = new Date(0L); // 1970年1月1日
        Date epoch1970Plus1Day = new Date(24L * 60L * 60L * 1000L); // 1970年1月2日
        
        if (createReqVO.getApplyDate().before(epoch1970Plus1Day)) {
            throw exception(ErrorCodeConstants.PURCHASE_REQUEST_APPLY_DATE_INVALID);
        }
        if (createReqVO.getRequiredDate().before(epoch1970Plus1Day)) {
            throw exception(ErrorCodeConstants.PURCHASE_REQUEST_REQUIRED_DATE_INVALID);
        }
        
        // 验证需求日期不能早于申请日期
        if (createReqVO.getRequiredDate().before(createReqVO.getApplyDate())) {
            throw exception(ErrorCodeConstants.PURCHASE_REQUEST_REQUIRED_DATE_BEFORE_APPLY_DATE);
        }
    }

} 