package com.kyx.service.hr.service.attendance;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceClockInReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceClockRecordPageReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceClockRecordRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceClockRecordSummaryRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMyMonthDayRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceMyTodayRespVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceSyncReqVO;
import com.kyx.service.hr.controller.admin.attendance.vo.AttendanceWorkbenchRespVO;

import javax.validation.Valid;
import java.util.List;

/**
 * 员工打卡 Service 接口
 *
 * @author MK
 */
public interface AttendanceClockRecordService {

    /**
     * 员工打卡
     *
     * @param reqVO 打卡信息
     * @return 打卡记录 ID
     */
    Long clock(@Valid AttendanceClockInReqVO reqVO);

    /**
     * 获取我的今日打卡信息
     *
     * @return 今日打卡
     */
    AttendanceMyTodayRespVO getMyToday();

    /**
     * 获取我的月度打卡日历
     *
     * @param year 年份
     * @param month 月份
     * @return 月度打卡
     */
    List<AttendanceMyMonthDayRespVO> getMyMonth(Integer year, Integer month);

    /**
     * 获取打卡记录分页
     *
     * @param pageReqVO 分页参数
     * @return 分页结果
     */
    PageResult<AttendanceClockRecordRespVO> getClockRecordPage(AttendanceClockRecordPageReqVO pageReqVO);

    /**
     * 获取我的考勤分页
     *
     * @param pageReqVO 分页参数
     * @return 分页结果
     */
    PageResult<AttendanceClockRecordRespVO> getMyClockRecordPage(AttendanceClockRecordPageReqVO pageReqVO);

    /**
     * 获取考勤汇总
     *
     * @param pageReqVO 查询参数
     * @return 汇总结果
     */
    AttendanceClockRecordSummaryRespVO getClockRecordSummary(AttendanceClockRecordPageReqVO pageReqVO);

    /**
     * 获取考勤工作台。
     *
     * @return 工作台数据
     */
    AttendanceWorkbenchRespVO getWorkbench();

    /**
     * 同步钉钉打卡
     *
     * @param reqVO 同步请求
     * @return 成功处理数量
     */
    Integer syncDingTalk(@Valid AttendanceSyncReqVO reqVO);

    /**
     * Sync DingTalk attendance records and return create/update counters.
     *
     * @param reqVO sync request
     * @return sync result
     */
    AttendanceDingTalkSyncResult syncDingTalkDetailed(@Valid AttendanceSyncReqVO reqVO);

}
