package com.lrc.ocr.http;

import com.lrc.ocr.domain.order.model.entity.ProductEntity;
import com.lrc.ocr.domain.order.service.IOrderService;
import com.lrc.ocr.model.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/product")
@Api(tags = "订单相关接口")
@Slf4j
public class ProductController {

    @Resource
    private IOrderService orderService;

    /**
     * 展示商品
     * @return
     */
    @ApiOperation("展示商品")
    @PostMapping("/list/products")
    public Result<List<ProductEntity>> listProduct(){
        List<ProductEntity> productEntities = orderService.listProduct();
        log.info("展示商品：{}", productEntities);
        return Result.success(productEntities);
    }

    /**
     * 创建订单
     * @param productId
     * @return
     */
    @ApiOperation("创建订单")
    @PostMapping("/createOrder")
    public Result<String> createOrder(Long productId){
        String result = orderService.createOrder(productId);
        return Result.success(result);
    }

}
