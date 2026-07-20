package com.kyx.service.hr.controller.admin.training.vo;

import com.kyx.foundation.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TrainingCoursePageReqVO extends PageParam {

    private String courseName;

    private String courseType;

    private String category;

    private String lecturer;

    private String provider;

    private Integer status;

}
