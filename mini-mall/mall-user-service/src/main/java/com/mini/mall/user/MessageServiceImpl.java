package com.mini.mall.user;

import com.mini.rpc.spring.annotation.RpcService;
import com.mini.mall.common.api.MessageService;
import com.mini.mall.common.entity.Message;
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
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

@RpcService
public class MessageServiceImpl implements MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageServiceImpl.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final RowMapper<Message> MESSAGE_ROW_MAPPER = (rs, rowNum) -> {
        Message m = new Message();
        m.setId(rs.getLong("id"));
        m.setFromUserId(rs.getLong("from_user_id"));
        m.setToUserId(rs.getLong("to_user_id"));
        m.setContent(rs.getString("content"));
        m.setCreateTime(rs.getTimestamp("create_time"));
        return m;
    };

    @PostConstruct
    public void init() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_message (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "from_user_id BIGINT," +
            "to_user_id BIGINT," +
            "content VARCHAR(1024)," +
            "create_time DATETIME" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    @Override
    public Message send(Long fromUserId, Long toUserId, String content) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO t_message (from_user_id, to_user_id, content, create_time) VALUES (?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, fromUserId);
            ps.setLong(2, toUserId);
            ps.setString(3, content);
            ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            return ps;
        }, keyHolder);

        Message message = new Message();
        message.setId(keyHolder.getKey().longValue());
        message.setFromUserId(fromUserId);
        message.setToUserId(toUserId);
        message.setContent(content);
        message.setCreateTime(new Date());
        log.info("发送消息: from={}, to={}, content={}", fromUserId, toUserId, content);
        return message;
    }

    @Override
    public List<Message> listConversation(Long userIdA, Long userIdB) {
        String sql = "SELECT * FROM t_message " +
            "WHERE (from_user_id=? AND to_user_id=?) OR (from_user_id=? AND to_user_id=?) " +
            "ORDER BY create_time ASC, id ASC";
        return jdbcTemplate.query(sql, MESSAGE_ROW_MAPPER, userIdA, userIdB, userIdB, userIdA);
    }
}
