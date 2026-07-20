package com.kyx.service.business.controller.admin.dept.vo.postsync;

import lombok.Data;

import java.util.List;

/**
 * 外部系统岗位API响应 DTO
 *
 * @author MK
 */
@Data
public class ExternalPostApiResponse {

    /**
     * 总数
     */
    private Long total;

    /**
     * 岗位列表
     */
    private List<ExternalPostDTO> rows;

    /**
     * 响应码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String msg;

}