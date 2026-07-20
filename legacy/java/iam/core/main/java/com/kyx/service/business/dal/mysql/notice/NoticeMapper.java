package com.kyx.service.business.dal.mysql.notice;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.business.controller.admin.notice.vo.NoticePageReqVO;
import com.kyx.service.business.dal.dataobject.notice.NoticeDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface NoticeMapper extends BaseMapperX<NoticeDO> {

    String WORK_NOTICE_VISIBLE_CONDITION = "AND (n.receiver_type IS NULL " +
            "OR n.receiver_type = '' " +
            "OR n.receiver_type = 'ALL' " +
            "OR (n.receiver_type = 'USER' " +
            "AND n.receiver_user_ids IS NOT NULL " +
            "AND FIND_IN_SET((CAST(#{userId} AS CHAR) COLLATE utf8mb4_general_ci), " +
            "n.receiver_user_ids COLLATE utf8mb4_general_ci) > 0)) ";

    default PageResult<NoticeDO> selectPage(NoticePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<NoticeDO>()
                .likeIfPresent(NoticeDO::getTitle, reqVO.getTitle())
                .eqIfPresent(NoticeDO::getStatus, reqVO.getStatus())
                .orderByDesc(NoticeDO::getId));
    }

    @Select("<script>" +
            "SELECT n.* " +
            "FROM system_notice n " +
            "LEFT JOIN system_notice_read r " +
            "ON n.id = r.notice_id " +
            "AND r.user_id = #{userId} " +
            "AND r.user_type = #{userType} " +
            "AND r.deleted = 0 " +
            "WHERE n.deleted = 0 " +
            "AND n.status = 0 " +
            WORK_NOTICE_VISIBLE_CONDITION +
            "<if test='createTime != null and createTime.length == 2'>" +
            "AND n.create_time &gt;= #{createTime[0]} " +
            "AND n.create_time &lt;= #{createTime[1]} " +
            "</if>" +
            "<if test='readStatus != null'>" +
            "<choose>" +
            "<when test='readStatus'>AND r.notice_id IS NOT NULL</when>" +
            "<otherwise>AND r.notice_id IS NULL</otherwise>" +
            "</choose>" +
            "</if>" +
            "ORDER BY n.create_time DESC, n.id DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<NoticeDO> selectWorkNoticeList(@Param("userId") Long userId,
                                        @Param("userType") Integer userType,
                                        @Param("readStatus") Boolean readStatus,
                                        @Param("createTime") LocalDateTime[] createTime,
                                        @Param("limit") Integer limit);

    @Select("<script>" +
            "SELECT COUNT(*) " +
            "FROM system_notice n " +
            "LEFT JOIN system_notice_read r " +
            "ON n.id = r.notice_id " +
            "AND r.user_id = #{userId} " +
            "AND r.user_type = #{userType} " +
            "AND r.deleted = 0 " +
            "WHERE n.deleted = 0 " +
            "AND n.status = 0 " +
            WORK_NOTICE_VISIBLE_CONDITION +
            "<if test='createTime != null and createTime.length == 2'>" +
            "AND n.create_time &gt;= #{createTime[0]} " +
            "AND n.create_time &lt;= #{createTime[1]} " +
            "</if>" +
            "<if test='readStatus != null'>" +
            "<choose>" +
            "<when test='readStatus'>AND r.notice_id IS NOT NULL</when>" +
            "<otherwise>AND r.notice_id IS NULL</otherwise>" +
            "</choose>" +
            "</if>" +
            "</script>")
    Long selectWorkNoticeCount(@Param("userId") Long userId,
                               @Param("userType") Integer userType,
                               @Param("readStatus") Boolean readStatus,
                               @Param("createTime") LocalDateTime[] createTime);

    @Select("<script>" +
            "SELECT n.id " +
            "FROM system_notice n " +
            "LEFT JOIN system_notice_read r " +
            "ON n.id = r.notice_id " +
            "AND r.user_id = #{userId} " +
            "AND r.user_type = #{userType} " +
            "AND r.deleted = 0 " +
            "WHERE n.deleted = 0 " +
            "AND n.status = 0 " +
            WORK_NOTICE_VISIBLE_CONDITION +
            "AND r.notice_id IS NULL" +
            "</script>")
    List<Long> selectUnreadNoticeIds(@Param("userId") Long userId,
                                     @Param("userType") Integer userType);

    default List<Long> selectVisibleNoticeIds(Long userId, Integer userType, Collection<Long> noticeIds) {
        if (noticeIds == null || noticeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectVisibleNoticeIds0(userId, userType, noticeIds);
    }

    @Select("<script>" +
            "SELECT n.id " +
            "FROM system_notice n " +
            "WHERE n.deleted = 0 " +
            "AND n.status = 0 " +
            WORK_NOTICE_VISIBLE_CONDITION +
            "AND n.id IN " +
            "<foreach collection='noticeIds' item='noticeId' open='(' separator=',' close=')'>" +
            "#{noticeId}" +
            "</foreach>" +
            "</script>")
    List<Long> selectVisibleNoticeIds0(@Param("userId") Long userId,
                                       @Param("userType") Integer userType,
                                       @Param("noticeIds") Collection<Long> noticeIds);

}
