package com.campus.repair.consumer;

import com.campus.repair.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 通知队列消费者 - 模拟短信/站内信
 */
@Component
public class NotifyConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotifyConsumer.class);

    @RabbitListener(queues = RabbitConfig.QUEUE_NOTIFY)
    public void handle(Map<String, Object> msg) {
        Object orderId = msg.get("orderId");
        Object status = msg.get("status");
        log.info("模拟发送短信通知用户... orderId={}, status={}", orderId, status);
    }
}
