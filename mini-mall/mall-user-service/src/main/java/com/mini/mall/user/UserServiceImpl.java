package com.mini.mall.user;

import com.mini.rpc.spring.annotation.RpcService;
import com.mini.mall.common.api.UserService;
import com.mini.mall.common.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import javax.annotation.PostConstruct;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@RpcService
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final RowMapper<User> USER_ROW_MAPPER = (rs, rowNum) -> {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        u.setSecurityQuestion(rs.getString("security_question"));
        u.setSecurityAnswer(rs.getString("security_answer"));
        u.setRole(rs.getString("role"));
        return u;
    };

    @PostConstruct
    public void init() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_user (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "username VARCHAR(64)," +
            "password VARCHAR(64)," +
            "email VARCHAR(128)," +
            "phone VARCHAR(20)," +
            "security_question VARCHAR(255)," +
            "security_answer VARCHAR(255)," +
            "role VARCHAR(20)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_user", Integer.class);
        if (count != null && count == 0) {
            jdbcTemplate.update("INSERT INTO t_user (id, username, password, email, role) VALUES (?,?,?,?,?)",
                1L, "admin", "123456", "admin@test.com", "USER");
            log.info("初始化测试用户: id=1, username=admin");
        }
    }

    @Override
    public User getById(Long id) {
        log.info("查询用户: id={}", id);
        List<User> list = jdbcTemplate.query("SELECT * FROM t_user WHERE id=?", USER_ROW_MAPPER, id);
        return list.isEmpty() ? null : sanitize(list.get(0));
    }

    @Override
    public User register(String username, String password, String email) {
        log.info("注册用户: username={}", username);
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setEmail(email);
        user.setRole("USER");
        long id = insertUser(user);
        user.setId(id);
        return user;
    }

    @Override
    public User registerByPhone(String phone, String password, String securityQuestion, String securityAnswer) {
        log.info("手机号注册: phone={}", phone);
        if (findByPhoneInternal(phone) != null) {
            log.info("手机号已注册: phone={}", phone);
            return null;
        }
        User user = new User();
        user.setPhone(phone);
        user.setPassword(password);
        user.setSecurityQuestion(securityQuestion);
        user.setSecurityAnswer(securityAnswer);
        user.setRole("USER");
        long id = insertUser(user);
        user.setId(id);
        return sanitize(user);
    }

    @Override
    public User login(String phone, String password) {
        log.info("登录: phone={}", phone);
        User user = findByPhoneInternal(phone);
        if (user == null || user.getPassword() == null || !user.getPassword().equals(password)) {
            log.info("登录失败: phone={}", phone);
            return null;
        }
        return sanitize(user);
    }

    @Override
    public User findByPhone(String phone) {
        User user = findByPhoneInternal(phone);
        return user == null ? null : sanitize(user);
    }

    @Override
    public boolean verifySecurityAnswer(String phone, String securityAnswer) {
        User user = findByPhoneInternal(phone);
        if (user == null || user.getSecurityAnswer() == null) {
            return false;
        }
        return user.getSecurityAnswer().equals(securityAnswer);
    }

    @Override
    public boolean resetPassword(String phone, String newPassword) {
        int rows = jdbcTemplate.update("UPDATE t_user SET password=? WHERE phone=?", newPassword, phone);
        if (rows > 0) {
            log.info("密码重置成功: phone={}", phone);
        }
        return rows > 0;
    }

    @Override
    public User adminLogin(String username, String password) {
        log.info("管理员登录: username={}", username);
        if ("root".equals(username) && "123456".equals(password)) {
            User admin = new User(0L, "root", null);
            admin.setRole("ADMIN");
            return admin;
        }
        return null;
    }

    @Override
    public List<User> listAll() {
        log.info("查询所有用户");
        List<User> users = jdbcTemplate.query("SELECT * FROM t_user", USER_ROW_MAPPER);
        List<User> result = new ArrayList<>();
        for (User u : users) {
            result.add(sanitize(u));
        }
        return result;
    }

    private User findByPhoneInternal(String phone) {
        if (phone == null) {
            return null;
        }
        List<User> list = jdbcTemplate.query("SELECT * FROM t_user WHERE phone=?", USER_ROW_MAPPER, phone);
        return list.isEmpty() ? null : list.get(0);
    }

    private long insertUser(User u) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO t_user (username, password, email, phone, security_question, security_answer, role) VALUES (?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPassword());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getPhone());
            ps.setString(5, u.getSecurityQuestion());
            ps.setString(6, u.getSecurityAnswer());
            ps.setString(7, u.getRole());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? 0L : key.longValue();
    }

    // 脱敏：不返回 password 和密保答案，避免泄露
    private User sanitize(User u) {
        User result = new User(u.getId(), u.getUsername(), u.getEmail());
        result.setPhone(u.getPhone());
        result.setSecurityQuestion(u.getSecurityQuestion());
        result.setRole(u.getRole() != null ? u.getRole() : "USER");
        return result;
    }
}
