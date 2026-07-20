package com.kyx.service.hr.service.joblevel;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.joblevel.vo.JobLevelOptionVO;
import com.kyx.service.hr.controller.admin.joblevel.vo.JobLevelPageReqVO;
import com.kyx.service.hr.controller.admin.joblevel.vo.JobLevelRespVO;
import com.kyx.service.hr.controller.admin.joblevel.vo.JobLevelSaveReqVO;
import com.kyx.service.hr.dal.dataobject.joblevel.JobLevelDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 职级管理 Service 接口
 *
 * @author MK
 */
public interface JobLevelService {

    /**
     * 创建职级
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createJobLevel(@Valid JobLevelSaveReqVO createReqVO);

    /**
     * 更新职级
     *
     * @param updateReqVO 更新信息
     */
    void updateJobLevel(@Valid JobLevelSaveReqVO updateReqVO);

    /**
     * 删除职级
     *
     * @param id 编号
     */
    void deleteJobLevel(Long id);

    /**
     * 获得职级
     *
     * @param id 编号
     * @return 职级
     */
    JobLevelDO getJobLevel(Long id);

    /**
     * 获得职级分页
     *
     * @param pageReqVO 分页查询
     * @return 职级分页
     */
    PageResult<JobLevelRespVO> getJobLevelPage(JobLevelPageReqVO pageReqVO);

    /**
     * 获得职级列表
     *
     * @return 职级列表
     */
    List<JobLevelRespVO> getJobLevelList();

    /**
     * 获得职级选项列表
     *
     * @return 职级选项列表
     */
    List<JobLevelOptionVO> getJobLevelOptions();

    /**
     * 根据序列ID获得职级列表
     *
     * @param sequenceId 序列ID
     * @return 职级列表
     */
    List<JobLevelRespVO> getJobLevelListBySequenceId(Long sequenceId);

    /**
     * 根据职级编码获得职级
     *
     * @param levelCode 职级编码
     * @return 职级
     */
    JobLevelDO getJobLevelByCode(String levelCode);

    /**
     * 校验职级是否存在
     *
     * @param id 编号
     */
    void validateJobLevelExists(Long id);

    /**
     * 校验职级编码是否唯一
     *
     * @param id 编号
     * @param levelCode 职级编码
     */
    void validateLevelCodeUnique(Long id, String levelCode);

} 