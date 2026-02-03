package com.lrc.ocr.po;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * ocr任务
 */
@Data
public class OcrTask {
    private Long id;
    private String taskId;
    private Long userId;
    private String fileName;
    private String fileUrl;
    private String status;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime startTime;
    private LocalDateTime completeTime;
    private Integer queuePosition;
}