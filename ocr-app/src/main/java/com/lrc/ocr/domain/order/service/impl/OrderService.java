package com.lrc.ocr.domain.order.service.impl;

import com.lrc.ocr.domain.order.model.entity.OrderEntity;
import com.lrc.ocr.domain.order.model.entity.PayOrderEntity;
import com.lrc.ocr.domain.order.model.entity.ProductEntity;
import com.lrc.ocr.domain.order.model.entity.UnpaidOrderEntity;
import com.lrc.ocr.domain.order.model.valobj.PayStatusVO;
import com.lrc.ocr.domain.order.repository.IOrderRepository;
import com.lrc.ocr.domain.order.service.IOrderService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
public abstract class OrderService implements IOrderService {
    @Resource
    private IOrderRepository orderRepository;
    @Override
    public void deliverGoods(String orderId) {
        orderRepository.deliverGoods(orderId);
    }

    @Override
    public boolean changeOrderPaySuccess(String orderId, String transactionId, BigDecimal divide, Date date) {
        return orderRepository.changeOrderPaySuccess(orderId, transactionId, divide,date);
    }

    @Override
    public List<ProductEntity> listProduct() {
        return orderRepository.queryProduct();
    }

    @Override
    public String createOrder(Long productId) {
        // 获取用户ID
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        // 查询未支付的订单
        UnpaidOrderEntity unpaidOrderEntity = orderRepository.queryUnpaidOrder(userId, productId);
        if (unpaidOrderEntity != null) {
            // 创建订单
            PayOrderEntity payOrderEntity = payOrder(userId, unpaidOrderEntity.getOrderId(), unpaidOrderEntity.getProductName(), unpaidOrderEntity.getTotalAmount());
            return "支付成功";
        }

        // 查询商品
        ProductEntity productEntity = orderRepository.queryProductByProductId(productId);

        // 保存订单
        OrderEntity orderEntity = saveOrder(userId, productEntity);
        // 提交订单
        PayOrderEntity payOrderEntity = payOrder(userId, orderEntity.getOrderId(), productEntity.getProductName(), orderEntity.getTotalAmount());

        return "支付成功";
    }

    protected abstract OrderEntity saveOrder(String userId, ProductEntity productEntity);

    protected abstract PayOrderEntity payOrder(String userId, String orderId, String productName, BigDecimal amountTotal);
}
