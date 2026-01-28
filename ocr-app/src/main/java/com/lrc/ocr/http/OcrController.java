package com.lrc.ocr.http;

import com.lrc.ocr.domain.ocr.model.entity.OcrInputEntity;
import com.lrc.ocr.domain.ocr.model.entity.factory.OcrInputFactory;
import com.lrc.ocr.domain.ocr.repository.IOcrRepository;
import com.lrc.ocr.domain.ocr.service.FilterStrategyFactory;
import com.lrc.ocr.domain.ocr.service.IOcrService;
import com.lrc.ocr.handle.UserSentinelResourceHandler;
import com.lrc.ocr.model.Result;
import com.lrc.ocr.po.OcrResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = "ocr请求接口")
@RestController
@RequestMapping("/ocr")
public class OcrController {

    @Resource
    private FilterStrategyFactory filterStrategyFactory;
    @Resource
    private IOcrRepository ocrRepository;


    /**
     *
     * @param file 文件
     * @param isAggregate 聚合对象还是纯文本
     * @return List<聚合对象或纯文本>
     */
    @ApiOperation("上传文件获取结果")
    @PostMapping("/getByFile")
    public Result<List<?>> getTextOnlyByFile(@RequestPart("file") MultipartFile file, boolean isAggregate, String filterType){
        OcrInputEntity fromFile = OcrInputFactory.createFromFile(file);
        IOcrService ocrService = filterStrategyFactory.createFilterStrategy(filterType);
        List<?> list = ocrService.processOcrAndFilter(fromFile, isAggregate);
        return Result.success(list);
    }


    /**
     *
     * @param url 图片链接
     * @param isAggregate 聚合对象还是纯文本
     * @return List<聚合对象或纯文本>
     */
    @ApiOperation("通过url获取结果")
    @PostMapping("/getTotalByUrl")
    public Result<List<?>> getTotalByUrl(String url, boolean isAggregate, String filterType){
        OcrInputEntity fromUrl = OcrInputFactory.createFromUrl(url);
        IOcrService ocrService = filterStrategyFactory.createFilterStrategy(filterType);
        List<?> list = ocrService.processOcrAndFilter(fromUrl, isAggregate);
        return Result.success(list);
    }

    /**
     * 获取当前登录用户的全部OCR识别结果
     * @return List<OcrResult>
     */
    @ApiOperation("获取当前用户的全部识别结果")
    @GetMapping("/getUserResults")
    public Result<List<OcrResult>> getUserOcrResults(){
        // 获取当前登录用户的ID
        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = Long.parseLong(userIdStr);
        
        // 获取用户的OCR结果
        List<OcrResult> ocrResults = ocrRepository.getUserOcrResults(userId);
        return Result.success(ocrResults);
    }

    /**
     * 根据识别结果ID删除该条识别结果
     * @param id 识别结果ID
     * @return 删除是否成功
     */
    @ApiOperation("根据识别结果ID删除该条识别结果")
    @DeleteMapping("/deleteById/{id}")
    public Result<Boolean> deleteOcrResult(@PathVariable("id") Long id){
        // 获取当前登录用户的ID
        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = Long.parseLong(userIdStr);
        
        // 删除OCR结果
        boolean success = ocrRepository.deleteOcrResult(id, userId);
        
        return Result.success(success);
    }

}
