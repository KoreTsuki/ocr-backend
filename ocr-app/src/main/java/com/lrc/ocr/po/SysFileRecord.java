package com.lrc.ocr.po;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class SysFileRecord implements Serializable {

    private Long id;
    private String fileHash;
    private String fileName;
    private String minioUrl;
    private Long fileSize;
    private String contentType;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}
