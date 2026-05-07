package com.campus.repair.consumer;

import com.campus.repair.config.RabbitConfig;
import com.campus.repair.context.UserContext;
import com.campus.repair.entity.OrderLog;
import com.campus.repair.mapper.OrderLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 日志队列消费者 - 异步写入 order_log
 */
@Component
public class LogConsumer {

    private static final Logger log = LoggerFactory.getLogger(LogConsumer.class);

    private final OrderLogMapper orderLogMapper;

    public LogConsumer(OrderLogMapper orderLogMapper) {
        this.orderLogMapper = orderLogMapper;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_LOG)
    public void handle(Map<String, Object> msg) {
        try {
            Object orderIdObj = msg.get("orderId");
            Object statusObj = msg.get("status");
            Object operatorIdObj = msg.get("operatorId");
            Long orderId = orderIdObj instanceof Number ? ((Number) orderIdObj).longValue() : null;
            Long operatorId = operatorIdObj instanceof Number ? ((Number) operatorIdObj).longValue() : 0L;
            if (orderId == null) return;
            OrderLog orderLog = new OrderLog();
            orderLog.setOrderId(orderId);
            orderLog.setOperatorId(operatorId != null ? operatorId : 0L);
            orderLog.setAction("STATUS_CHANGE");
            orderLog.setContent("status -> " + statusObj);
            orderLogMapper.insert(orderLog);
        } catch (Exception e) {
            log.warn("写入 order_log 失败: {}", e.getMessage());
        }
    }
}
