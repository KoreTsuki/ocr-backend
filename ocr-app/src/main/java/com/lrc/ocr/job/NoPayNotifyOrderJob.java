package com.lrc.ocr.job;

import com.google.common.eventbus.EventBus;
import com.lrc.ocr.domain.order.service.IOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.transaction.Transaction;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.List;

@Slf4j
@Component
public class NoPayNotifyOrderJob {

    @Resource
    private IOrderService orderService;

    @Resource
    private EventBus eventBus;



    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");

    @Scheduled(cron = "0 0/1 * * * ?")
    public void exec() {
        try {

            List<String> orderIds = orderService.queryNoPayNotifyOrder();
            if (orderIds.isEmpty()) {
                log.info("定时任务，订单支付状态更新，暂无未更新订单 orderId is null");
                return;
            }
            for (String orderId : orderIds) {
                // 查询结果

            }
        } catch (Exception e) {
            log.error("定时任务，订单支付状态更新失败", e);
        }
    }

}