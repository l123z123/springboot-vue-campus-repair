package com.campus.repair.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ：Direct 交换机与队列 + JSON 消息转换器
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "repair.exchange";
    public static final String ROUTING_NOTIFY = "notify";
    public static final String ROUTING_LOG = "log";
    public static final String ROUTING_DELAY = "delay";

    public static final String QUEUE_NOTIFY = "notify.queue";
    public static final String QUEUE_LOG = "log.queue";
    public static final String QUEUE_DELAY = "delay.queue";

    @Bean
    public DirectExchange repairExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue notifyQueue() {
        return new Queue(QUEUE_NOTIFY, true);
    }

    @Bean
    public Queue logQueue() {
        return new Queue(QUEUE_LOG, true);
    }

    @Bean
    public Queue delayQueue() {
        return new Queue(QUEUE_DELAY, true);
    }

    @Bean
    public Binding bindingNotify(Queue notifyQueue, DirectExchange repairExchange) {
        return BindingBuilder.bind(notifyQueue).to(repairExchange).with(ROUTING_NOTIFY);
    }

    @Bean
    public Binding bindingLog(Queue logQueue, DirectExchange repairExchange) {
        return BindingBuilder.bind(logQueue).to(repairExchange).with(ROUTING_LOG);
    }

    @Bean
    public Binding bindingDelay(Queue delayQueue, DirectExchange repairExchange) {
        return BindingBuilder.bind(delayQueue).to(repairExchange).with(ROUTING_DELAY);
    }

    /**
     * 使用 Jackson JSON 作为 RabbitTemplate 和 @RabbitListener 的消息转换器，
     * 避免 Java 反序列化带来的安全告警。
     */
    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
