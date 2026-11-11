package com.setec.forum.websocket;

import com.setec.forum.config.CorsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket.
 *
 *   endpoint            /ws  (native WebSocket) - browsers connect to ws(s)://host/ws
 *   public broadcast    /topic/resources            (comments, reactions, edits, deletes, new posts)
 *   private per user    /user/queue/notifications   (notifications for the logged-in user)
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final CorsProperties corsProperties;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Same origins as the REST CORS config (cors.allowed-origins) instead of a hard-coded one.
        String[] origins = corsProperties.getAllowedOrigins().toArray(new String[0]);

        // Use the native WebSocket transport because the web client connects with
        // @stomp/stompjs brokerURL=ws(s)://.../ws. SockJS would require a SockJS
        // client factory and a different transport URL.
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origins)
                .addInterceptors(jwtHandshakeInterceptor);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue")
                // Heartbeats keep the socket alive through proxies and let both sides notice a dead
                // connection (the client then reconnects automatically).
                .setHeartbeatValue(new long[]{10_000, 10_000})
                .setTaskScheduler(heartbeatScheduler());
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }

    /**
     * Deliberately NOT a Spring bean: the messaging infrastructure already owns a TaskScheduler bean
     * and a second one could make by-type injection ambiguous. A daemon thread never blocks shutdown.
     */
    private ThreadPoolTaskScheduler heartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setDaemon(true);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }
}
