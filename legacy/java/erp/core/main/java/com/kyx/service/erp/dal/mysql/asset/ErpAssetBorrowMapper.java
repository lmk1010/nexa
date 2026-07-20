package com.kyx.service.erp.dal.mysql.asset;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.erp.controller.admin.asset.vo.borrow.ErpAssetBorrowPageReqVO;
import com.kyx.service.erp.dal.dataobject.asset.ErpAssetBorrowDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

/**
 * ERP 资产借用记录 Mapper
 * 
 * 业务说明：专门处理临时借用业务，与长期领用业务分离
 * 对应表：erp_asset_borrow
 *
 * @author kyx
 */
@Mapper
public interface ErpAssetBorrowMapper extends BaseMapperX<ErpAssetBorrowDO> {

    default PageResult<ErpAssetBorrowDO> selectPage(ErpAssetBorrowPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpAssetBorrowDO>()
                .likeIfPresent(ErpAssetBorrowDO::getBorrowNo, reqVO.getBorrowNo())
                .eqIfPresent(ErpAssetBorrowDO::getAssetId, reqVO.getAssetId())
                .eqIfPresent(ErpAssetBorrowDO::getBorrowUserId, reqVO.getBorrowUserId())
                .eqIfPresent(ErpAssetBorrowDO::getBorrowDeptId, reqVO.getBorrowDeptId())
                .betweenIfPresent(ErpAssetBorrowDO::getBorrowDate, reqVO.getBorrowDate())
                .betweenIfPresent(ErpAssetBorrowDO::getExpectedReturnDate, reqVO.getExpectedReturnDate())
                .eqIfPresent(ErpAssetBorrowDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpAssetBorrowDO::getApprovalStatus, reqVO.getApprovalStatus())
                .orderByDesc(ErpAssetBorrowDO::getId));
    }

    default ErpAssetBorrowDO selectByAssetIdAndStatus(Long assetId, Integer status) {
        return selectOne(ErpAssetBorrowDO::getAssetId, assetId, ErpAssetBorrowDO::getStatus, status);
    }

    default List<ErpAssetBorrowDO> selectByAssetId(Long assetId) {
        return selectList(ErpAssetBorrowDO::getAssetId, assetId);
    }

    default List<ErpAssetBorrowDO> selectByBorrowUserId(Long borrowUserId) {
        return selectList(ErpAssetBorrowDO::getBorrowUserId, borrowUserId);
    }

    default List<ErpAssetBorrowDO> selectOverdueReturns() {
        return selectList(new LambdaQueryWrapperX<ErpAssetBorrowDO>()
                .eq(ErpAssetBorrowDO::getStatus, 1) // 借用中
                .lt(ErpAssetBorrowDO::getExpectedReturnDate, LocalDate.now()) // 超过预计归还日期
                .orderByAsc(ErpAssetBorrowDO::getExpectedReturnDate));
    }

    /**
     * 根据BMP流程实例ID查询借用记录
     */
    default ErpAssetBorrowDO selectByBmpProcessInstanceId(String bmpProcessInstanceId) {
        return selectOne(ErpAssetBorrowDO::getBmpProcessInstanceId, bmpProcessInstanceId);
    }
} 