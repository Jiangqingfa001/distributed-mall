package com.mini.mall.product;

import com.mini.rpc.spring.annotation.RpcService;
import com.mini.mall.common.api.ProductService;
import com.mini.mall.common.entity.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@RpcService
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final RowMapper<Product> PRODUCT_ROW_MAPPER = (rs, rowNum) -> {
        Product p = new Product();
        p.setId(rs.getLong("id"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        p.setPrice(rs.getBigDecimal("price"));
        p.setStock(rs.getInt("stock"));
        p.setStatus(rs.getString("status"));
        return p;
    };

    @PostConstruct
    public void init() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_product (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "name VARCHAR(128)," +
            "description VARCHAR(512)," +
            "price DECIMAL(10,2)," +
            "stock INT," +
            "status VARCHAR(20)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_product", Integer.class);
        if (count != null && count == 0) {
            jdbcTemplate.update("INSERT INTO t_product (id, name, price, stock, status) VALUES (?,?,?,?,?)",
                1L, "iPhone 15", new BigDecimal("5999.00"), 50, "ON_SALE");
            jdbcTemplate.update("INSERT INTO t_product (id, name, price, stock, status) VALUES (?,?,?,?,?)",
                2L, "MacBook Pro", new BigDecimal("12999.00"), 30, "ON_SALE");
            jdbcTemplate.update("INSERT INTO t_product (id, name, price, stock, status) VALUES (?,?,?,?,?)",
                3L, "AirPods Pro", new BigDecimal("1999.00"), 100, "ON_SALE");
            log.info("初始化 3 个默认商品");
        }
    }

    @Override
    public Product getById(Long id) {
        log.info("查询商品: id={}", id);
        List<Product> list = jdbcTemplate.query("SELECT * FROM t_product WHERE id=?", PRODUCT_ROW_MAPPER, id);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<Product> listAll() {
        log.info("查询所有商品");
        return jdbcTemplate.query("SELECT * FROM t_product", PRODUCT_ROW_MAPPER);
    }

    @Override
    public List<Product> listOnSale() {
        log.info("查询上架商品");
        return jdbcTemplate.query("SELECT * FROM t_product WHERE status IS NULL OR status <> 'OFF_SALE'",
            PRODUCT_ROW_MAPPER);
    }

    @Override
    public boolean setStatus(Long productId, String status) {
        int rows = jdbcTemplate.update("UPDATE t_product SET status=? WHERE id=?", status, productId);
        if (rows > 0) {
            log.info("商品状态更新: productId={}, status={}", productId, status);
        }
        return rows > 0;
    }

    @Override
    public boolean deductStock(Long productId, int quantity) {
        log.info("扣减库存: productId={}, quantity={}", productId, quantity);
        int rows = jdbcTemplate.update(
            "UPDATE t_product SET stock = stock - ? WHERE id=? AND stock >= ?", quantity, productId, quantity);
        if (rows > 0) {
            return true;
        }
        log.warn("库存不足: productId={}, 需要={}", productId, quantity);
        return false;
    }

    @Override
    public Product addProduct(String name, BigDecimal price, Integer stock) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO t_product (name, description, price, stock, status) VALUES (?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name.trim());
            ps.setString(2, null);
            ps.setBigDecimal(3, price);
            ps.setInt(4, stock);
            ps.setString(5, "ON_SALE");
            return ps;
        }, keyHolder);
        Product product = new Product(keyHolder.getKey().longValue(), name.trim(), price, stock);
        product.setStatus("ON_SALE");
        log.info("新增商品: id={}, name={}, price={}, stock={}", product.getId(), name, price, stock);
        return product;
    }

    @Override
    public boolean updateProduct(Long productId, String name, BigDecimal price, Integer stock) {
        int rows = jdbcTemplate.update("UPDATE t_product SET name=?, price=?, stock=? WHERE id=?",
            name.trim(), price, stock, productId);
        if (rows > 0) {
            log.info("修改商品: productId={}, name={}, price={}, stock={}", productId, name, price, stock);
        }
        return rows > 0;
    }
}
