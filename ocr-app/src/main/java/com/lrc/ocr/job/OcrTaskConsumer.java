package com.lrc.ocr.job;

import com.lrc.ocr.domain.ocr.model.entity.OcrInputEntity;
import com.lrc.ocr.domain.ocr.model.entity.factory.OcrInputFactory;
import com.lrc.ocr.domain.ocr.service.FilterStrategyFactory;
import com.lrc.ocr.domain.ocr.service.impl.OcrService;
import com.lrc.ocr.enums.TaskStatus;
import com.lrc.ocr.po.OcrTask;
import com.lrc.ocr.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * OCR任务消费者
 */
@Component
@Slf4j
public class OcrTaskConsumer implements ApplicationRunner {

    @Resource
    private TaskService taskService;

    @Resource
    private FilterStrategyFactory filterStrategyFactory;

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 启动任务消费者线程
        executorService.submit(this::consumeTasks);
        log.info("OCR任务消费者启动成功");
    }

    private void consumeTasks() {
        while (true) {
            try {
                String taskId = taskService.getNextTask();
                if (taskId != null) {
                    // 处理任务
                    processTask(taskId);
                } else {
                    // 队列为空，短暂休眠
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                log.error("任务消费者异常", e);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * 处理OCR任务
     *
     * @param taskId 任务ID
     */
    private void processTask(String taskId) {
        executorService.submit(() -> {
            try {
                // 更新任务状态为处理中
                taskService.updateTaskStatus(taskId, TaskStatus.PROCESSING, null);

                // 获取任务信息
                OcrTask task = taskService.getTaskInfo(taskId);
                if (task == null) {
                    taskService.updateTaskStatus(taskId, TaskStatus.FAILED, "任务信息不存在");
                    return;
                }

                // 执行OCR处理
                String fileUrl = task.getFileUrl();
                
                // 所有任务都有有效的URL，统一处理
                OcrInputEntity inputEntity = OcrInputFactory.createFromUrl(fileUrl);
                
                // 执行OCR处理
                OcrService ocrService = filterStrategyFactory.createFilterStrategy("DEFAULT");
                ocrService.processOcrAndFilter(inputEntity, true, task.getUserId(), true);

                // 更新任务状态为成功
                taskService.updateTaskStatus(taskId, TaskStatus.SUCCESS, null);
            } catch (Exception e) {
                log.error("处理OCR任务失败: {}", taskId, e);
                taskService.updateTaskStatus(taskId, TaskStatus.FAILED, e.getMessage());
            }
        });
    }
}