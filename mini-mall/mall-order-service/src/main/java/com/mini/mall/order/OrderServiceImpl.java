package com.mini.mall.order;

import com.mini.rpc.spring.annotation.RpcReference;
import com.mini.rpc.spring.annotation.RpcService;
import com.mini.mall.common.api.OrderService;
import com.mini.mall.common.api.ProductService;
import com.mini.mall.common.api.UserService;
import com.mini.mall.common.entity.Order;
import com.mini.mall.common.entity.Product;
import com.mini.mall.common.entity.User;
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
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

@RpcService
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    @RpcReference
    private UserService userService;

    @RpcReference
    private ProductService productService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final RowMapper<Order> ORDER_ROW_MAPPER = (rs, rowNum) -> {
        Order o = new Order();
        o.setId(rs.getLong("id"));
        o.setUserId(rs.getLong("user_id"));
        o.setProductId(rs.getLong("product_id"));
        o.setQuantity(rs.getInt("quantity"));
        o.setTotalPrice(rs.getBigDecimal("total_price"));
        o.setStatus(rs.getString("status"));
        o.setCreateTime(rs.getTimestamp("create_time"));
        o.setAddress(rs.getString("address"));
        o.setShipTime(rs.getTimestamp("ship_time"));
        return o;
    };

    @PostConstruct
    public void init() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_order (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "user_id BIGINT," +
            "product_id BIGINT," +
            "quantity INT," +
            "total_price DECIMAL(10,2)," +
            "status VARCHAR(20)," +
            "create_time DATETIME," +
            "address VARCHAR(512)," +
            "ship_time DATETIME" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    @Override
    public Order createOrder(Long userId, Long productId, int quantity, String address) {
        log.info("创建订单: userId={}, productId={}, quantity={}, address={}", userId, productId, quantity, address);

        if (address == null || address.trim().isEmpty()) {
            throw new RuntimeException("收货地址不能为空");
        }

        User user = userService.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在: " + userId);
        }

        Product product = productService.getById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在: " + productId);
        }

        boolean success = productService.deductStock(productId, quantity);
        if (!success) {
            throw new RuntimeException("库存不足");
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setTotalPrice(product.getPrice().multiply(new BigDecimal(quantity)));
        order.setAddress(address.trim());
        order.setStatus("已创建");
        order.setCreateTime(new Date());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO t_order (user_id, product_id, quantity, total_price, status, create_time, address) VALUES (?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, userId);
            ps.setLong(2, productId);
            ps.setInt(3, quantity);
            ps.setBigDecimal(4, order.getTotalPrice());
            ps.setString(5, order.getStatus());
            ps.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
            ps.setString(7, order.getAddress());
            return ps;
        }, keyHolder);
        order.setId(keyHolder.getKey().longValue());

        log.info("订单创建成功: orderId={}, 总价={}", order.getId(), order.getTotalPrice());
        return order;
    }

    @Override
    public Order getById(Long id) {
        log.info("查询订单: id={}", id);
        List<Order> list = jdbcTemplate.query("SELECT * FROM t_order WHERE id=?", ORDER_ROW_MAPPER, id);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<Order> listByUserId(Long userId) {
        log.info("查询用户订单: userId={}", userId);
        return jdbcTemplate.query("SELECT * FROM t_order WHERE user_id=? ORDER BY id DESC", ORDER_ROW_MAPPER, userId);
    }

    @Override
    public List<Order> listAll() {
        log.info("查询所有订单");
        return jdbcTemplate.query("SELECT * FROM t_order ORDER BY id DESC", ORDER_ROW_MAPPER);
    }

    @Override
    public boolean shipOrder(Long orderId) {
        log.info("发货: orderId={}", orderId);
        int rows = jdbcTemplate.update("UPDATE t_order SET status='已发货', ship_time=? WHERE id=?",
            new Timestamp(System.currentTimeMillis()), orderId);
        if (rows > 0) {
            log.info("发货成功: orderId={}", orderId);
        }
        return rows > 0;
    }
}
