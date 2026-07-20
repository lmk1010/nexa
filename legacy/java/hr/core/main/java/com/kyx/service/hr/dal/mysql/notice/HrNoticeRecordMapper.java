package com.kyx.service.hr.dal.mysql.notice;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.hr.controller.admin.notice.vo.HrNoticeRecordPageReqVO;
import com.kyx.service.hr.dal.dataobject.notice.HrNoticeRecordDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

/**
 * HR notice record mapper.
 */
@Mapper
public interface HrNoticeRecordMapper extends BaseMapperX<HrNoticeRecordDO> {

    default PageResult<HrNoticeRecordDO> selectPage(HrNoticeRecordPageReqVO reqVO) {
        LambdaQueryWrapperX<HrNoticeRecordDO> wrapper = new LambdaQueryWrapperX<HrNoticeRecordDO>()
                .eqIfPresent(HrNoticeRecordDO::getChannel, reqVO.getChannel())
                .eqIfPresent(HrNoticeRecordDO::getBusinessType, reqVO.getBusinessType())
                .eqIfPresent(HrNoticeRecordDO::getSendStatus, reqVO.getSendStatus());
        if (StringUtils.hasText(reqVO.getKeyword())) {
            String keyword = reqVO.getKeyword().trim();
            wrapper.and(query -> query.like(HrNoticeRecordDO::getTitle, keyword)
                    .or()
                    .like(HrNoticeRecordDO::getContent, keyword)
                    .or()
                    .like(HrNoticeRecordDO::getNoticeKey, keyword)
                    .or()
                    .like(HrNoticeRecordDO::getBusinessType, keyword)
                    .or()
                    .like(HrNoticeRecordDO::getChannel, keyword)
                    .or()
                    .like(HrNoticeRecordDO::getErrorMessage, keyword));
        }
        wrapper.orderByDesc(HrNoticeRecordDO::getSendTime)
                .orderByDesc(HrNoticeRecordDO::getId);
        return selectPage(reqVO, wrapper);
    }

    default HrNoticeRecordDO selectByNoticeKey(String noticeKey) {
        return selectOne(HrNoticeRecordDO::getNoticeKey, noticeKey);
    }

}
