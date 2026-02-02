package com.lrc.ocr.utils;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

@Service
public class FileDuplicateChecker {

    @Resource
    private RedissonClient redissonClient;

    private static final String FILE_HASH_PREFIX = "file:hash:";

    private static final long EXPIRE_TIME = 12 * 60 * 60 * 1000;

    /**
     * 【核心修改】检查文件是否存在
     * * @param file 待检查的文件
     * @return 如果存在，返回已存在的 URL (String)；如果不存在，返回 null。
     */
    public String getExistingFileUrl(MultipartFile file) {
        try {
            String fileHash = generateFileHash(file);
            String redisKey = FILE_HASH_PREFIX + fileHash;

            // 获取 Bucket，注意泛型是 String，因为我们要存 URL
            RBucket<String> bucket = redissonClient.getBucket(redisKey);

            if (bucket.isExists()) {
                // 如果 Redis 里有，直接把存好的 URL 拿出来返回
                return bucket.get();
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 【核心修改】标记文件已上传，并记录 URL
     * 上传成功后调用此方法，把 URL 存进 Redis
     */
    public void markFileAsUploaded(MultipartFile file, String url) {
        try {
            String fileHash = generateFileHash(file);
            String redisKey = FILE_HASH_PREFIX + fileHash;

            // 将 URL 存入 Redis，并设置过期时间
            redissonClient.getBucket(redisKey).set(url, EXPIRE_TIME, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 检查 URL 是否重复（纯文本 URL 的防抖逻辑）
     * 保持原有逻辑：存在返回 true，不存在则占位并返回 false
     */
    public boolean isUrlDuplicate(String url) {
        try {
            String urlHash = generateStringHash(url);
            String redisKey = FILE_HASH_PREFIX + urlHash;

            RBucket<Boolean> bucket = redissonClient.getBucket(redisKey);

            if (bucket.isExists()) {
                return true;
            }

            // 存入 true 占位
            bucket.set(true, EXPIRE_TIME, TimeUnit.MILLISECONDS);
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ==========================================
    // 以下是哈希生成辅助方法
    // ==========================================

    private String generateFileHash(MultipartFile file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        try (InputStream inputStream = file.getInputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return bytesToHex(digest.digest());
    }

    private String generateStringHash(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.update(input.getBytes());
        return bytesToHex(digest.digest());
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}