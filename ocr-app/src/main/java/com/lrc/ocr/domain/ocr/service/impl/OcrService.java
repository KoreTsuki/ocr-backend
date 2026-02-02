package com.lrc.ocr.domain.ocr.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lrc.ocr.domain.ocr.model.aggregate.ApiDataAggregate;
import com.lrc.ocr.domain.ocr.model.aggregate.ApiResponseAggregate;
import com.lrc.ocr.domain.ocr.model.entity.*;
import com.lrc.ocr.domain.ocr.model.exception.OcrServiceException;
import com.lrc.ocr.domain.ocr.model.valobj.OcrErrorVO;
import com.lrc.ocr.domain.ocr.repository.IOcrRepository;
import com.lrc.ocr.domain.ocr.service.IOcrService;
import com.lrc.ocr.domain.ocr.service.OcrStrategyFactory;
import com.lrc.ocr.domain.ocr.service.strategy.OcrStrategy;
import com.lrc.ocr.utils.FileDuplicateChecker;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public abstract class OcrService implements IOcrService {

    @Resource
    private OcrStrategyFactory ocrStrategyFactory;
    @Resource
    private IOcrRepository ocrRepository;
    @Resource
    private com.lrc.ocr.domain.ocr.service.FileUploadService fileUploadService;
    @Resource
    private FileDuplicateChecker fileDuplicateChecker;
    @Resource
    private RedissonClient redissonClient; // 引入 Redisson

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * OCR 处理主流程
     * 1. 分布式锁限流
     * 2. 检查重复
     * 3. 扣减额度 (DB操作)
     * 4. 执行OCR (耗时网络操作)
     * 5. 保存结果 (DB操作)
     */
    public List<?> processOcrAndFilter(OcrInputEntity input, boolean isAggregate) {
        // 1. 获取当前用户ID
        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = Long.parseLong(userIdStr);

        // 2. 定义分布式锁 Key (粒度：用户级)
        String lockKey = "lock:ocr:user:" + userId;
        RLock lock = redissonClient.getLock(lockKey);

        boolean isLocked = false;
        try {
            // 尝试获取锁：等待0秒(立即失败)，锁定30秒(防止死锁)
            // 如果用户狂点按钮，第二个请求会在这里直接被拒绝，不会打到数据库和OCR服务
            isLocked = lock.tryLock(0, 30, TimeUnit.SECONDS);

            if (!isLocked) {
                throw new OcrServiceException(OcrErrorVO.SYSTEM_ERROR.getCode(), "任务处理中，请勿频繁点击");
            }

            // === 业务逻辑开始 ===

            // A. 检查输入是否重复 (Redis)
            checkInputDuplicate(input);

            // B. 校验并扣除额度 (DB事务)
            // 注意：这里去掉了外层的 @Transactional，防止 OCR 耗时操作占用数据库连接
            deductUserQuota(userId);

            // C. 预处理文件上传 (如果需要) -> 提前获取 URL
            String storageUrl = prepareStorageUrl(input);

            // D. 执行 OCR 服务 (最耗时操作，不应在数据库事务中)
            ApiResponseAggregate apiResponseAggregate = doOcrService(input);
            List<ApiDataAggregate> apiDataAggregates = apiResponseAggregate.getData().get(0);

            // E. 保存 OCR 结果到数据库 (DB事务)
            saveOcrResultToDatabase(userId, input, storageUrl, apiResponseAggregate);

            // F. 返回结果
            if (isAggregate) {
                return apiDataAggregates;
            }
            List<String> textOnlyListByData = getTextOnlyListByData(apiDataAggregates);
            return filter(textOnlyListByData);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OcrServiceException(OcrErrorVO.SYSTEM_ERROR);
        } catch (OcrServiceException e) {
            throw e; // 业务异常直接抛出
        } catch (Exception e) {
            log.error("OCR process failed for user: {}", userId, e);
            throw new OcrServiceException(OcrErrorVO.SYSTEM_ERROR);
        } finally {
            // 3. 释放锁
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 检查输入是否重复
     */
    private void checkInputDuplicate(OcrInputEntity input) {
        if (input instanceof UrlOcrInputEntity) {
            String url = ((UrlOcrInputEntity) input).getUrl();
            if (fileDuplicateChecker.isUrlDuplicate(url)) {
                throw new OcrServiceException(OcrErrorVO.URL_ERROR.getCode(), "URL已提交，请不要重复提交");
            }
        }
        // 文件重复检查通常在 Controller 层或 FileUploadService 内部处理了
    }

    /**
     * 扣减用户额度 (独立事务)
     * 将事务范围缩小到仅此方法，避免长事务
     */
    @Transactional(rollbackFor = Exception.class)
    public void deductUserQuota(Long userId) {
        // 使用 select for update 或乐观锁更好，但有了 Redisson 外部锁，这里普通的 update 也安全
        UserEntity userEntity = ocrRepository.getById(userId);

        if (userEntity == null || userEntity.getLines() <= 0) {
            throw new OcrServiceException(OcrErrorVO.USER_LINE_ERROR);
        }

        // 扣减
        UserEntity updateEntity = userEntity.setLines(userEntity.getLines() - 1);
        ocrRepository.updateById(updateEntity);
    }

    /**
     * 准备存储 URL (上传文件或直接使用 URL)
     */
    private String prepareStorageUrl(OcrInputEntity input) {
        if (input instanceof UrlOcrInputEntity) {
            return ((UrlOcrInputEntity) input).getUrl();
        } else if (input instanceof FileOcrInputEntity) {
            MultipartFile file = ((FileOcrInputEntity) input).getFile();
            // 上传到 MinIO / OSS
            return fileUploadService.uploadToUrl(file);
        }
        return "";
    }

    /**
     * 保存结果 (独立事务)
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveOcrResultToDatabase(Long userId, OcrInputEntity input, String imageUrl, ApiResponseAggregate apiResponseAggregate) {
        try {
            boolean isPdf = false;

            // 判断是否为 PDF
            if (input instanceof UrlOcrInputEntity) {
                isPdf = imageUrl.toLowerCase().endsWith(".pdf");
            } else if (input instanceof FileOcrInputEntity) {
                MultipartFile file = ((FileOcrInputEntity) input).getFile();
                isPdf = file.getContentType() != null && file.getContentType().equals("application/pdf")
                        || file.getOriginalFilename() != null && file.getOriginalFilename().toLowerCase().endsWith(".pdf");
            }

            String textResult;
            if (isPdf) {
                // PDF 处理逻辑：合并所有页面的文本
                List<List<ApiDataAggregate>> datas = apiResponseAggregate.getData();
                StringBuilder mergedText = new StringBuilder();
                if (datas != null) {
                    for (List<ApiDataAggregate> data : datas) {
                        for (ApiDataAggregate aggregate : data) {
                            if (aggregate.getOcrText() != null) {
                                mergedText.append(aggregate.getOcrText().getText()).append("\n");
                            }
                        }
                    }
                }
                textResult = mergedText.toString().trim();
            } else {
                // 图片处理逻辑：JSON 序列化
                if (apiResponseAggregate.getData() != null && !apiResponseAggregate.getData().isEmpty()) {
                    List<ApiDataAggregate> apiDataAggregates = apiResponseAggregate.getData().get(0);
                    textResult = objectMapper.writeValueAsString(apiDataAggregates);
                } else {
                    textResult = "";
                }
            }

            ocrRepository.saveOcrResult(userId, imageUrl, textResult);
        } catch (Exception e) {
            // 记录日志，但不阻断流程（因为钱已经扣了，OCR也跑了，保存记录失败不应该抛给用户错误）
            log.error("Failed to save OCR history to DB. User: {}, Url: {}", userId, imageUrl, e);
        }
    }


    /**
     * 校验额度
     */
    private void checkLines(){
        // 校验当前额度
        String id = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserEntity userEntity = ocrRepository.getById(Long.parseLong(id));
        // 校验用户
        if (userEntity == null || userEntity.getLines() <= 0) {
            throw new OcrServiceException(OcrErrorVO.USER_LINE_ERROR);
        }
        UserEntity updateEntity = userEntity.setLines(userEntity.getLines() - 1);
        ocrRepository.updateById(updateEntity);
    }


    /**
     * 根据类型调用ocr服务处理
     * @param input
     * @return
     */
    private ApiResponseAggregate doOcrService(OcrInputEntity input){
        OcrStrategy strategy = ocrStrategyFactory.createStrategy(input);
        return strategy.process(input);
    }

    /**
     * 通过响应聚合数据仅获取文本
     * @param apiDataAggregates
     * @return
     */
    private List<String> getTextOnlyListByData(List<ApiDataAggregate> apiDataAggregates) {
        return apiDataAggregates.stream()
                .map(ApiDataAggregate::getOcrText) // 使用map操作来提取文本
                .map(OcrTextEntity::getText)
                .collect(Collectors.toList());
    }

    /**
     * 使用正则表达式进行过滤
     * @param texts
     * @return
     */

    protected abstract List<String> filter(List<String> texts);


}
