package com.kyx.service.erp.service.asset;

import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.erp.controller.admin.asset.vo.myassets.ErpMyOwnedAssetsPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.myassets.ErpMyOwnedAssetsRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.myassets.ErpMyCheckoutPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.myassets.ErpMyCheckoutRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.myassets.ErpMyReturnPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.myassets.ErpMyReturnRespVO;
import com.kyx.service.erp.controller.admin.asset.vo.myassets.ErpMyTransferPageReqVO;
import com.kyx.service.erp.controller.admin.asset.vo.myassets.ErpMyTransferRespVO;

/**
 * ERP 我的资产 Service 接口
 *
 * @author kyx
 */
public interface ErpMyAssetsService {

    /**
     * 获得我拥有的资产分页
     *
     * @param userId 用户编号
     * @param pageReqVO 分页查询
     * @return 我拥有的资产分页
     */
    PageResult<ErpMyOwnedAssetsRespVO> getMyOwnedAssetsPage(Long userId, ErpMyOwnedAssetsPageReqVO pageReqVO);

    /**
     * 获得我申请领用的资产分页
     *
     * @param userId 用户编号
     * @param pageReqVO 分页查询
     * @return 我申请领用的资产分页
     */
    PageResult<ErpMyCheckoutRespVO> getMyCheckoutPage(Long userId, ErpMyCheckoutPageReqVO pageReqVO);

    /**
     * 获得我的归还记录分页
     *
     * @param userId 用户编号
     * @param pageReqVO 分页查询
     * @return 我的归还记录分页
     */
    PageResult<ErpMyReturnRespVO> getMyReturnPage(Long userId, ErpMyReturnPageReqVO pageReqVO);

    /**
     * 获得我转移的资产分页
     *
     * @param userId 用户编号
     * @param pageReqVO 分页查询
     * @return 我转移的资产分页
     */
    PageResult<ErpMyTransferRespVO> getMyTransferPage(Long userId, ErpMyTransferPageReqVO pageReqVO);

} 