package com.lrc.ocr.po;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class OcrAuditLog implements Serializable {

    private Long id;
    private Long resultId;
    private Long userId;
    private Long reviewerId;
    private String beforeText;
    private String afterText;
    private LocalDateTime createTime;

    private static final long serialVersionUID = 1L;
}
