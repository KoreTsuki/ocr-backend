package com.lrc.ocr.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 
 * @TableName ocr识别结果表
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class OcrResult implements Serializable {
    /**
     * 自增主键
     */
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 图片链接
     */
    private String imageUrl;

    /**
     * ocr识别的文字结果
     */
    private String textResult;

    /**
     * 人工审核后的文本
     */
    private String auditText;

    /**
     * 审核状态 0-待审核 1-审核通过 2-人工修补
     */
    private Integer auditStatus;

    /**
     * 审核人ID
     */
    private Long reviewerId;

    /**
     * 审核时间
     */
    private LocalDateTime auditTime;

    /**
     * 逻辑删除 1为已删除
     */
    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}
