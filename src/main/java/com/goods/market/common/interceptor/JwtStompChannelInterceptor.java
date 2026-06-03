package com.goods.market.common.interceptor;

import com.goods.market.common.auth.AuthPrincipal;
import com.goods.market.common.auth.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtStompChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        log.info("Command: {}", accessor.getCommand());
        log.info("Command: {}", accessor.toString());

        // STOMP 연결 시점에만 토큰 확인 후 인증 객체 주입
        if (StompCommand.CONNECT == accessor.getCommand()) {
            String token = accessor.getFirstNativeHeader("Authorization");

            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            if (token != null && jwtTokenProvider.validate(token)) {
                Long memberId = jwtTokenProvider.getMemberId(token);

                // AuthPrincipal 객체 생성 (사용하시는 DTO/Value Object)
                AuthPrincipal authPrincipal = new AuthPrincipal(memberId);

                // 스프링 시큐리티 인증 토큰 생성
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(authPrincipal, null, Collections.emptyList());

                accessor.setUser(authentication);
            }
        }
        return message;
    }
}
