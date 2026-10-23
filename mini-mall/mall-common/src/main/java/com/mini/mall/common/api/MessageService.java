package com.mini.mall.common.api;

import com.mini.mall.common.entity.Message;

import java.util.List;

public interface MessageService {

    Message send(Long fromUserId, Long toUserId, String content);

    List<Message> listConversation(Long userIdA, Long userIdB);
}
