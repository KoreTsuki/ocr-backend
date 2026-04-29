package com.lrc.ocr.dao;

import com.lrc.ocr.po.SysOcrTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysOcrTaskMapper {

    int insert(SysOcrTask record);

    int updateByTaskId(SysOcrTask record);

    SysOcrTask selectByTaskId(@Param("taskId") String taskId);

    List<SysOcrTask> selectByUserId(@Param("userId") Long userId);
}
