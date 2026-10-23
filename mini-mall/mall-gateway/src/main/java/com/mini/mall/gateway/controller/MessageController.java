package com.mini.mall.gateway.controller;

import com.mini.rpc.spring.annotation.RpcReference;
import com.mini.mall.common.api.MessageService;
import com.mini.mall.common.entity.Message;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/message")
public class MessageController {

    @RpcReference
    private MessageService messageService;

    @PostMapping("/send")
    public Message send(@RequestParam Long fromUserId,
                        @RequestParam Long toUserId,
                        @RequestParam String content) {
        return messageService.send(fromUserId, toUserId, content);
    }

    @GetMapping("/conversation")
    public List<Message> conversation(@RequestParam Long userIdA,
                                      @RequestParam Long userIdB) {
        return messageService.listConversation(userIdA, userIdB);
    }
}
