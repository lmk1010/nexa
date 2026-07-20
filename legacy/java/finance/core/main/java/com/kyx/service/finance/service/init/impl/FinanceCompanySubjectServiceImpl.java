package com.kyx.service.finance.service.init.impl;

import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.util.collection.CollectionUtils;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.finance.controller.admin.init.vo.company.FinanceCompanySubjectRespVO;
import com.kyx.service.finance.controller.admin.init.vo.company.FinanceCompanySubjectSaveReqVO;
import com.kyx.service.finance.dal.dataobject.init.FinanceCompanyDO;
import com.kyx.service.finance.dal.dataobject.init.FinanceCompanySubjectDO;
import com.kyx.service.finance.dal.mysql.init.FinanceCompanyMapper;
import com.kyx.service.finance.dal.mysql.init.FinanceCompanySubjectMapper;
import com.kyx.service.finance.service.init.FinanceCompanySubjectService;
import com.mzt.logapi.starter.annotation.LogRecord;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.kyx.foundation.common.exception.util.ServiceExceptionUtil.exception;
import static com.kyx.service.finance.enums.ErrorCodeConstants.*;
import static com.kyx.service.finance.enums.LogRecordConstants.*;

/**
 * 账套科目服务实现
 * <p>
 * 核心规则：
 * 1. 分类直属下级为一级科目，一级/二级科目由账套创建时从模板导入，不允许手动新增/删除
 * 2. 三级及以下科目可手动新增/修改/删除
 * 3. 修改时仅允许变更：subjectName / remark / status / sort / feeType / manageSwitch / bizType
 * 4. 删除时校验：无子科目 + 未被凭证/流水引用
 * 5. 末级科目（isLeaf=true）才能被凭证/流水引用
 * 6. 余额增减由 subjectType 决定，无需 balance_dir 字段
 * 7. 新增科目时 feeType/manageSwitch/bizType 继承父级（可覆盖）
 *
 * @author xyang
 */
@Service
public class FinanceCompanySubjectServiceImpl implements FinanceCompanySubjectService {

    private static final String ROOT_SUBJECT_CODE = "0";
    /** 允许手动新增/删除的最小层级（三级及以下） */
    private static final int MIN_EDITABLE_LEVEL = 3;

    @Resource
    private FinanceCompanySubjectMapper companySubjectMapper;
    @Resource
    private FinanceCompanyMapper companyMapper;

    // ----------------------------------------------------------------
    // 新增
    // ----------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = FINANCE_SUBJECT_TEMPLATE_TYPE,
            subType = "新增账套科目",
            bizNo = "{{#_ret}}",
            success = "新增了账套科目【{{#reqVO.subjectCode}}】")
    public Long createCompanySubject(FinanceCompanySubjectSaveReqVO reqVO) {
        // 1. 校验账套存在
        FinanceCompanyDO company = companyMapper.selectById(reqVO.getCompanyId());
        if (company == null) {
            throw exception(COMPANY_NOT_EXISTS);
        }

        // 2. 查找父级科目
        FinanceCompanySubjectDO parent = companySubjectMapper.selectEnabledByCompanyIdAndSubjectCode(
                reqVO.getCompanyId(), reqVO.getParentCode());
        if (parent == null) {
            throw exception(SUBJECT_TEMPLATE_PARENT_CODE_NOT_EXISTS);
        }

        // 3. 新增科目层级必须 >= 3（一级/二级固定不可手动新增）
        int newLevel = parent.getLevel() + 1;
        if (newLevel < MIN_EDITABLE_LEVEL) {
            throw exception(SUBJECT_COMPANY_LEVEL_RESTRICT);
        }

        // 4. 科目编码唯一性校验
        if (checkSubjectCodeExists(reqVO.getCompanyId(), reqVO.getSubjectCode(), null)) {
            throw exception(SUBJECT_TEMPLATE_CODE_EXISTS);
        }

        // 5. 构建新科目（继承父级 subjectType，feeType，余额增减由 subjectType 决定）
        FinanceCompanySubjectDO newSubject = new FinanceCompanySubjectDO();
        newSubject.setCompanyId(reqVO.getCompanyId());
        newSubject.setParentCode(reqVO.getParentCode());
        newSubject.setSubjectCode(reqVO.getSubjectCode());
        newSubject.setSubjectName(reqVO.getSubjectName());
        newSubject.setSubjectType(parent.getSubjectType());   // 继承父级科目类型
        newSubject.setLevel(newLevel);
        newSubject.setIsLeaf(true);                           // 新增科目默认为末级
        newSubject.setSort(reqVO.getSort() != null ? reqVO.getSort() : 999);
        newSubject.setRemark(reqVO.getRemark());
        newSubject.setFeeType(reqVO.getFeeType() != null ? reqVO.getFeeType() : parent.getFeeType()); // 继承/覆盖费用性质
        newSubject.setManageSwitch(reqVO.getManageSwitch() != null ? reqVO.getManageSwitch() : parent.getManageSwitch());
        newSubject.setBizType(reqVO.getBizType() != null ? reqVO.getBizType() : parent.getBizType());
        newSubject.setStatus(reqVO.getStatus() != null ? reqVO.getStatus() : CommonStatusEnum.ENABLE.getStatus());

        // 6. 父级如果是末级，则更新为非末级
        if (Boolean.TRUE.equals(parent.getIsLeaf())) {
            FinanceCompanySubjectDO parentUpdate = new FinanceCompanySubjectDO();
            parentUpdate.setId(parent.getId());
            parentUpdate.setIsLeaf(false);
            companySubjectMapper.updateById(parentUpdate);
        }

        companySubjectMapper.insert(newSubject);
        return newSubject.getId();
    }

    // ----------------------------------------------------------------
    // 修改
    // ----------------------------------------------------------------

    @Override
    @LogRecord(type = FINANCE_SUBJECT_TEMPLATE_TYPE,
            subType = "修改账套科目",
            bizNo = "{{#reqVO.id}}",
            success = "修改了账套科目【{{#reqVO.id}}】")
    public Boolean updateCompanySubject(FinanceCompanySubjectSaveReqVO reqVO) {
        FinanceCompanySubjectDO old = companySubjectMapper.selectById(reqVO.getId());
        if (old == null) {
            throw exception(SUBJECT_COMPANY_NOT_EXISTS);
        }

        // 仅允许修改：名称/备注/状态/排序/feeType/manageSwitch/bizType（三级及以上）
        FinanceCompanySubjectDO update = new FinanceCompanySubjectDO();
        update.setId(reqVO.getId());
        if (reqVO.getSubjectName() != null) {
            update.setSubjectName(reqVO.getSubjectName());
        }
        if (reqVO.getRemark() != null) {
            update.setRemark(reqVO.getRemark());
        }
        if (reqVO.getStatus() != null) {
            update.setStatus(reqVO.getStatus());
        }
        if (reqVO.getSort() != null) {
            update.setSort(reqVO.getSort());
        }
        if (reqVO.getFeeType() != null) {
            update.setFeeType(reqVO.getFeeType());
        }
        if (reqVO.getManageSwitch() != null) {
            update.setManageSwitch(reqVO.getManageSwitch());
        }
        if (reqVO.getBizType() != null) {
            update.setBizType(reqVO.getBizType());
        }
        return companySubjectMapper.updateById(update) > 0;
    }

    // ----------------------------------------------------------------
    // 删除
    // ----------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = FINANCE_SUBJECT_TEMPLATE_TYPE,
            subType = "删除账套科目",
            bizNo = "{{#id}}",
            success = "删除了账套科目【{{#id}}】")
    public Boolean deleteCompanySubject(Long id) {
        FinanceCompanySubjectDO subject = companySubjectMapper.selectById(id);
        if (subject == null) {
            throw exception(SUBJECT_COMPANY_NOT_EXISTS);
        }

        // 一级/二级科目不允许删除
        if (subject.getLevel() < MIN_EDITABLE_LEVEL) {
            throw exception(SUBJECT_COMPANY_LEVEL_RESTRICT);
        }

        // 存在子科目不允许删除
        long childCount = companySubjectMapper.countByCompanyIdAndParentCode(
                subject.getCompanyId(), subject.getSubjectCode());
        if (childCount > 0) {
            throw exception(SUBJECT_TEMPLATE_HAS_CHILDREN);
        }

        // 删除后，若父级无其他子科目，则将父级标记为末级
        long siblingCount = companySubjectMapper.countByCompanyIdAndParentCode(
                subject.getCompanyId(), subject.getParentCode()) - 1;
        if (siblingCount <= 0) {
            FinanceCompanySubjectDO parentSubject = companySubjectMapper
                    .selectEnabledByCompanyIdAndSubjectCode(subject.getCompanyId(), subject.getParentCode());
            if (parentSubject != null) {
                FinanceCompanySubjectDO parentUpdate = new FinanceCompanySubjectDO();
                parentUpdate.setId(parentSubject.getId());
                parentUpdate.setIsLeaf(true);
                companySubjectMapper.updateById(parentUpdate);
            }
        }

        return companySubjectMapper.deleteById(id) > 0;
    }

    // ----------------------------------------------------------------
    // 启用/停用
    // ----------------------------------------------------------------

    @Override
    public Boolean updateCompanySubjectStatus(Long id, Integer status) {
        FinanceCompanySubjectDO subject = companySubjectMapper.selectById(id);
        if (subject == null) {
            throw exception(SUBJECT_COMPANY_NOT_EXISTS);
        }
        FinanceCompanySubjectDO update = new FinanceCompanySubjectDO();
        update.setId(id);
        update.setStatus(status);
        return companySubjectMapper.updateById(update) > 0;
    }

    // ----------------------------------------------------------------
    // 查询
    // ----------------------------------------------------------------

    @Override
    public FinanceCompanySubjectDO getCompanySubject(Long id) {
        return companySubjectMapper.selectById(id);
    }

    @Override
    public List<FinanceCompanySubjectRespVO> listCompanySubjectTree(Long companyId) {
        List<FinanceCompanySubjectDO> list = companySubjectMapper.selectListByCompanyId(companyId);
        if (CollectionUtils.isAnyEmpty(list)) {
            return Collections.emptyList();
        }
        Map<String, List<FinanceCompanySubjectRespVO>> nodes = list.stream()
                .map(t -> BeanUtils.toBean(t, FinanceCompanySubjectRespVO.class))
                .collect(Collectors.groupingBy(
                        t -> StringUtils.isBlank(t.getParentCode()) ? ROOT_SUBJECT_CODE : t.getParentCode()));

        return CollectionUtils.buildTree(nodes,
                FinanceCompanySubjectRespVO::getSubjectCode,
                ROOT_SUBJECT_CODE,
                Comparator.comparingInt(FinanceCompanySubjectRespVO::getLevel)
                        .thenComparing(t -> Optional.ofNullable(t.getSort()).orElse(Integer.MAX_VALUE))
                        .thenComparing(FinanceCompanySubjectRespVO::getSubjectCode));
    }

    @Override
    public List<FinanceCompanySubjectDO> listLeafSubjects(Long companyId) {
        return companySubjectMapper.selectLeafEnabledByCompanyId(companyId);
    }

    @Override
    public FinanceCompanySubjectDO getEnabledByCode(Long companyId, String subjectCode) {
        return companySubjectMapper.selectEnabledByCompanyIdAndSubjectCode(companyId, subjectCode);
    }

    @Override
    public boolean checkSubjectCodeExists(Long companyId, String subjectCode, Long excludeId) {
        FinanceCompanySubjectDO existing = companySubjectMapper
                .selectByCompanyIdAndSubjectCode(companyId, subjectCode);
        if (existing == null) {
            return false;
        }
        return excludeId == null || !existing.getId().equals(excludeId);
    }
}
