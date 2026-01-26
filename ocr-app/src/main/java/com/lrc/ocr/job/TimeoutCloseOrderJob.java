package com.lrc.ocr.job;

import com.lrc.ocr.domain.order.service.IOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 超时关单任务
 */
@Slf4j
@Component
public class TimeoutCloseOrderJob {

    @Resource
    private IOrderService orderService;


    @Scheduled(cron = "0 0/10 * * * ?")
    public void exec() {
        try {
            List<String> orderIds = orderService.queryTimeoutCloseOrderList();
            if (orderIds.isEmpty()) {
                log.info("定时任务，超时30分钟订单关闭，暂无超时未支付订单 orderIds is null");
                return;
            }
            for (String orderId : orderIds) {

            }
        } catch (Exception e) {
            log.error("定时任务，超时15分钟订单关闭失败", e);
        }
    }

}