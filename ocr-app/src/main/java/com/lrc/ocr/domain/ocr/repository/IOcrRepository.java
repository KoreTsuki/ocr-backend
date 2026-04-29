package com.lrc.ocr.domain.ocr.repository;


import com.lrc.ocr.domain.ocr.model.entity.UserEntity;
import com.lrc.ocr.po.OcrAuditLog;
import com.lrc.ocr.po.OcrResult;

import java.util.List;

public interface IOcrRepository {
    UserEntity getById(Long id);

    void updateById(UserEntity updateEntity);
    
    /**
     * 保存OCR识别结果
     * @param userId 用户ID
     * @param imageUrl 图片链接
     * @param textResult 识别结果文本
     */
    void saveOcrResult(Long userId, String imageUrl, String textResult);
    
    /**
     * 获取用户的全部OCR识别结果
     * @param userId 用户ID
     * @return OCR识别结果列表
     */
    List<OcrResult> getUserOcrResults(Long userId);

    OcrResult getOcrResult(Long id, Long userId);

    boolean updateAuditResult(Long id, Long userId, Long reviewerId, String auditText, Integer auditStatus);

    void saveAuditLog(Long resultId, Long userId, Long reviewerId, String beforeText, String afterText);

    List<OcrAuditLog> getAuditLogs(Long resultId, Long userId);
    
    /**
     * 删除OCR识别结果
     * @param id 识别结果ID
     * @param userId 用户ID
     * @return 删除是否成功
     */
    boolean deleteOcrResult(Long id, Long userId);
}
