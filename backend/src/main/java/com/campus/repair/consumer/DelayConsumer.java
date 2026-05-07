package com.campus.repair.consumer;

import com.campus.repair.config.RabbitConfig;
import com.campus.repair.entity.RepairOrder;
import com.campus.repair.mapper.RepairOrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 延迟队列消费者 - 超时未接单自动取消（需配合 TTL/死信使用，此处仅消费逻辑）
 */
@Component
public class DelayConsumer {

    private static final Logger log = LoggerFactory.getLogger(DelayConsumer.class);
    private static final int STATUS_CANCELLED = 3;
    private static final int STATUS_PENDING = 0;

    private final RepairOrderMapper repairOrderMapper;

    public DelayConsumer(RepairOrderMapper repairOrderMapper) {
        this.repairOrderMapper = repairOrderMapper;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_DELAY)
    public void handle(Map<String, Object> msg) {
        try {
            Object orderIdObj = msg.get("orderId");
            if (orderIdObj == null) return;
            Long orderId = orderIdObj instanceof Number ? ((Number) orderIdObj).longValue() : null;
            if (orderId == null) return;
            RepairOrder order = repairOrderMapper.selectById(orderId);
            if (order != null && STATUS_PENDING == order.getStatus()) {
                order.setStatus(STATUS_CANCELLED);
                repairOrderMapper.updateById(order);
                log.info("订单超时未接单，已自动取消 orderId={}", orderId);
            }
        } catch (Exception e) {
            log.warn("延迟处理失败: {}", e.getMessage());
        }
    }
}
