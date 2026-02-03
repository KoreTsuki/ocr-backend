package com.lrc.ocr.repository;

import com.lrc.ocr.dao.OcrResultMapper;
import com.lrc.ocr.dao.UserMapper;
import com.lrc.ocr.domain.ocr.model.entity.UserEntity;
import com.lrc.ocr.domain.ocr.repository.IOcrRepository;
import com.lrc.ocr.po.OcrResult;
import com.lrc.ocr.po.User;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;

@Repository
public class OcrRepository implements IOcrRepository {

    @Resource
    private UserMapper userMapper;
    
    @Resource
    private OcrResultMapper ocrResultMapper;

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
        ocrResult.setIsDelete(0); // 0表示未删除
        ocrResultMapper.insert(ocrResult);
    }

    @Override
    public List<OcrResult> getUserOcrResults(Long userId) {
        return ocrResultMapper.selectByUserId(userId);
    }

    @Override
    public boolean deleteOcrResult(Long id, Long userId) {
        return ocrResultMapper.deleteByIdAndUserId(id, userId) > 0;
    }
}
