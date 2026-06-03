package com.goods.market.common.config;

import com.goods.market.common.interceptor.JwtStompChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtStompChannelInterceptor jwtStompChannelInterceptor;


    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 1. 클라이언트가 웹소켓 연결을 시작할 '입구(Endpoint)' 설정
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*")// CORS 허용 (실무 배포 시에는 프론트엔드 도메인으로 제한 필요)
                .withSockJS(); // 웹소켓을 지원하지 않는 환경을 위한 폴백(Fallback) 옵션
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        //2. 메시지를 수신(subcribe)하는 요청의 접두사 (우체통 역할)
        // 메모리 기반 Simple Broker를 활성화하여 해당 경로를 구독하는 클라이언트에게 메시지를 전달
        registry.enableSimpleBroker("/sub");

        // 3. 메시지를 발송(Publish)하는 요청의 접두사 (우체부 역할)
        // 클라이언트가 메시지를 보낼 때 라우팅될 Prefix를 지정
        registry.setApplicationDestinationPrefixes("/pub");
    }


    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtStompChannelInterceptor);
    }
}
