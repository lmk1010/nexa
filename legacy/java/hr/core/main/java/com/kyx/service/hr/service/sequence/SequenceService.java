package com.kyx.service.hr.service.sequence;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.sequence.vo.SequenceOptionVO;
import com.kyx.service.hr.controller.admin.sequence.vo.SequencePageReqVO;
import com.kyx.service.hr.controller.admin.sequence.vo.SequenceRespVO;
import com.kyx.service.hr.controller.admin.sequence.vo.SequenceSaveReqVO;
import com.kyx.service.hr.dal.dataobject.sequence.SequenceDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 序列管理 Service 接口
 *
 * @author MK
 */
public interface SequenceService {

    /**
     * 创建序列
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createSequence(@Valid SequenceSaveReqVO createReqVO);

    /**
     * 更新序列
     *
     * @param updateReqVO 更新信息
     */
    void updateSequence(@Valid SequenceSaveReqVO updateReqVO);

    /**
     * 删除序列
     *
     * @param id 编号
     */
    void deleteSequence(Long id);

    /**
     * 获得序列
     *
     * @param id 编号
     * @return 序列
     */
    SequenceDO getSequence(Long id);

    /**
     * 获得序列分页
     *
     * @param pageReqVO 分页查询
     * @return 序列分页
     */
    PageResult<SequenceRespVO> getSequencePage(SequencePageReqVO pageReqVO);

    /**
     * 获得序列列表
     *
     * @param ids 编号数组
     * @return 序列列表
     */
    List<SequenceDO> getSequenceList(List<Long> ids);

    /**
     * 获得序列树形列表
     *
     * @return 序列树形列表
     */
    List<SequenceRespVO> getSequenceTree();

    /**
     * 获得序列选项列表
     *
     * @return 序列选项列表
     */
    List<SequenceOptionVO> getSequenceOptions();

    /**
     * 根据序列名称获得序列
     *
     * @param sequenceName 序列名称
     * @return 序列
     */
    SequenceDO getSequenceByName(String sequenceName);

    /**
     * 根据状态获得序列列表
     *
     * @param status 状态
     * @return 序列列表
     */
    List<SequenceDO> getSequenceListByStatus(Integer status);

} 