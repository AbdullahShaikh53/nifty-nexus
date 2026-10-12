package com.niftynexus.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket. Topics:
 *  /topic/quotes            - every tick for all subscribed symbols
 *  /topic/quotes.{SYMBOL}   - per-symbol ticks
 *  /topic/feed-status       - live/stale/disconnected
 *  /topic/user.{userId}     - order fills, alerts, notifications
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final AppProperties props;
    public WebSocketConfig(AppProperties props) { this.props = props; }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns(SecurityConfig.LAN_ORIGIN_PATTERNS);
    }
}
