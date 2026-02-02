package com.lrc.ocr.domain.ocr.service;

import com.lrc.ocr.exception.ServiceException;
import com.lrc.ocr.utils.FileDuplicateChecker;
import com.lrc.ocr.utils.MinioUtil;
import lombok.extern.slf4j.Slf4j; // 引入 lombok 日志
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.UUID;

import static com.lrc.ocr.enums.BaseError.FILE_ERROR;


@Slf4j
@Service
public class FileUploadService {

    @Resource
    private MinioUtil minioUtil;

    @Resource
    private FileDuplicateChecker fileDuplicateChecker;

    /**
     * 上传文件转URL
     */
    public String uploadToUrl(MultipartFile file) {
        // 1. 先查 Redis 有没有现成的 URL
        String existingUrl = fileDuplicateChecker.getExistingFileUrl(file);
        if (existingUrl != null) {
            return existingUrl; // 有就直接返回，不再抛异常，也不再上传 MinIO
        }

        // 2. 没有则上传 MinIO
        String onlyFileName = getOnlyFileName(file);
        String url = minioUtil.uploadToMinio(file, onlyFileName);

        // 3. 上传成功后，把 URL 记账到 Redis
        fileDuplicateChecker.markFileAsUploaded(file, url);

        return url;
    }

    /**
     * 获取文件并生成唯一名称
     */
    private String getOnlyFileName(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (StringUtils.isBlank(filename)) {
            throw new ServiceException(FILE_ERROR);
        }

        String contentType = file.getContentType();
        // 4. 优化 ContentType 判断逻辑，防止空指针
        if (contentType == null ||
                (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
            // 有些上传可能拿不到 contentType，可以尝试通过后缀名辅助判断，或者直接拦截
            log.warn("不支持的文件类型: {}, filename: {}", contentType, filename);
            throw new ServiceException(FILE_ERROR);
        }

        // 5. 更安全地获取后缀名
        String extension = "";
        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex >= 0) {
            extension = filename.substring(dotIndex); // 包含点，例如 ".jpg"
        } else {
            throw new ServiceException(FILE_ERROR.getCode(), "文件缺少后缀名");
        }

        // 拼接成唯一名称: UUID + 后缀
        return UUID.randomUUID().toString() + extension;
    }
}