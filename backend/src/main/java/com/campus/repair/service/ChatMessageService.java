package com.campus.repair.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.repair.entity.ChatMessage;

import java.util.List;

public interface ChatMessageService {
    ChatMessage sendMessage(Long orderId, Long receiverId, String content, String images);

    IPage<ChatMessage> getMessageList(Page<ChatMessage> page, Long orderId);

    List<ChatMessage> getUnreadMessages(Long userId);

    void markAsRead(Long orderId, Long userId);

    IPage<ChatMessage> getRecentConversations(Page<ChatMessage> page, Long userId);
}
