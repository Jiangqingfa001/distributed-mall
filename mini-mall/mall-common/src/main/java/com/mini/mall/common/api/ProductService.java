package com.mini.mall.common.api;

import com.mini.mall.common.entity.Product;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    Product getById(Long id);

    List<Product> listAll();

    List<Product> listOnSale();

    boolean setStatus(Long productId, String status);

    boolean deductStock(Long productId, int quantity);

    Product addProduct(String name, BigDecimal price, Integer stock);

    boolean updateProduct(Long productId, String name, BigDecimal price, Integer stock);
}