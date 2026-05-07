package com.campus.repair.service.impl;

import com.campus.repair.service.NotifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket 实时通知：推送到 /topic/notify
 */
@Service
public class NotifyServiceImpl implements NotifyService {

    private static final Logger log = LoggerFactory.getLogger(NotifyServiceImpl.class);
    private static final String TOPIC_NOTIFY = "/topic/notify";

    private final SimpMessagingTemplate messagingTemplate;

    public NotifyServiceImpl(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void sendRepairNotify(String type, Map<String, Object> payload) {
        try {
            Map<String, Object> msg = new HashMap<>(payload != null ? payload : new HashMap<>());
            msg.put("type", type);
            msg.put("timestamp", System.currentTimeMillis());
            messagingTemplate.convertAndSend(TOPIC_NOTIFY, msg);
        } catch (Exception e) {
            log.warn("WebSocket 推送失败: {}", e.getMessage());
        }
    }
}
