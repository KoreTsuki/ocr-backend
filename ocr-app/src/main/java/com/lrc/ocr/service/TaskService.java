package com.lrc.ocr.service;

import com.lrc.ocr.constants.TaskQueueConstants;
import com.lrc.ocr.dao.SysOcrTaskMapper;
import com.lrc.ocr.enums.TaskStatus;
import com.lrc.ocr.po.OcrTask;
import com.lrc.ocr.po.SysOcrTask;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RBucket;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * 任务服务
 */
@Service
@Slf4j
public class TaskService {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private SysOcrTaskMapper sysOcrTaskMapper;

    public String createTask(String fileName, String fileUrl) {
        return createTask(null, fileName, fileUrl);
    }

    /**
     * 创建OCR任务
     * @param userId 用户ID
     * @param fileName 文件名
     * @param fileUrl 文件URL
     * @return 任务ID
     */
    public String createTask(Long userId, String fileName, String fileUrl) {
        String taskId = UUID.randomUUID().toString();
        OcrTask task = new OcrTask();
        task.setTaskId(taskId);
        task.setUserId(userId);
        task.setFileName(fileName);
        task.setFileUrl(fileUrl);
        task.setStatus(TaskStatus.WAITING.name());
        task.setCreateTime(LocalDateTime.now());

        // 保存任务信息
        RBucket<OcrTask> bucket = redissonClient.getBucket(String.format(TaskQueueConstants.OCR_TASK_INFO, taskId));
        bucket.set(task);

        // 加入任务队列
        RBlockingQueue<String> queue = redissonClient.getBlockingQueue(TaskQueueConstants.OCR_TASK_QUEUE);
        queue.offer(taskId);

        // 更新队列长度
        int queueLength = queue.size();
        task.setQueuePosition(queueLength);
        bucket.set(task);
        sysOcrTaskMapper.insert(toSysOcrTask(task));

        log.info("创建OCR任务: {}", taskId);
        return taskId;
    }

    /**
     * 获取任务信息
     * @param taskId 任务ID
     * @return 任务信息
     */
    public OcrTask getTaskInfo(String taskId) {
        RBucket<OcrTask> bucket = redissonClient.getBucket(String.format(TaskQueueConstants.OCR_TASK_INFO, taskId));
        OcrTask task = bucket.get();
        if (task != null) {
            return task;
        }
        SysOcrTask sysTask = sysOcrTaskMapper.selectByTaskId(taskId);
        return sysTask == null ? null : toOcrTask(sysTask);
    }

    /**
     * 更新任务状态
     * @param taskId 任务ID
     * @param status 任务状态
     * @param errorMessage 错误信息
     */
    public void updateTaskStatus(String taskId, TaskStatus status, String errorMessage) {
        updateTaskStatus(taskId, status, errorMessage, null);
    }

    public void updateTaskStatus(String taskId, TaskStatus status, String errorMessage, String consumerId) {
        RBucket<OcrTask> bucket = redissonClient.getBucket(String.format(TaskQueueConstants.OCR_TASK_INFO, taskId));
        OcrTask task = bucket.get();
        if (task == null) {
            SysOcrTask sysTask = sysOcrTaskMapper.selectByTaskId(taskId);
            task = sysTask == null ? null : toOcrTask(sysTask);
        }
        if (task != null) {
            task.setStatus(status.name());
            task.setErrorMessage(errorMessage);
            task.setUpdateTime(LocalDateTime.now());
            if (consumerId != null) {
                task.setConsumerId(consumerId);
            }

            if (status == TaskStatus.PROCESSING) {
                task.setStartTime(LocalDateTime.now());
            } else if (status == TaskStatus.SUCCESS || status == TaskStatus.FAILED) {
                task.setCompleteTime(LocalDateTime.now());
                if (task.getStartTime() != null) {
                    task.setExecuteDurationMs(ChronoUnit.MILLIS.between(task.getStartTime(), task.getCompleteTime()));
                }
            }

            bucket.set(task);
            sysOcrTaskMapper.updateByTaskId(toSysOcrTask(task));

            // 发布状态更新通知
            publishTaskStatusUpdate(task);

            log.info("更新任务状态: {} -> {}", taskId, status.name());
        }
    }

    /**
     * 获取下一个任务
     * @return 任务ID
     */
    public String getNextTask() {
        RBlockingQueue<String> queue = redissonClient.getBlockingQueue(TaskQueueConstants.OCR_TASK_QUEUE);
        try {
            return queue.poll();
        } catch (Exception e) {
            log.error("获取队列任务失败", e);
            return null;
        }
    }

    /**
     * 获取队列长度
     * @return 队列长度
     */
    public int getQueueLength() {
        RBlockingQueue<String> queue = redissonClient.getBlockingQueue(TaskQueueConstants.OCR_TASK_QUEUE);
        return queue.size();
    }

    public List<SysOcrTask> getUserTaskList(Long userId) {
        return sysOcrTaskMapper.selectByUserId(userId);
    }

    public String buildConsumerId() {
        try {
            return InetAddress.getLocalHost().getHostName() + ":" + Thread.currentThread().getName();
        } catch (Exception e) {
            return "consumer:" + Thread.currentThread().getName();
        }
    }

    /**
     * 发布任务状态更新通知
     * @param task 任务信息
     */
    private void publishTaskStatusUpdate(OcrTask task) {
        RTopic topic = redissonClient.getTopic(TaskQueueConstants.OCR_TASK_STATUS_CHANNEL);
        topic.publish(task);
    }

    private SysOcrTask toSysOcrTask(OcrTask task) {
        return new SysOcrTask()
                .setTaskId(task.getTaskId())
                .setUserId(task.getUserId())
                .setFileName(task.getFileName())
                .setFileUrl(task.getFileUrl())
                .setStatus(task.getStatus())
                .setErrorMessage(task.getErrorMessage())
                .setCreateTime(task.getCreateTime())
                .setUpdateTime(task.getUpdateTime())
                .setStartTime(task.getStartTime())
                .setCompleteTime(task.getCompleteTime())
                .setQueuePosition(task.getQueuePosition())
                .setConsumerId(task.getConsumerId())
                .setExecuteDurationMs(task.getExecuteDurationMs());
    }

    private OcrTask toOcrTask(SysOcrTask task) {
        OcrTask ocrTask = new OcrTask();
        ocrTask.setId(task.getId());
        ocrTask.setTaskId(task.getTaskId());
        ocrTask.setUserId(task.getUserId());
        ocrTask.setFileName(task.getFileName());
        ocrTask.setFileUrl(task.getFileUrl());
        ocrTask.setStatus(task.getStatus());
        ocrTask.setErrorMessage(task.getErrorMessage());
        ocrTask.setCreateTime(task.getCreateTime());
        ocrTask.setUpdateTime(task.getUpdateTime());
        ocrTask.setStartTime(task.getStartTime());
        ocrTask.setCompleteTime(task.getCompleteTime());
        ocrTask.setQueuePosition(task.getQueuePosition());
        ocrTask.setConsumerId(task.getConsumerId());
        ocrTask.setExecuteDurationMs(task.getExecuteDurationMs());
        return ocrTask;
    }
}
