package com.campus.repair.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket STOMP 配置：/ws 支持原生 WebSocket（推荐，避免 sockjs 的 unload 弃用）并保留 SockJS 以兼容旧客户端
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 原生 WebSocket，供 @stomp/stompjs 使用 brokerURL=ws(s)://.../api/ws，避免 sockjs-client 的 unload 弃用
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }
}
