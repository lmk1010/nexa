package com.kyx.service.im.dal.mysql.invitecode;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.im.controller.admin.invitecode.vo.InviteCodePageReqVO;
import com.kyx.service.im.dal.dataobject.invitecode.InviteCodeDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 邀请码 Mapper
 *
 * @author MK
 */
@Mapper
public interface InviteCodeMapper extends BaseMapperX<InviteCodeDO> {

    /**
     * 分页查询邀请码列表（使用XML方式）
     * 
     * 支持以下查询条件：
     * - 邀请码模糊查询
     * - 类型精确查询
     * - 状态精确查询
     * - 创建时间范围查询
     * - 创建人姓名模糊查询
     * - 租户ID查询（多租户场景）
     * 
     * @param page 分页参数
     * @param reqVO 分页查询参数
     * @return 分页结果
     */
    IPage<InviteCodeDO> selectPageByXml(IPage<InviteCodeDO> page, @Param("ew") InviteCodePageReqVO reqVO);

    /**
     * 根据邀请码查询邀请码信息
     *
     * @param code 邀请码
     * @return 邀请码信息
     */
    default InviteCodeDO selectByCode(String code) {
        return selectOne(InviteCodeDO::getCode, code);
    }

    /**
     * 根据邀请码查询邀请码信息（不使用租户过滤）
     *
     * @param code 邀请码
     * @return 邀请码信息
     */
    InviteCodeDO selectByCodeWithoutTenant(String code);

    /**
     * 根据邀请码查询邀请码信息（包含租户名称）
     *
     * @param code 邀请码
     * @return 邀请码信息
     */
    InviteCodeDO selectByCodeWithTenant(String code);

    /**
     * 根据类型查询邀请码列表
     *
     * @param type 邀请码类型
     * @return 邀请码列表
     */
    default List<InviteCodeDO> selectListByType(Integer type) {
        return selectList(InviteCodeDO::getType, type);
    }

    /**
     * 根据状态查询邀请码列表
     *
     * @param status 邀请码状态
     * @return 邀请码列表
     */
    default List<InviteCodeDO> selectListByStatus(Integer status) {
        return selectList(InviteCodeDO::getStatus, status);
    }

    /**
     * 根据租户ID查询邀请码列表
     *
     * @param tenantId 租户ID
     * @return 邀请码列表
     */
    default List<InviteCodeDO> selectListByTenantId(Long tenantId) {
        return selectList(InviteCodeDO::getTenantId, tenantId);
    }

    /**
     * 根据租户ID和类型查询邀请码列表
     *
     * @param tenantId 租户ID
     * @param type 类型
     * @return 邀请码列表
     */
    default List<InviteCodeDO> selectListByTenantIdAndType(Long tenantId, Integer type) {
        return selectList(new LambdaQueryWrapperX<InviteCodeDO>()
                .eq(InviteCodeDO::getTenantId, tenantId)
                .eq(InviteCodeDO::getType, type));
    }

    /**
     * 查询所有租户的邀请码统计信息
     *
     * @return 租户邀请码统计列表
     */
    List<TenantInviteCodeStatsVO> selectTenantInviteCodeStats();

    /**
     * 更新邀请码使用次数
     *
     * @param id 邀请码ID
     * @return 更新行数
     */
    int updateUsedCount(@Param("id") Long id);

    /**
     * 批量更新过期邀请码状态
     *
     * @return 更新行数
     */
    int updateExpiredStatus();

    /**
     * 批量更新已用完邀请码状态
     *
     * @return 更新行数
     */
    int updateUsedUpStatus();

} 