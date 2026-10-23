package com.mini.mall.gateway.controller;

import com.mini.rpc.spring.annotation.RpcReference;
import com.mini.mall.common.api.OrderService;
import com.mini.mall.common.api.ProductService;
import com.mini.mall.common.api.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StatsController {

    @RpcReference
    private UserService userService;

    @RpcReference
    private ProductService productService;

    @RpcReference
    private OrderService orderService;

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> result = new HashMap<>();
        result.put("totalUsers", userService.listAll().size());
        result.put("totalProducts", productService.listAll().size());
        result.put("totalOrders", orderService.listAll().size());
        return result;
    }
}
