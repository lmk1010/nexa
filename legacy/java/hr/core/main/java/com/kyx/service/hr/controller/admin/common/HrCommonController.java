package com.kyx.service.hr.controller.admin.common;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.hr.controller.admin.joblevel.vo.JobLevelOptionVO;
import com.kyx.service.hr.controller.admin.sequence.vo.SequenceOptionVO;
import com.kyx.service.hr.dal.dataobject.location.LocationDO;
import com.kyx.service.hr.service.joblevel.JobLevelService;
import com.kyx.service.hr.service.location.LocationService;
import com.kyx.service.hr.service.sequence.SequenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;

import static com.kyx.foundation.common.pojo.CommonResult.success;

/**
 * HR 通用下拉选项 Controller
 *
 * @author MK
 */
@Tag(name = "管理后台 - HR通用下拉选项")
@RestController
@RequestMapping("/hr/common")
public class HrCommonController {

    @Resource
    private JobLevelService jobLevelService;

    @Resource
    private SequenceService sequenceService;

    @Resource
    private LocationService locationService;

    @Resource
    private RestTemplate restTemplate;

    @Value("${kyx.service.business.url:http://localhost:48082}")
    private String businessServiceUrl;

    @GetMapping("/job-level-options")
    @Operation(summary = "获得职级下拉选项列表")
    public CommonResult<List<JobLevelOptionVO>> getJobLevelOptions() {
        List<JobLevelOptionVO> options = jobLevelService.getJobLevelOptions();
        return success(options);
    }

    @GetMapping("/sequence-options")
    @Operation(summary = "获得序列下拉选项列表")
    public CommonResult<List<SequenceOptionVO>> getSequenceOptions() {
        List<SequenceOptionVO> options = sequenceService.getSequenceOptions();
        return success(options);
    }

    @GetMapping("/dept-options")
    @Operation(summary = "获得部门下拉选项列表")
    public CommonResult<List<DeptOptionVO>> getDeptOptions() {
        try {
            // 调用business服务获取部门精简列表
            String url = businessServiceUrl + "/admin-api/system/dept/list-all-simple";
            ResponseEntity<CommonResult<List<DeptSimpleVO>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<CommonResult<List<DeptSimpleVO>>>() {}
            );
            
            if (response.getBody() != null && response.getBody().getData() != null) {
                // 转换为下拉选项格式
                List<DeptOptionVO> options = response.getBody().getData().stream()
                        .map(dept -> {
                            DeptOptionVO option = new DeptOptionVO();
                            option.setValue(dept.getId());
                            option.setLabel(dept.getName());
                            return option;
                        })
                        .collect(Collectors.toList());
                return success(options);
            }
        } catch (Exception e) {
            // 如果调用失败，记录日志并返回空列表
            System.err.println("调用business服务获取部门列表失败: " + e.getMessage());
        }
        
        // 如果调用失败，返回空列表
        return success(new ArrayList<>());
    }

    private DeptOptionVO createDeptOption(Long id, String name) {
        DeptOptionVO option = new DeptOptionVO();
        option.setValue(id);
        option.setLabel(name);
        return option;
    }

    @GetMapping("/location-options")
    @Operation(summary = "获得工作地点下拉选项列表")
    public CommonResult<List<LocationOptionVO>> getLocationOptions() {
        // 获取所有启用的地点
        List<LocationDO> locations = locationService.getLocationList();
        List<LocationOptionVO> options = locations.stream()
                .map(location -> {
                    LocationOptionVO option = new LocationOptionVO();
                    option.setValue(location.getId());
                    option.setLabel(location.getLocationName());
                    return option;
                })
                .collect(Collectors.toList());
        return success(options);
    }

    /**
     * 部门精简信息 VO (用于接收business服务返回的数据)
     */
    public static class DeptSimpleVO {
        private Long id;
        private String name;
        private Long parentId;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getParentId() {
            return parentId;
        }

        public void setParentId(Long parentId) {
            this.parentId = parentId;
        }
    }

    /**
     * 部门选项 VO
     */
    public static class DeptOptionVO {
        private Long value;
        private String label;

        public Long getValue() {
            return value;
        }

        public void setValue(Long value) {
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

    /**
     * 工作地点选项 VO
     */
    public static class LocationOptionVO {
        private Long value;
        private String label;

        public Long getValue() {
            return value;
        }

        public void setValue(Long value) {
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }
} 