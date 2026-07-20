package com.kyx.service.business.service.dept;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.kyx.service.business.controller.admin.dept.vo.post.PostSaveReqVO;
import com.kyx.service.business.controller.admin.dept.vo.postsync.ExternalPostApiResponse;
import com.kyx.service.business.controller.admin.dept.vo.postsync.ExternalPostDTO;
import com.kyx.service.business.dal.dataobject.dept.PostDO;
import com.kyx.service.business.dal.dataobject.dept.PostSyncDO;
import com.kyx.service.business.dal.mysql.dept.PostSyncMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 岗位同步 Service 实现类
 *
 * @author MK
 */
@Service
@Validated
@Slf4j
public class PostSyncServiceImpl implements PostSyncService {

    @Resource
    private PostSyncMapper postSyncMapper;

    @Resource
    private PostService postService;

    // 外部系统配置，从配置文件读取
    @Value("${sync.external.post.api-url:https://order.liantucn.com/api/system/post/list?pageNum=1&pageSize=100}")
    private String externalApiUrl;

    @Value("${sync.external.post.auth-token:}")
    private String externalAuthToken;

    @Value("${sync.external.post.cookie:}")
    private String externalCookie;

    @Value("${sync.external.post.uuid:}")
    private String externalUuid;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int fetchAndSaveExternalPosts() {
        try {
            log.info("开始拉取外部岗位数据...");
            
            // 拉取外部数据
            ExternalPostApiResponse apiResponse = fetchExternalPosts();
            
            if (apiResponse.getRows() == null || apiResponse.getRows().isEmpty()) {
                log.warn("外部系统未返回岗位数据");
                return 0;
            }

            // 保存到同步表
            int savedCount = saveExternalPosts(apiResponse.getRows());
            
            log.info("成功拉取并保存外部岗位数据，共{}条", savedCount);
            return savedCount;
            
        } catch (Exception e) {
            log.error("拉取并保存外部岗位数据失败", e);
            throw new RuntimeException("拉取并保存外部岗位数据失败：" + e.getMessage());
        }
    }

    /**
     * 从外部系统拉取岗位数据
     */
    private ExternalPostApiResponse fetchExternalPosts() {
        try {
            // 构建HTTP请求
            HttpRequest request = HttpRequest.get(externalApiUrl)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Authorization", externalAuthToken)
                    .header("Connection", "keep-alive")
                    .header("Sec-Fetch-Dest", "empty")
                    .header("Sec-Fetch-Mode", "cors")
                    .header("Sec-Fetch-Site", "same-origin")
                    .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36")
                    .header("sec-ch-ua", "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Google Chrome\";v=\"138\"")
                    .header("sec-ch-ua-mobile", "?0")
                    .header("sec-ch-ua-platform", "\"macOS\"");

            // 添加Cookie信息
            if (StrUtil.isNotBlank(externalCookie)) {
                request.header("Cookie", externalCookie);
            }

            // 添加UUID信息
            if (StrUtil.isNotBlank(externalUuid)) {
                request.header("uuid", externalUuid);
            }

            // 执行请求
            HttpResponse response = request.execute();

            if (!response.isOk()) {
                throw new RuntimeException("外部API调用失败，状态码：" + response.getStatus());
            }

            String responseBody = response.body();
            log.debug("外部API响应：{}", responseBody);

            // 解析响应
            ExternalPostApiResponse apiResponse = JSONUtil.toBean(responseBody, ExternalPostApiResponse.class);

            if (apiResponse.getCode() != 200) {
                throw new RuntimeException("外部API调用失败，错误信息：" + apiResponse.getMsg());
            }

            return apiResponse;

        } catch (Exception e) {
            log.error("拉取外部岗位数据失败", e);
            throw new RuntimeException("拉取外部岗位数据失败：" + e.getMessage());
        }
    }

    /**
     * 保存外部岗位数据到同步表
     */
    private int saveExternalPosts(List<ExternalPostDTO> externalPosts) {
        int savedCount = 0;

        for (ExternalPostDTO externalPost : externalPosts) {
            try {
                // 检查是否已存在
                PostSyncDO existingSync = postSyncMapper.selectByExternalPostId(externalPost.getPostId());

                PostSyncDO postSyncDO;
                if (existingSync != null) {
                    // 更新现有记录
                    postSyncDO = existingSync;
                } else {
                    // 创建新记录
                    postSyncDO = new PostSyncDO();
                    postSyncDO.setSyncStatus(PostSyncDO.SyncStatus.PENDING.getValue());
                }

                // 设置外部系统数据
                postSyncDO.setExternalPostId(externalPost.getPostId());
                postSyncDO.setExternalPostCode(externalPost.getPostCode());
                postSyncDO.setExternalPostName(externalPost.getPostName());
                postSyncDO.setExternalPostSort(StrUtil.isNotBlank(externalPost.getPostSort()) 
                        ? Integer.parseInt(externalPost.getPostSort()) : 0);
                postSyncDO.setExternalStatus(StrUtil.isNotBlank(externalPost.getStatus()) 
                        ? Integer.parseInt(externalPost.getStatus()) : 0);
                postSyncDO.setExternalRemark(externalPost.getRemark());
                postSyncDO.setExternalCreateBy(externalPost.getCreateBy());
                postSyncDO.setExternalUpdateBy(externalPost.getUpdateBy());

                // 转换时间字符串
                if (StrUtil.isNotBlank(externalPost.getCreateTime())) {
                    postSyncDO.setExternalCreateTime(LocalDateTime.parse(externalPost.getCreateTime(), FORMATTER));
                }
                if (StrUtil.isNotBlank(externalPost.getUpdateTime())) {
                    postSyncDO.setExternalUpdateTime(LocalDateTime.parse(externalPost.getUpdateTime(), FORMATTER));
                }

                if (existingSync != null) {
                    postSyncMapper.updateById(postSyncDO);
                } else {
                    postSyncMapper.insert(postSyncDO);
                }

                savedCount++;

            } catch (Exception e) {
                log.error("保存外部岗位数据失败，岗位ID：{}", externalPost.getPostId(), e);
            }
        }

        return savedCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String syncPendingPosts() {
        try {
            log.info("开始执行待同步岗位数据同步...");
            
            // 获取所有待同步的记录
            List<PostSyncDO> pendingSyncs = postSyncMapper.selectPendingSync();
            
            if (pendingSyncs.isEmpty()) {
                log.info("没有待同步的岗位数据");
                return "没有待同步的岗位数据";
            }

            int successCount = 0;
            int failCount = 0;

            for (PostSyncDO postSync : pendingSyncs) {
                try {
                    // 检查本地是否已存在相同编码的岗位
                    PostDO existingPost = null;
                    if (postSync.getLocalPostId() != null) {
                        existingPost = postService.getPost(postSync.getLocalPostId());
                    }

                    PostSaveReqVO postSaveReqVO = new PostSaveReqVO();
                    postSaveReqVO.setCode(postSync.getExternalPostCode());
                    postSaveReqVO.setName(postSync.getExternalPostName());
                    postSaveReqVO.setSort(postSync.getExternalPostSort());
                    postSaveReqVO.setStatus(postSync.getExternalStatus());
                    postSaveReqVO.setRemark(postSync.getExternalRemark());

                    Long localPostId;
                    if (existingPost != null) {
                        // 更新现有岗位
                        postSaveReqVO.setId(existingPost.getId());
                        postService.updatePost(postSaveReqVO);
                        localPostId = existingPost.getId();
                    } else {
                        // 创建新岗位
                        localPostId = postService.createPost(postSaveReqVO);
                    }

                    // 更新同步状态
                    postSync.setLocalPostId(localPostId);
                    postSync.setSyncStatus(PostSyncDO.SyncStatus.SYNCED.getValue());
                    postSync.setSyncErrorMsg(null);
                    postSyncMapper.updateById(postSync);

                    successCount++;
                    log.debug("成功同步岗位：{}", postSync.getExternalPostName());

                } catch (Exception e) {
                    log.error("同步岗位失败，岗位名称：{}，错误：{}", postSync.getExternalPostName(), e.getMessage());

                    // 更新失败状态
                    try {
                        postSync.setSyncStatus(PostSyncDO.SyncStatus.FAILED.getValue());
                        postSync.setSyncErrorMsg(e.getMessage());
                        postSyncMapper.updateById(postSync);
                    } catch (Exception updateEx) {
                        log.error("更新同步失败状态失败", updateEx);
                    }

                    failCount++;
                }
            }

            String resultMsg = String.format("岗位同步完成！成功：%d条，失败：%d条", successCount, failCount);
            log.info(resultMsg);
            return resultMsg;
            
        } catch (Exception e) {
            log.error("执行岗位同步失败", e);
            throw new RuntimeException("执行岗位同步失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupSyncedRecords(int daysToKeep) {
        try {
            log.info("开始清理{}天前已同步的岗位记录...", daysToKeep);
            
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysToKeep);
            
            // 这里可以添加清理逻辑，例如删除N天前已同步的记录
            // 暂时保留所有记录，可以根据需要实现
            
            log.info("清理已同步的岗位记录完成");
            return 0;
            
        } catch (Exception e) {
            log.error("清理已同步记录失败", e);
            throw new RuntimeException("清理已同步记录失败：" + e.getMessage());
        }
    }

}