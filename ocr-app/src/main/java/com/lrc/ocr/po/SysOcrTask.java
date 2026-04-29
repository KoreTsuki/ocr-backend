package com.lrc.ocr.po;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class SysOcrTask implements Serializable {

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
    private String consumerId;
    private Long executeDurationMs;

    private static final long serialVersionUID = 1L;
}
