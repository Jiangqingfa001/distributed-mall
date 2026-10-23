package com.mini.mall.gateway.controller;

import com.mini.rpc.spring.annotation.RpcReference;
import com.mini.mall.common.api.ProductService;
import com.mini.mall.common.entity.Product;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    @RpcReference
    private ProductService productService;

    @GetMapping("/{id}")
    public Product getById(@PathVariable Long id) {
        return productService.getById(id);
    }

    @GetMapping("/list")
    public List<Product> listAll() {
        return productService.listAll();
    }

    @GetMapping("/listOnSale")
    public List<Product> listOnSale() {
        return productService.listOnSale();
    }

    @PostMapping("/status")
    public Map<String, Object> setStatus(@RequestParam Long productId,
                                         @RequestParam String status) {
        boolean ok = productService.setStatus(productId, status);
        Map<String, Object> result = new HashMap<>();
        result.put("code", ok ? 0 : 1);
        result.put("message", ok ? "操作成功" : "商品不存在");
        return result;
    }

    @PostMapping("/add")
    public Map<String, Object> add(@RequestParam String name,
                                   @RequestParam BigDecimal price,
                                   @RequestParam Integer stock) {
        Map<String, Object> result = new HashMap<>();
        if (name == null || name.trim().isEmpty()) {
            result.put("code", 1);
            result.put("message", "商品名称不能为空");
            return result;
        }
        Product product = productService.addProduct(name, price, stock);
        result.put("code", 0);
        result.put("message", "新增成功");
        result.put("data", product);
        return result;
    }

    @PostMapping("/update")
    public Map<String, Object> update(@RequestParam Long productId,
                                      @RequestParam String name,
                                      @RequestParam BigDecimal price,
                                      @RequestParam Integer stock) {
        boolean ok = productService.updateProduct(productId, name, price, stock);
        Map<String, Object> result = new HashMap<>();
        result.put("code", ok ? 0 : 1);
        result.put("message", ok ? "修改成功" : "商品不存在");
        return result;
    }
}
