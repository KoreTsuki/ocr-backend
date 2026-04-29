package com.lrc.ocr.dao;

import com.lrc.ocr.po.OcrResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OcrResultMapper {

    int deleteByPrimaryKey(Long id);

    int insert(OcrResult record);

    int insertSelective(OcrResult record);

    OcrResult selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(OcrResult record);

    int updateByPrimaryKey(OcrResult record);

    /**
     * 获取用户的全部OCR识别结果，排除已删除的
     * @param userId 用户ID
     * @return OCR识别结果列表
     */
    List<OcrResult> selectByUserId(@Param("userId") Long userId);

    OcrResult selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    int updateAuditInfo(OcrResult record);

    /**
     * 根据ID和用户ID删除OCR识别结果（逻辑删除）
     * @param id 识别结果ID
     * @param userId 用户ID
     * @return 删除的记录数
     */
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

}
