package com.lrc.ocr.domain.order.service.impl;

import com.lrc.ocr.domain.order.model.aggregates.CreateOrderAggregate;
import com.lrc.ocr.domain.order.model.entity.OrderEntity;
import com.lrc.ocr.domain.order.model.entity.PayOrderEntity;
import com.lrc.ocr.domain.order.model.entity.ProductEntity;
import com.lrc.ocr.domain.order.model.valobj.OrderStatusVO;
import com.lrc.ocr.domain.order.model.valobj.PayStatusVO;
import com.lrc.ocr.domain.order.repository.IOrderRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
public class OrderServiceImpl extends OrderService {

    @Resource
    private IOrderRepository orderRepository;

    @Override
    protected OrderEntity saveOrder(String userId, ProductEntity productEntity) {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderId(RandomStringUtils.randomNumeric(12));
        orderEntity.setOrderTime(new Date());
        orderEntity.setOrderStatus(OrderStatusVO.CREATE);
        orderEntity.setTotalAmount(productEntity.getPrice());

        CreateOrderAggregate createOrderAggregate = new CreateOrderAggregate().setOpenid(userId)
                .setOrder(orderEntity)
                .setProduct(productEntity);

        orderRepository.saveOrder(createOrderAggregate);
        return orderEntity;
    }

    @Override
    protected PayOrderEntity payOrder(String userId, String orderId, String productName, BigDecimal amountTotal) {
        // 移除微信支付相关代码，直接返回支付成功状态
        PayOrderEntity payOrderEntity = new PayOrderEntity()
                .setOpenid(userId)
                .setOrderId(orderId)
                .setPayUrl("")
                .setPayStatus(PayStatusVO.SUCCESS);

        // 更新订单支付信息
        orderRepository.updateOrderPayInfo(payOrderEntity);
        return payOrderEntity;
    }

    @Override
    public List<String> queryTimeoutCloseOrderList() {
        return orderRepository.queryTimeoutCloseOrderList();
    }

    @Override
    public boolean changeOrderClose(String orderId) {
        return orderRepository.changeOrderClose(orderId);
    }

    @Override
    public List<String> queryReplenishmentOrder() {
        return orderRepository.queryReplenishmentOrder();
    }

    @Override
    public List<String> queryNoPayNotifyOrder() {
        return orderRepository.queryNoPayNotifyOrder();
    }
}
