package com.lrc.ocr.http;

import com.lrc.ocr.domain.ocr.model.entity.OcrInputEntity;
import com.lrc.ocr.domain.ocr.model.entity.factory.OcrInputFactory;
import com.lrc.ocr.domain.ocr.repository.IOcrRepository;
import com.lrc.ocr.domain.ocr.service.FilterStrategyFactory;
import com.lrc.ocr.domain.ocr.service.IOcrService;
import com.lrc.ocr.handle.UserSentinelResourceHandler;
import com.lrc.ocr.model.Result;
import com.lrc.ocr.po.OcrResult;
import com.lrc.ocr.po.OcrTask;
import com.lrc.ocr.service.TaskService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "ocr请求接口")
@RestController
@RequestMapping("/ocr")
public class OcrController {

    @Resource
    private IOcrRepository ocrRepository;
    @Resource
    private TaskService taskService;
    @Resource
    private com.lrc.ocr.domain.ocr.service.FileUploadService fileUploadService;

    /**
     * 上传单个文件获取任务ID
     * @param file 文件
     * @return 任务ID
     */
    @ApiOperation("上传文件获取任务ID")
    @PostMapping("/uploadFile")
    public Result<Map<String, Object>> uploadFile(@RequestPart("file") MultipartFile file){
        try {
            // 获取当前用户ID
            Long userId = null;
            String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            userId = Long.parseLong(userIdStr);

            // 上传文件到MinIO获取URL
            String fileUrl = fileUploadService.uploadToUrl(file);

            // 创建任务
            String taskId = taskService.createTask(userId, file.getOriginalFilename(), fileUrl);

            Map<String, Object> result = new HashMap<>();
            result.put("taskId", taskId);
            result.put("fileName", file.getOriginalFilename());
            result.put("queuePosition", taskService.getQueueLength());

            return Result.success(result);
        } catch (Exception e) {
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传多个文件获取任务ID列表
     * @param files 文件列表
     * @return 任务ID列表
     */
    @ApiOperation("批量上传文件获取任务ID列表")
    @PostMapping("/uploadFiles")
    public Result<List<Map<String, Object>>> uploadFiles(@RequestPart("files") MultipartFile[] files){
        List<Map<String, Object>> results = new ArrayList<>();

        // 获取当前用户ID
        Long userId = null;
        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        userId = Long.parseLong(userIdStr);

        for (MultipartFile file : files) {
            try {
                // 上传文件到MinIO获取URL
                String fileUrl = fileUploadService.uploadToUrl(file);

                // 创建任务
                String taskId = taskService.createTask(userId, file.getOriginalFilename(), fileUrl);

                Map<String, Object> result = new HashMap<>();
                result.put("taskId", taskId);
                result.put("fileName", file.getOriginalFilename());
                result.put("queuePosition", taskService.getQueueLength());

                results.add(result);
            } catch (Exception e) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("fileName", file.getOriginalFilename());
                errorResult.put("error", "文件上传失败: " + e.getMessage());
                results.add(errorResult);
            }
        }

        return Result.success(results);
    }

    /**
     * 查询任务状态
     * @param taskId 任务ID
     * @return 任务信息
     */
    @ApiOperation("查询任务状态")
    @GetMapping("/task/status/{taskId}")
    public Result<OcrTask> getTaskStatus(@PathVariable("taskId") String taskId){
        OcrTask task = taskService.getTaskInfo(taskId);
        if (task == null) {
            return Result.error("任务不存在");
        }
        return Result.success(task);
    }

    /**
     * 通过URL创建OCR任务
     * @param url 图片链接
     * @return 任务ID
     */
    @ApiOperation("通过URL创建OCR任务")
    @PostMapping("/createTaskByUrl")
    public Result<Map<String, Object>> createTaskByUrl(String url){
        try {
            // 获取当前用户ID
            Long userId = null;
            String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            userId = Long.parseLong(userIdStr);

            // 验证URL
            OcrInputEntity fromUrl = OcrInputFactory.createFromUrl(url);

            // 创建任务
            String taskId = taskService.createTask(userId, "url_image", url);

            Map<String, Object> result = new HashMap<>();
            result.put("taskId", taskId);
            result.put("fileName", "url_image");
            result.put("queuePosition", taskService.getQueueLength());

            return Result.success(result);
        } catch (Exception e) {
            return Result.error("创建任务失败: " + e.getMessage());
        }
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
