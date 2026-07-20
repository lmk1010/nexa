package com.kyx.service.business.controller.admin.dept.vo.postsync;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 外部系统岗位数据 DTO
 *
 * @author MK
 */
@Data
public class ExternalPostDTO {

    /**
     * 外部系统岗位ID
     */
    private Long postId;

    /**
     * 外部系统岗位编码
     */
    private String postCode;

    /**
     * 外部系统岗位名称
     */
    private String postName;

    /**
     * 外部系统显示顺序
     */
    private String postSort;

    /**
     * 外部系统状态（0正常 1停用）
     */
    private String status;

    /**
     * 外部系统备注
     */
    private String remark;

    /**
     * 外部系统创建者
     */
    private String createBy;

    /**
     * 外部系统创建时间
     */
    private String createTime;

    /**
     * 外部系统更新者
     */
    private String updateBy;

    /**
     * 外部系统更新时间
     */
    private String updateTime;

    /**
     * 是否标记（前端使用）
     */
    private Boolean flag;

}