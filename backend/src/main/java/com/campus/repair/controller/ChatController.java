package com.campus.repair.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.repair.common.BusinessException;
import com.campus.repair.common.Result;
import com.campus.repair.context.UserContext;
import com.campus.repair.entity.ChatMessage;
import com.campus.repair.service.ChatMessageService;
import lombok.Data;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/chat")
public class ChatController {
    private final ChatMessageService chatMessageService;

    public ChatController(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    @PostMapping("/send")
    public Result<ChatMessage> sendMessage(@RequestBody SendMessageRequest request) {
        Long oid = parseId(request.getOrderId(), "orderId");
        Long rid = request.getReceiverId() != null ? parseId(request.getReceiverId(), "receiverId") : null;
        ChatMessage message = chatMessageService.sendMessage(
                oid,
                rid,
                request.getContent(),
                request.getImages()
        );
        return Result.success(message);
    }

    private static Long parseId(String raw, String field) {
        if (raw == null) {
            throw new BusinessException("参数 " + field + " 无效");
        }
        String t = String.valueOf(raw).trim();
        if (!StringUtils.hasText(t)) {
            throw new BusinessException("参数 " + field + " 无效");
        }
        try {
            return Long.parseLong(t);
        } catch (NumberFormatException e) {
            throw new BusinessException("参数 " + field + " 不是有效数字ID");
        }
    }

    @GetMapping("/list/{orderId}")
    public Result<IPage<ChatMessage>> getMessageList(
            @PathVariable Long orderId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "50") long size) {
        Page<ChatMessage> pageParam = new Page<>(page, size);
        IPage<ChatMessage> result = chatMessageService.getMessageList(pageParam, orderId);
        return Result.success(result);
    }

    @GetMapping("/unread")
    public Result<List<ChatMessage>> getUnreadMessages() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.success(Collections.emptyList());
        }
        List<ChatMessage> messages = chatMessageService.getUnreadMessages(userId);
        return Result.success(messages);
    }

    @GetMapping("/conversations")
    public Result<IPage<ChatMessage>> getRecentConversations(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            Page<ChatMessage> emptyPage = new Page<>(page, size);
            return Result.success(emptyPage);
        }
        Page<ChatMessage> pageParam = new Page<>(page, size);
        IPage<ChatMessage> result = chatMessageService.getRecentConversations(pageParam, userId);
        return Result.success(result);
    }

    @PostMapping("/mark-read/{orderId}")
    public Result<Void> markAsRead(@PathVariable Long orderId) {
        Long userId = UserContext.getUserId();
        if (userId != null) {
            chatMessageService.markAsRead(orderId, userId);
        }
        return Result.success();
    }

    @Data
    public static class SendMessageRequest {
        /** 用字符串接前端雪花 ID，避免 long 与 JSON 双精度问题 */
        private String orderId;
        private String receiverId;
        private String content;
        private String images;
    }
}
