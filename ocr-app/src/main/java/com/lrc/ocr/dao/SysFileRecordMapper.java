package com.lrc.ocr.dao;

import com.lrc.ocr.po.SysFileRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysFileRecordMapper {

    SysFileRecord selectByFileHash(@Param("fileHash") String fileHash);

    int insert(SysFileRecord record);
}
