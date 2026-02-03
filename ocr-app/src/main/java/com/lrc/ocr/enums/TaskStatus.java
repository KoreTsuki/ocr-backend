package com.lrc.ocr.enums;

import lombok.Getter;

/**
 * 任务状态枚举
 */
@Getter
public enum TaskStatus {
    WAITING("等待中"),
    PROCESSING("处理中"),
    SUCCESS("成功"),
    FAILED("失败");

    private final String desc;

    TaskStatus(String desc) {
        this.desc = desc;
    }

}