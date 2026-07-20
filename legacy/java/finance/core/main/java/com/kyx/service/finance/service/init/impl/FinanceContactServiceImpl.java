package com.kyx.service.finance.service.init.impl;

import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.common.util.collection.CollectionUtils;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.finance.controller.admin.init.vo.contact.FinanceContactGroupSaveReqVO;
import com.kyx.service.finance.controller.admin.init.vo.contact.FinanceContactGroupTreeRespVO;
import com.kyx.service.finance.controller.admin.init.vo.contact.FinanceContactPageReqVO;
import com.kyx.service.finance.controller.admin.init.vo.contact.FinanceContactSaveReqVO;
import com.kyx.service.finance.dal.dataobject.init.FinanceContactDO;
import com.kyx.service.finance.dal.dataobject.init.FinanceContactGroupDO;
import com.kyx.service.finance.dal.mysql.init.FinanceContactGroupMapper;
import com.kyx.service.finance.dal.mysql.init.FinanceContactMapper;
import com.kyx.service.finance.dal.mysql.receivable.FinanceReceivablePayableMapper;
import com.kyx.service.finance.dal.mysql.transaction.FinanceTransactionMapper;
import com.kyx.service.finance.dal.mysql.voucher.FinanceVoucherDetailMapper;
import com.kyx.service.finance.service.init.FinanceContactService;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.service.impl.DiffParseFunction;
import com.mzt.logapi.starter.annotation.LogRecord;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.finance.enums.ErrorCodeConstants.*;
import static com.kyx.service.finance.enums.LogRecordConstants.*;

/**
 * 往来信息服务实现
 *
 * @author xyang
 */
@Service
@Validated
public class FinanceContactServiceImpl implements FinanceContactService {

    private static final int DEFAULT_STATUS = 0;
    private static final long ROOT_PARENT_ID = 0L;

    @Resource
    private FinanceContactMapper financeContactMapper;
    @Resource
    private FinanceContactGroupMapper financeContactGroupMapper;
    @Resource
    private FinanceTransactionMapper financeTransactionMapper;
    @Resource
    private FinanceReceivablePayableMapper financeReceivablePayableMapper;
    @Resource
    private FinanceVoucherDetailMapper financeVoucherDetailMapper;

    @Override
    @LogRecord(type = FINANCE_CONTACT_TEMPLATE_TYPE,
            subType = FINANCE_CONTACT_TEMPLATE_CREATE_SUB_TYPE,
            bizNo = "{{#_ret}}",
            success = FINANCE_CONTACT_TEMPLATE_CREATE_SUCCESS)
    public Long createContact(FinanceContactSaveReqVO reqVO) {
        normalizeContactReq(reqVO);
        validateGroupExists(reqVO.getGroupId());
        FinanceContactDO contactDO = BeanUtils.toBean(reqVO, FinanceContactDO.class);
        if (contactDO.getStatus() == null) {
            contactDO.setStatus(DEFAULT_STATUS);
        }
        financeContactMapper.insert(contactDO);
        return contactDO.getId();
    }

    @Override
    @LogRecord(type = FINANCE_CONTACT_TEMPLATE_TYPE,
            subType = FINANCE_CONTACT_TEMPLATE_UPDATE_SUB_TYPE,
            bizNo = "{{#reqVO.id}}",
            success = FINANCE_CONTACT_TEMPLATE_UPDATE_SUCCESS)
    public Boolean updateContact(FinanceContactSaveReqVO reqVO) {
        FinanceContactDO old = financeContactMapper.selectById(reqVO.getId());
        if (old == null) {
            throw exception(CONTACT_NOT_EXISTS);
        }
        normalizeContactReq(reqVO);
        validateGroupExists(reqVO.getGroupId());
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, old);

        FinanceContactDO contactDO = BeanUtils.toBean(reqVO, FinanceContactDO.class);
        return SqlHelper.retBool(financeContactMapper.updateById(contactDO));
    }

    @Override
    @LogRecord(type = FINANCE_CONTACT_TEMPLATE_TYPE,
            subType = FINANCE_CONTACT_TEMPLATE_DELETE_SUB_TYPE,
            bizNo = "{{#id}}",
            success = FINANCE_CONTACT_TEMPLATE_DELETE_SUCCESS)
    public Boolean deleteContact(Long id) {
        FinanceContactDO contactDO = financeContactMapper.selectById(id);
        if (contactDO == null) {
            return false;
        }
        if (financeTransactionMapper.existsByContactId(id)
                || financeReceivablePayableMapper.existsByContactId(id)
                || financeVoucherDetailMapper.existsByContactId(id)) {
            throw exception(CONTACT_USED);
        }
        return SqlHelper.retBool(financeContactMapper.deleteById(contactDO));
    }

    @Override
    public Boolean updateContactStatus(Long id, Integer status) {
        FinanceContactDO contactDO = financeContactMapper.selectById(id);
        if (contactDO == null) {
            throw exception(CONTACT_NOT_EXISTS);
        }
        contactDO.setStatus(status);
        return SqlHelper.retBool(financeContactMapper.updateById(contactDO));
    }

    @Override
    public Boolean batchUpdateContactStatus(Collection<Long> ids, Integer status) {
        List<Long> normalizedIds = normalizeIdList(ids);
        if (normalizedIds.isEmpty()) {
            return true;
        }
        return SqlHelper.retBool(financeContactMapper.updateStatusByIds(normalizedIds, status));
    }

    @Override
    public Boolean batchDeleteContact(Collection<Long> ids) {
        List<Long> normalizedIds = normalizeIdList(ids);
        if (normalizedIds.isEmpty()) {
            return true;
        }
        for (Long id : normalizedIds) {
            deleteContact(id);
        }
        return true;
    }

    @Override
    public FinanceContactDO getContact(Long id) {
        FinanceContactDO contactDO = financeContactMapper.selectById(id);
        if (contactDO == null) {
            throw exception(CONTACT_NOT_EXISTS);
        }
        FinanceContactGroupDO groupDO = financeContactGroupMapper.selectById(contactDO.getGroupId());
        if (groupDO != null) {
            contactDO.setGroupName(groupDO.getGroupName());
        }
        return contactDO;
    }

    @Override
    public PageResult<FinanceContactDO> pageContact(FinanceContactPageReqVO reqVO) {
        List<FinanceContactGroupDO> groups = financeContactGroupMapper.selectAll();
        Map<Long, FinanceContactGroupDO> groupMap = groups.stream()
                .collect(Collectors.toMap(FinanceContactGroupDO::getId, item -> item));

        if (reqVO.getGroupId() != null) {
            if (!groupMap.containsKey(reqVO.getGroupId())) {
                throw exception(CONTACT_GROUP_NOT_EXISTS);
            }
            reqVO.setGroupIds(new ArrayList<>(collectGroupIds(reqVO.getGroupId(), groups)));
        } else {
            reqVO.setGroupIds(null);
        }

        PageResult<FinanceContactDO> pageResult = financeContactMapper.selectPage(reqVO);
        if (pageResult.getList() == null || pageResult.getList().isEmpty()) {
            return pageResult;
        }
        for (FinanceContactDO contactDO : pageResult.getList()) {
            FinanceContactGroupDO groupDO = groupMap.get(contactDO.getGroupId());
            if (groupDO != null) {
                contactDO.setGroupName(groupDO.getGroupName());
            }
        }
        return pageResult;
    }

    @Override
    public List<FinanceContactGroupTreeRespVO> listContactGroupTree() {
        List<FinanceContactGroupDO> groups = financeContactGroupMapper.selectAll();
        if (groups.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<FinanceContactGroupTreeRespVO>> nodeMap = groups.stream()
                .map(group -> BeanUtils.toBean(group, FinanceContactGroupTreeRespVO.class))
                .collect(Collectors.groupingBy(node -> node.getParentId() == null ? ROOT_PARENT_ID : node.getParentId()));
        ToIntFunction<FinanceContactGroupTreeRespVO> keySort = node -> node.getSort() == null ? 0 : node.getSort();
        return CollectionUtils.buildTree(nodeMap, FinanceContactGroupTreeRespVO::getId, ROOT_PARENT_ID,
                Comparator.comparingInt(keySort).thenComparing(FinanceContactGroupTreeRespVO::getId));
    }

    @Override
    public Long createContactGroup(FinanceContactGroupSaveReqVO reqVO) {
        Long parentId = reqVO.getParentId();
        if (parentId == null || parentId <= ROOT_PARENT_ID) {
            throw exception(CONTACT_GROUP_NOT_EXISTS);
        }
        FinanceContactGroupDO parent = financeContactGroupMapper.selectById(parentId);
        if (parent == null) {
            throw exception(CONTACT_GROUP_NOT_EXISTS);
        }
        String groupName = normalizeGroupName(reqVO.getGroupName());
        validateGroupNameUnique(parentId, groupName, null);

        Integer sort = reqVO.getSort();
        if (sort == null) {
            Integer maxSort = financeContactGroupMapper.selectMaxSortByParentId(parentId);
            sort = maxSort == null ? 10 : maxSort + 10;
        }
        Integer parentLevel = parent.getLevel() == null ? 1 : parent.getLevel();
        FinanceContactGroupDO groupDO = FinanceContactGroupDO.builder()
                .groupName(groupName)
                .parentId(parentId)
                .ancestors(buildAncestors(parent))
                .level(parentLevel + 1)
                .sort(sort)
                .levelFixed(false)
                .editable(true)
                .status(DEFAULT_STATUS)
                .build();
        financeContactGroupMapper.insert(groupDO);
        return groupDO.getId();
    }

    @Override
    public Boolean updateContactGroup(FinanceContactGroupSaveReqVO reqVO) {
        if (reqVO.getId() == null) {
            throw exception(CONTACT_GROUP_NOT_EXISTS);
        }
        FinanceContactGroupDO old = financeContactGroupMapper.selectById(reqVO.getId());
        if (old == null) {
            throw exception(CONTACT_GROUP_NOT_EXISTS);
        }
        if (!Boolean.TRUE.equals(old.getEditable())) {
            throw exception(CONTACT_GROUP_NOT_EDITABLE);
        }
        String groupName = normalizeGroupName(reqVO.getGroupName());
        validateGroupNameUnique(old.getParentId(), groupName, old.getId());

        FinanceContactGroupDO update = new FinanceContactGroupDO();
        update.setId(old.getId());
        update.setGroupName(groupName);
        if (reqVO.getSort() != null) {
            update.setSort(reqVO.getSort());
        }
        return SqlHelper.retBool(financeContactGroupMapper.updateById(update));
    }

    @Override
    public Boolean deleteContactGroup(Long id) {
        if (id == null || id <= ROOT_PARENT_ID) {
            throw exception(CONTACT_GROUP_NOT_EXISTS);
        }
        FinanceContactGroupDO groupDO = financeContactGroupMapper.selectById(id);
        if (groupDO == null) {
            throw exception(CONTACT_GROUP_NOT_EXISTS);
        }
        if (!Boolean.TRUE.equals(groupDO.getEditable())) {
            throw exception(CONTACT_GROUP_NOT_EDITABLE);
        }
        List<FinanceContactGroupDO> groups = financeContactGroupMapper.selectAll();
        Set<Long> groupIds = collectGroupIds(id, groups);
        List<Long> scopeIds = new ArrayList<>(groupIds);
        if (financeContactMapper.existsByGroupIds(scopeIds)) {
            throw exception(CONTACT_GROUP_HAS_CONTACT);
        }
        return financeContactGroupMapper.deleteBatchIds(scopeIds) > 0;
    }

    private Set<Long> collectGroupIds(Long rootGroupId, List<FinanceContactGroupDO> groups) {
        Map<Long, List<FinanceContactGroupDO>> childrenMap = groups.stream()
                .collect(Collectors.groupingBy(group -> group.getParentId() == null ? ROOT_PARENT_ID : group.getParentId()));
        LinkedHashSet<Long> result = new LinkedHashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        queue.add(rootGroupId);
        while (!queue.isEmpty()) {
            Long currentId = queue.poll();
            if (!result.add(currentId)) {
                continue;
            }
            List<FinanceContactGroupDO> children = childrenMap.get(currentId);
            if (children == null || children.isEmpty()) {
                continue;
            }
            for (FinanceContactGroupDO child : children) {
                queue.offer(child.getId());
            }
        }
        return result;
    }

    private String buildAncestors(FinanceContactGroupDO parent) {
        if (parent == null || parent.getId() == null || parent.getId() <= ROOT_PARENT_ID) {
            return String.valueOf(ROOT_PARENT_ID);
        }
        String parentAncestors = StringUtils.hasText(parent.getAncestors())
                ? StringUtils.trimWhitespace(parent.getAncestors())
                : String.valueOf(ROOT_PARENT_ID);
        return parentAncestors + "," + parent.getId();
    }

    private void validateGroupExists(Long groupId) {
        if (groupId == null || groupId <= 0) {
            throw exception(CONTACT_GROUP_NOT_EXISTS);
        }
        FinanceContactGroupDO groupDO = financeContactGroupMapper.selectById(groupId);
        if (groupDO == null) {
            throw exception(CONTACT_GROUP_NOT_EXISTS);
        }
    }

    private void validateGroupNameUnique(Long parentId, String groupName, Long excludeId) {
        if (financeContactGroupMapper.existsByParentIdAndName(parentId, groupName, excludeId)) {
            throw exception(CONTACT_GROUP_NAME_EXISTS);
        }
    }

    private String normalizeGroupName(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return StringUtils.trimWhitespace(value);
    }

    private void normalizeContactReq(FinanceContactSaveReqVO reqVO) {
        reqVO.setContactName(normalizeRequiredText(reqVO.getContactName()));
        reqVO.setAddress(normalizeOptionalText(reqVO.getAddress()));
        reqVO.setAccountType(normalizeOptionalText(reqVO.getAccountType()));
        reqVO.setAccountName(normalizeOptionalText(reqVO.getAccountName()));
        reqVO.setAccountNo(normalizeOptionalText(reqVO.getAccountNo()));
        reqVO.setOwnerName(normalizeOptionalText(reqVO.getOwnerName()));
        reqVO.setPhone(normalizeOptionalText(reqVO.getPhone()));
        reqVO.setRemark(normalizeOptionalText(reqVO.getRemark()));
    }

    private String normalizeRequiredText(String value) {
        return StringUtils.trimWhitespace(value);
    }

    private String normalizeOptionalText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return StringUtils.trimWhitespace(value);
    }

    private List<Long> normalizeIdList(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return ids.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        ArrayList::new
                ));
    }
}
