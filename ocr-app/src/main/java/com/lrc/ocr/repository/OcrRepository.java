package com.lrc.ocr.repository;

import com.lrc.ocr.dao.OcrResultMapper;
import com.lrc.ocr.dao.OcrAuditLogMapper;
import com.lrc.ocr.dao.UserMapper;
import com.lrc.ocr.domain.ocr.model.entity.UserEntity;
import com.lrc.ocr.domain.ocr.repository.IOcrRepository;
import com.lrc.ocr.po.OcrAuditLog;
import com.lrc.ocr.po.OcrResult;
import com.lrc.ocr.po.User;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class OcrRepository implements IOcrRepository {

    @Resource
    private UserMapper userMapper;
    
    @Resource
    private OcrResultMapper ocrResultMapper;

    @Resource
    private OcrAuditLogMapper ocrAuditLogMapper;

    @Override
    public UserEntity getById(Long id) {
        User user = userMapper.getById(id);
        if (user == null) {
            return null;
        }
        return new UserEntity(user.getId(), user.getOpenid(), user.getLines());
    }

    @Override
    public void updateById(UserEntity updateEntity) {
        User user = new User();
        user.setId(updateEntity.getId());
        user.setOpenid(updateEntity.getOpenid());
        user.setLines(updateEntity.getLines());
        userMapper.updateById(user);
    }

    @Override
    public void saveOcrResult(Long userId, String imageUrl, String textResult) {
        OcrResult ocrResult = new OcrResult();
        ocrResult.setUserId(userId);
        ocrResult.setImageUrl(imageUrl);
        ocrResult.setTextResult(textResult);
        ocrResult.setAuditStatus(0);
        ocrResult.setIsDelete(0); // 0表示未删除
        ocrResultMapper.insert(ocrResult);
    }

    @Override
    public List<OcrResult> getUserOcrResults(Long userId) {
        return ocrResultMapper.selectByUserId(userId);
    }

    @Override
    public OcrResult getOcrResult(Long id, Long userId) {
        return ocrResultMapper.selectByIdAndUserId(id, userId);
    }

    @Override
    public boolean updateAuditResult(Long id, Long userId, Long reviewerId, String auditText, Integer auditStatus) {
        OcrResult updateRecord = new OcrResult()
                .setId(id)
                .setUserId(userId)
                .setReviewerId(reviewerId)
                .setAuditText(auditText)
                .setAuditStatus(auditStatus)
                .setAuditTime(LocalDateTime.now());
        return ocrResultMapper.updateAuditInfo(updateRecord) > 0;
    }

    @Override
    public void saveAuditLog(Long resultId, Long userId, Long reviewerId, String beforeText, String afterText) {
        OcrAuditLog auditLog = new OcrAuditLog()
                .setResultId(resultId)
                .setUserId(userId)
                .setReviewerId(reviewerId)
                .setBeforeText(beforeText)
                .setAfterText(afterText)
                .setCreateTime(LocalDateTime.now());
        ocrAuditLogMapper.insert(auditLog);
    }

    @Override
    public List<OcrAuditLog> getAuditLogs(Long resultId, Long userId) {
        return ocrAuditLogMapper.selectByResultIdAndUserId(resultId, userId);
    }

    @Override
    public boolean deleteOcrResult(Long id, Long userId) {
        return ocrResultMapper.deleteByIdAndUserId(id, userId) > 0;
    }
}
