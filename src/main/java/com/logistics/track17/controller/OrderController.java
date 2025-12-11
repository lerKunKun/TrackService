package com.logistics.track17.controller;

import com.logistics.track17.dto.OrderResponse;
import com.logistics.track17.dto.PageResult;
import com.logistics.track17.dto.Result;
import com.logistics.track17.dto.ShopResponse;
import com.logistics.track17.entity.Order;
import com.logistics.track17.service.OrderService;
import com.logistics.track17.service.ShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final ShopService shopService;

    public OrderController(OrderService orderService, ShopService shopService) {
        this.orderService = orderService;
        this.shopService = shopService;
    }

    /**
     * 获取订单列表
     */
    @GetMapping
    public Result<PageResult<OrderResponse>> getList(
            @RequestParam(required = false) Long shopId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        log.info("Getting order list: shopId={}, page={}, pageSize={}", shopId, page, pageSize);

        // 获取订单列表
        List<Order> orders = orderService.getOrderList(shopId, page, pageSize);
        Long total = orderService.countOrders(shopId);

        // 转换为DTO
        List<OrderResponse> orderResponses = orders.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        // 构建分页结果
        PageResult<OrderResponse> pageResult = new PageResult<>();
        pageResult.setList(orderResponses);
        pageResult.setTotal(total);
        pageResult.setPage(page);
        pageResult.setPageSize(pageSize);

        return Result.success("获取订单列表成功", pageResult);
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/{id}")
    public Result<OrderResponse> getDetail(@PathVariable Long id) {
        log.info("Getting order detail: {}", id);

        Order order = orderService.getById(id);
        if (order == null) {
            return Result.error(404, "订单不存在");
        }

        OrderResponse response = convertToResponse(order);
        return Result.success("获取订单详情成功", response);
    }

    /**
     * 转换为响应DTO
     */
    private OrderResponse convertToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        BeanUtils.copyProperties(order, response);

        // 获取店铺名称
        if (order.getShopId() != null) {
            ShopResponse shop = shopService.getById(order.getShopId());
            if (shop != null) {
                response.setShopName(shop.getShopName());
            }
        }

        return response;
    }
}
