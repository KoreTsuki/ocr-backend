package com.lrc.ocr.dao;

import com.lrc.ocr.po.OcrAuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OcrAuditLogMapper {

    int insert(OcrAuditLog record);

    List<OcrAuditLog> selectByResultIdAndUserId(@Param("resultId") Long resultId, @Param("userId") Long userId);
}
