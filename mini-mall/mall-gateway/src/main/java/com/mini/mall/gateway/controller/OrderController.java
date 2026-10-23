package com.mini.mall.gateway.controller;

import com.mini.rpc.spring.annotation.RpcReference;
import com.mini.mall.common.api.OrderService;
import com.mini.mall.common.entity.Order;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @RpcReference
    private OrderService orderService;

    @PostMapping("/create")
    public Order create(@RequestParam Long userId,
                        @RequestParam Long productId,
                        @RequestParam int quantity,
                        @RequestParam String address) {
        return orderService.createOrder(userId, productId, quantity, address);
    }

    @GetMapping("/{id}")
    public Order getById(@PathVariable Long id) {
        return orderService.getById(id);
    }

    @GetMapping("/user/{userId}")
    public List<Order> listByUserId(@PathVariable Long userId) {
        return orderService.listByUserId(userId);
    }

    @GetMapping("/listAll")
    public List<Order> listAll() {
        return orderService.listAll();
    }

    @PostMapping("/ship")
    public Map<String, Object> ship(@RequestParam Long orderId) {
        boolean ok = orderService.shipOrder(orderId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", ok ? 0 : 1);
        result.put("message", ok ? "发货成功" : "订单不存在");
        return result;
    }
}
