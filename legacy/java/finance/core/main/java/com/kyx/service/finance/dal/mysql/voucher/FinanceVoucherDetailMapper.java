package com.kyx.service.finance.dal.mysql.voucher;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.foundation.mybatis.core.query.LambdaQueryWrapperX;
import com.kyx.service.finance.dal.dataobject.voucher.FinanceVoucherDetailDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 凭证明细 Mapper
 *
 * @author xyang
 */
@Mapper
public interface FinanceVoucherDetailMapper extends BaseMapperX<FinanceVoucherDetailDO> {

    default List<FinanceVoucherDetailDO> selectListByVoucherId(Long voucherId) {
        return selectList(new LambdaQueryWrapperX<FinanceVoucherDetailDO>()
                .eq(FinanceVoucherDetailDO::getVoucherId, voucherId)
                .orderByAsc(FinanceVoucherDetailDO::getLineNo, FinanceVoucherDetailDO::getId));
    }

    /**
     * 物理删除凭证明细（用于更新场景）
     * 说明：finance_voucher_detail 存在唯一索引 (voucher_id, line_no)，
     * 若仅逻辑删除会导致同 voucher 重建分录时唯一键冲突。
     */
    @Delete("DELETE FROM finance_voucher_detail WHERE voucher_id = #{voucherId}")
    void deletePhysicallyByVoucherId(@Param("voucherId") Long voucherId);

    default void deleteByVoucherId(Long voucherId) {
        deletePhysicallyByVoucherId(voucherId);
    }

    default boolean existsByAccountId(Long accountId) {
        if (accountId == null) {
            return false;
        }
        return selectCount(new LambdaQueryWrapperX<FinanceVoucherDetailDO>()
                .eq(FinanceVoucherDetailDO::getAccountId, accountId)) > 0;
    }

    default boolean existsByContactId(Long contactId) {
        if (contactId == null) {
            return false;
        }
        return selectCount(new LambdaQueryWrapperX<FinanceVoucherDetailDO>()
                .eq(FinanceVoucherDetailDO::getContactId, contactId)) > 0;
    }
}
