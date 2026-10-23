package com.mini.mall.common.api;

import com.mini.mall.common.entity.Order;

import java.util.List;

public interface OrderService {

    Order createOrder(Long userId, Long productId, int quantity, String address);

    Order getById(Long id);

    List<Order> listByUserId(Long userId);

    List<Order> listAll();

    boolean shipOrder(Long orderId);
}